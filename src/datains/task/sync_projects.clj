(ns datains.task.sync-projects
  "Tasks related to sync projects' metadata to database."
  (:require [clojure.tools.logging :as log]
            [datains.config :refer [env]]
            [clojurewerkz.quartzite
             [jobs :as jobs]
             [triggers :as triggers]]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [datains.task :as task]
            [datains.db.core :refer [*db*] :as db]
            [clojure.java.jdbc :as jdbc]
            ; [datains.adapters.dingtalk :as dingtalk]
            [datains.util :as util]))

;;; ------------------------------------------------- Sync Project ---------------------------------------------------
(defn- sync-projects! []
  (jdbc/with-db-transaction [t-conn *db*]
    (doseq [project (db/get-finished-project)]
      (log/debug "Sync Project: " project)
      (db/update-project! t-conn {:updates {:finished_time (util/time->int (util/now))}
                                  :id      (:id project)}))))

;;; ------------------------------------------------------ Task ------------------------------------------------------
;; triggers the syncing of all jobs which are scheduled to run in the current minutes
(jobs/defjob SyncProjects [_]
  (try
    (log/info "Sync projects' metadata...")
    (sync-projects!)
    (catch Throwable e
      (log/error e "SyncJobs task failed"))))

(def ^:private sync-projects-job-key     "datains.task.sync-projects.job")
(def ^:private sync-projects-trigger-key "datains.task.sync-projects.trigger")

(defn get-cron-conf
  []
  (let [cron (get-in env [:tasks :sync-projects :cron])]
    (if cron
      cron
      ;; run at the top of every minute
      "0 */1 * * * ?")))

(defmethod task/init! ::SyncJobs [_]
  (let [job     (jobs/build
                 (jobs/of-type SyncProjects)
                 (jobs/with-identity (jobs/key sync-projects-job-key)))
        trigger (triggers/build
                 (triggers/with-identity (triggers/key sync-projects-trigger-key))
                 (triggers/start-now)
                 (triggers/with-schedule
                   (cron/schedule
                    (cron/cron-schedule (get-cron-conf))
                    ;; If sync-projects! misfires, don't try to re-sync all the misfired jobs. Retry only the most
                    ;; recent misfire, discarding all others. This should hopefully cover cases where a misfire
                    ;; happens while the system is still running; if the system goes down for an extended period of
                    ;; time we don't want to re-send tons of (possibly duplicate) jobs.
                    ;;
                    ;; See https://www.nurkiewicz.com/2012/04/quartz-scheduler-misfire-instructions.html
                    (cron/with-misfire-handling-instruction-fire-and-proceed))))]
    (task/schedule-task! job trigger)))

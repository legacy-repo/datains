(ns datains.task.sync-jobs
  "Tasks related to sync jobs' metadata to database from cromwell instance."
  (:require [clojure.tools.logging :as log]
            [datains.config :refer [env]]
            [clojurewerkz.quartzite
             [jobs :as jobs]
             [triggers :as triggers]]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [datains.task :as task]
            [datains.db.handler :as db-handler]
            [datains.adapters.cromwell.core :as cromwell]
            [datains.db.core :refer [*db*] :as db]
            [clojure.java.jdbc :as jdbc]
            [datains.adapters.dingtalk :as dingtalk]
            [honeysql.core :as sql]
            [datains.util :as util]))

;;; ------------------------------------------------- Sync Jobs ---------------------------------------------------
(def incomplete-jobs-condition (sql/format {:where [:and
                                                    [:in :datains-workflow.status ["Submitted" "Aborting" "On Hold" "Running"]]
                                                    [:<> :datains-workflow.workflow-id nil]]}))

(defn- count-incomplete-jobs []
  (:count (db/get-workflow-count {:where-clause incomplete-jobs-condition})))

(defn- get-incomplete-jobs [page per-page]
  (:data (db-handler/search-workflows-with-projects
          {:where-clause incomplete-jobs-condition}
          page per-page)))

(defn- total-page [total per-page]
  (if (= (rem total per-page) 0)
    (quot total per-page)
    (+ (quot total per-page) 1)))

(defn- format-record [record]
  (-> (assoc record 
             :started_time (util/time->int (:started_time record))
             :finished_time (util/time->int (:finished_time record)))
      (dissoc :submitted_time)))

(defn- sync-jobs! []
  (let [per-page 10]  ; Get ten jobs each time
    (log/debug "Pages: " (+ (total-page (count-incomplete-jobs) per-page) 1))
    (for [which-page (range 1 (+ (total-page (count-incomplete-jobs) per-page) 1))]
      (let [jobs     (get-incomplete-jobs which-page per-page)]
        (log/debug "Jobs: " jobs)
        (jdbc/with-db-transaction [t-conn *db*]
          (doseq [job jobs]
            (let [workflow-id (:workflow_id job)
                  metadata    (cromwell/workflow-metadata workflow-id)]
              (log/debug "Sync Job: " workflow-id " " metadata)
              (db/update-workflow! t-conn {:updates (format-record metadata)
                                           :id      (:id job)}))))))))

;;; ------------------------------------------------------ Task ------------------------------------------------------
;; triggers the syncing of all jobs which are scheduled to run in the current minutes
(jobs/defjob SyncJobs [_]
  (try
    (log/info "Sync jobs' metadata from cromwell instance...")
    (sync-jobs!)
    (catch Throwable e
      (log/error e "SyncJobs task failed"))))

(def ^:private sync-jobs-job-key     "datains.task.sync-jobs.job")
(def ^:private sync-jobs-trigger-key "datains.task.sync-jobs.trigger")

(defn get-cron-conf
  []
  (let [cron (get-in env [:tasks :sync-jobs :cron])]
    (if cron
      cron
      ;; run at the top of every minute
      "0 */1 * * * ?")))

(defmethod task/init! ::SyncJobs [_]
  (let [job     (jobs/build
                 (jobs/of-type SyncJobs)
                 (jobs/with-identity (jobs/key sync-jobs-job-key)))
        trigger (triggers/build
                 (triggers/with-identity (triggers/key sync-jobs-trigger-key))
                 (triggers/start-now)
                 (triggers/with-schedule
                   (cron/schedule
                    (cron/cron-schedule (get-cron-conf))
                    ;; If sync-jobs! misfires, don't try to re-sync all the misfired jobs. Retry only the most
                    ;; recent misfire, discarding all others. This should hopefully cover cases where a misfire
                    ;; happens while the system is still running; if the system goes down for an extended period of
                    ;; time we don't want to re-send tons of (possibly duplicate) jobs.
                    ;;
                    ;; See https://www.nurkiewicz.com/2012/04/quartz-scheduler-misfire-instructions.html
                    (cron/with-misfire-handling-instruction-fire-and-proceed))))]
    (task/schedule-task! job trigger)))

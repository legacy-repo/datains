(ns datains.task.sync-apps
  "Tasks related to sync apps to datains database from choppy appstore."
  (:require [clojure.tools.logging :as log]
            [clojurewerkz.quartzite
             [jobs :as jobs]
             [triggers :as triggers]]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [datains.task :as task]
            [clj-http.client :as client]
            [datains.adapters.app-store.core :as app-store]
            [datains.db.core :refer [*db*] :as db]
            [clojure.java.jdbc :as jdbc]))

;;; ------------------------------------------------- Syncing Apps ---------------------------------------------------
(defn- sync-apps! []
  (let [apps (app-store/get-all-apps "choppy-app")]
    (jdbc/with-db-transaction [t-conn *db*]
      (db/delete-all-apps! t-conn)
      (doseq [app apps]
        (log/info app)
        (db/create-app! t-conn app)))))

;;; ------------------------------------------------------ Task ------------------------------------------------------
;; triggers the syncing of all apps which are scheduled to run in the current hour
(jobs/defjob SyncApps [_]
  (try
    (sync-apps!)
    (log/info "Sync apps from app store...")
    (catch Throwable e
      (log/error e "SyncApps task failed"))))

(def ^:private sync-apps-job-key     "datains.task.sync-apps.job")
(def ^:private sync-apps-trigger-key "datains.task.sync-apps.trigger")

(defmethod task/init! ::SyncApps [_]
  (let [job     (jobs/build
                 (jobs/of-type SyncApps)
                 (jobs/with-identity (jobs/key sync-apps-job-key)))
        trigger (triggers/build
                 (triggers/with-identity (triggers/key sync-apps-trigger-key))
                 (triggers/start-now)
                 (triggers/with-schedule
                   (cron/schedule
                    ;; run at the top of every hour
                    (cron/cron-schedule "0 */1 * * * ?")
                    ;; If sync-apps! misfires, don't try to re-sync all the misfired apps. Retry only the most
                    ;; recent misfire, discarding all others. This should hopefully cover cases where a misfire
                    ;; happens while the system is still running; if the system goes down for an extended period of
                    ;; time we don't want to re-send tons of (possibly duplicate) apps.
                    ;;
                    ;; See https://www.nurkiewicz.com/2012/04/quartz-scheduler-misfire-instructions.html
                    (cron/with-misfire-handling-instruction-fire-and-proceed))))]
    (task/schedule-task! job trigger)))

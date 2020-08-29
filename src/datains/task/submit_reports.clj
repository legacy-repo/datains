(ns datains.task.submit-reports
  "Tasks related to submit report to tservice."
  (:require [clojure.tools.logging :as log]
            [datains.config :refer [env]]
            [clojurewerkz.quartzite
             [jobs :as jobs]
             [triggers :as triggers]]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [datains.task :as task]
            [clojure.data.json :as json]
            [datains.db.handler :as db-handler]
            [datains.adapters.tservice.core :as tservice]
            [datains.db.core :refer [*db*] :as db]
            [clojure.java.jdbc :as jdbc]))

;;; ------------------------------------------------- Submit Jobs ---------------------------------------------------
(defn- count-submitted-reports []
  (:count (db/get-report-count {:query-map {:status "Submitted"}})))

(defn- get-submitted-reports [page per-page]
  (:data (db-handler/search-reports
          {:query-map {:status "Submitted"}}
          page per-page)))

(defn- total-page [total per-page]
  (if (= (rem total per-page) 0)
    (quot total per-page)
    (+ (quot total per-page) 1)))

(defn- submit-reports! []
  (let [per-page   10
        total-page (+ (total-page (count-submitted-reports) per-page) 1)]
    (log/debug "Num of Reports: " per-page " and " total-page)
    (doseq [which-page (range 1 total-page)]
      (let [reports (get-submitted-reports which-page per-page)]
        (log/debug "Reports: " reports)
        (jdbc/with-db-transaction [t-conn *db*]
          (doseq [report reports]
            (log/debug "Sumitting Report: " report)
            (let [report-metadata (json/read-str (:script report) :key-fn keyword)
                  report-name (:plugin-name report-metadata)
                  body (:metadata report-metadata)]
              (log/info (format "Running Report: %s %s" report-name body))
              (try
                (when-let [result (tservice/submit-report report-name body)]
                  (if (:log_url result)
                    (db/update-report! t-conn {:updates {:log (:log_url result)
                                                         :report_path (:download_url result)
                                                         :report_id (:id result)
                                                         :status "Started"}
                                               :id      (:id report)})
                    (comment "Add exception handler")))
                (catch Exception e
                  (db/update-report! t-conn {:updates {:log (.getMessage e)
                                                       :status "Failed"}})
                  (log/error (format "Submit Report Failed: %s" (.printStackTrace e))))))))))))

;;; ------------------------------------------------------ Task ------------------------------------------------------
;; triggers the submitting of all reports which are scheduled to run in the current minutes
(jobs/defjob SubmitReports [_]
  (try
    (log/info "Submit reports in submitted status...")
    (submit-reports!)
    (catch Throwable e
      (log/error e "SubmitReports task failed"))))

(def ^:private submit-reports-job-key     "datains.task.submit-reports.job")
(def ^:private submit-reports-trigger-key "datains.task.submit-reports.trigger")

(defn get-cron-conf
  []
  (let [cron (get-in env [:tasks :submit-reports :cron])]
    (if cron
      cron
      ;; run at the top of every minute
      "0 */1 * * * ?")))

(defmethod task/init! ::SubmitReports [_]
  (let [job     (jobs/build
                 (jobs/of-type SubmitReports)
                 (jobs/with-identity (jobs/key submit-reports-job-key)))
        trigger (triggers/build
                 (triggers/with-identity (triggers/key submit-reports-trigger-key))
                 (triggers/start-now)
                 (triggers/with-schedule
                   (cron/schedule
                    (cron/cron-schedule (get-cron-conf))
                    ;; If submit-reports! misfires, don't try to re-submit all the misfired jobs. Retry only the most
                    ;; recent misfire, discarding all others. This should hopefully cover cases where a misfire
                    ;; happens while the system is still running; if the system goes down for an extended period of
                    ;; time we don't want to re-send tons of (possibly duplicate) jobs.
                    ;;
                    ;; See https://www.nurkiewicz.com/2012/04/quartz-scheduler-misfire-instructions.html
                    (cron/with-misfire-handling-instruction-fire-and-proceed))))]
    (task/schedule-task! job trigger)))

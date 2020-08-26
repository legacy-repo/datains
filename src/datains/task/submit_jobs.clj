(ns datains.task.submit-jobs
  "Tasks related to submit jobs to cromwell instance from workflow table."
  (:require [clojure.tools.logging :as log]
            [datains.config :refer [env]]
            [clojurewerkz.quartzite
             [jobs :as jobs]
             [triggers :as triggers]]
            [clojurewerkz.quartzite.schedule.cron :as cron]
            [datains.task :as task]
            [me.raynes.fs :as fs]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [datains.db.handler :as db-handler]
            [datains.adapters.app-store.core :as app-store]
            [datains.adapters.cromwell.core :as cromwell]
            [datains.db.core :refer [*db*] :as db]
            [clojure.java.jdbc :as jdbc]
            [datains.adapters.dingtalk :as dingtalk]
            [clj-filesystem.core :as fs-service]
            [honeysql.core :as sql]))

;;; ------------------------------------------------- Submit Jobs ---------------------------------------------------
(def submitted-jobs-condition (sql/format {:where [:and
                                                   [:= :datains-workflow.status "Submitted"]
                                                   [:is :datains-workflow.workflow-id nil]]}))

(defn- count-submitted-jobs []
  (:count (db/get-workflow-count {:where-clause submitted-jobs-condition})))

(defn- get-submitted-jobs [page per-page]
  (:data (db-handler/search-workflows-with-projects
          {:where-clause submitted-jobs-condition}
          page per-page)))

(defn- total-page [total per-page]
  (if (= (rem total per-page) 0)
    (quot total per-page)
    (+ (quot total per-page) 1)))

(defn- submit-jobs! []
  (let [per-page   10
        total-page (+ (total-page (count-submitted-jobs) per-page) 1)]
    (log/debug "Num of Jobs: " per-page " and " total-page)
    (doseq [which-page (range 1 total-page)]
      (let [jobs (get-submitted-jobs which-page per-page)]  ; Get five jobs each time
        (log/debug "Jobs: " jobs)
        (jdbc/with-db-transaction [t-conn *db*]
          (doseq [job jobs]
            (log/debug "Sumitting Job: " job (fs-service/correct-file-path (:job_params job) (:fs-rootdir env)))
            ; when you use minio service, all file paths need to reset as the local path.
            ; e.g. minio://bucket-name/object-key --> /datains/minio/bucket-name/object-key
            (let [sample-file (app-store/make-sample-file! (:project_name job)
                                                           (:sample_id job)
                                                           (fs-service/correct-file-path (:job_params job) (:fs-rootdir env)))
                  results     (app-store/render-app! (:project_name job) (:app_name job) sample-file)
                  root-dir    (fs/parent sample-file)
                  wdl-file    (str root-dir "/workflow.wdl")
                  inputs      (str root-dir "/inputs")
                  imports-zip (str root-dir "/tasks.zip")
                  options     nil
                  labels      (merge (:labels job) {:app_name  (:app_name job)
                                                    :author    (:author job)
                                                    :group     (:group_name job)
                                                    :sample_id (:sample_id job)})
                  workflow-id (if (= 0 (:exit results))
                                (cromwell/submit-workflow (io/file wdl-file)
                                                          (io/file imports-zip)
                                                          (json/read-str (slurp inputs) :key-fn keyword)
                                                          options
                                                          labels)
                                (log/error results))]
              (log/info "Render App: " results)
              (log/info "Submit Job:" (merge job {:sample-file sample-file
                                                  :root-dir    root-dir
                                                  :wdl-file    wdl-file
                                                  :inputs      inputs
                                                  :imports-zip imports-zip
                                                  :options     options
                                                  :labels      labels
                                                  :workflow_id workflow-id}))
              (if workflow-id
                (db/update-workflow! t-conn {:updates {:workflow_id workflow-id}
                                             :id      (:id job)})
                (comment (dingtalk/send-markdown-msg! (:project_name job) "Wrong"))))))))))

;;; ------------------------------------------------------ Task ------------------------------------------------------
;; triggers the submitting of all jobs which are scheduled to run in the current minutes
(jobs/defjob SubmitJobs [_]
  (try
    (log/info "Submit jobs in submitted status...")
    (submit-jobs!)
    (catch Throwable e
      (log/error e "SubmitJobs task failed"))))

(def ^:private submit-jobs-job-key     "datains.task.submit-jobs.job")
(def ^:private submit-jobs-trigger-key "datains.task.submit-jobs.trigger")

(defn get-cron-conf
  []
  (let [cron (get-in env [:tasks :submit-jobs :cron])]
    (if cron
      cron
      ;; run at the top of every minute
      "0 */1 * * * ?")))

(defmethod task/init! ::SubmitJobs [_]
  (let [job     (jobs/build
                 (jobs/of-type SubmitJobs)
                 (jobs/with-identity (jobs/key submit-jobs-job-key)))
        trigger (triggers/build
                 (triggers/with-identity (triggers/key submit-jobs-trigger-key))
                 (triggers/start-now)
                 (triggers/with-schedule
                   (cron/schedule
                    (cron/cron-schedule (get-cron-conf))
                    ;; If submit-jobs! misfires, don't try to re-submit all the misfired jobs. Retry only the most
                    ;; recent misfire, discarding all others. This should hopefully cover cases where a misfire
                    ;; happens while the system is still running; if the system goes down for an extended period of
                    ;; time we don't want to re-send tons of (possibly duplicate) jobs.
                    ;;
                    ;; See https://www.nurkiewicz.com/2012/04/quartz-scheduler-misfire-instructions.html
                    (cron/with-misfire-handling-instruction-fire-and-proceed))))]
    (task/schedule-task! job trigger)))

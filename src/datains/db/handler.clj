(ns datains.db.handler
  (:require
   [clojure.java.jdbc :as jdbc]
   [datains.db.core :refer [*db*] :as db]
   [clojure.tools.logging :as log]
   [datains.util :as util]))

(defn count-workflow-with-status
  [project-id]
  (let [results     (db/count-workflow-with-status {:query-map {:project_id project-id}})
        trans-table {:Failed    :error
                     :Succeeded :success
                     :Running   :running
                     :Submitted :submitted}]
    (apply merge-with +
           (map (fn [result]
                  (assoc {:error     0
                          :success   0
                          :running   0
                          :submitted 0}
                         ((keyword (:status result)) trans-table) (:count result)))
                results))))

(defn- filter-query-map
  "Filter unqualified attribute or value.

   Change Log:
   1. Fix bug: PSQLException
      `filter-query-map` need to return nil when query-map is nil
  "
  [query-map]
  (let [query-map (into {} (filter (comp some? val) query-map))]
    (if (empty? query-map)
      nil
      query-map)))

(defn- page->offset 
  "Tranform page to offset."
  [page per-page]
  (* (- page 1) per-page))

(defn- search-entities
  ([func-map] (search-entities func-map nil 1 10))
  ([func-map page] (search-entities func-map nil page 10))
  ([func-map page per-page] (search-entities func-map nil page per-page))
  ([func-map where-map page per-page]
   (let [page     (if (nil? page) 1 page)
         per-page (if (nil? per-page) 10 per-page)
         params   {:limit  per-page
                   :offset (page->offset page per-page)}
         params   (merge params (-> where-map
                                    (assoc :query-map (filter-query-map (:query-map where-map)))))
         metadata {:total    (:count ((:count-func func-map) params))
                   :page     page
                   :per_page per-page}]
     (log/info "Query db by: " params)
     (merge metadata {:data ((:query-func func-map) params)}))))

(defn- search-entity
  [func-map id]
  (let [data   (:data (search-entities func-map {:query-map {:id id}} 1 10))
        record (first data)]
    (if record
      record
      {})))

(defn- update-entity!
  [func id record]
  (when record
    (func {:updates record
           :id      id})))

;; --------------------- App Record ---------------------
(def search-apps
  (partial
   search-entities
   {:query-func db/search-apps
    :count-func db/get-app-count}))

(def search-app
  (partial
   search-entity
   {:query-func db/search-apps
    :count-func db/get-app-count}))

(defn update-app! [id record]
  (update-entity! db/update-app! id record))

(defn delete-app! [id]
  (db/delete-app! {:id id}))

(defn create-app! [record]
  (db/create-app! record))

;; --------------------- Project Record ---------------------
(def search-projects
  (partial
   search-entities
   {:query-func db/search-projects
    :count-func db/get-project-count}))

(def search-project
  (partial
   search-entity
   {:query-func db/search-projects
    :count-func db/get-project-count}))

(defn update-project! [id record]
  (update-entity! db/update-project! id record))

(defn delete-project! [id]
  (db/delete-project! {:id id}))

(defn create-project! [record]
  (db/create-project! record))

(defn gen-workflow-record [project-id workflow]
  {:id             (util/uuid)
   :project_id     project-id
   :sample_id      (:sample_id workflow)
   :submitted_time (util/time->int (util/now))
   :started_time   0
   :finished_time  nil
   :workflow_id    nil
   :percentage     0
   :job_params     workflow
   :labels         {:project_id project-id}
   :status         "Submitted"})

(defn create-project-workflow! [record]
  (jdbc/with-db-transaction [t-conn *db*]
    (let [workflow-records (map #(gen-workflow-record (:id record) %) (:samples record))]
      (log/debug "Create project & workflow: " record workflow-records)
      (doseq [workflow workflow-records]
        (db/create-workflow! t-conn workflow))
      (db/create-project! t-conn record))))

;; --------------------- Workflow Record ---------------------
(def search-workflows
  (partial
   search-entities
   {:query-func db/search-workflows
    :count-func db/get-workflow-count}))

(def search-workflow
  (partial
   search-entity
   {:query-func db/search-workflows
    :count-func db/get-workflow-count}))

(def search-workflows-with-projects
  (partial
   search-entities
   {:query-func db/search-workflows-with-projects
    :count-func db/get-workflow-count}))

(defn update-workflow! [id record]
  (update-entity! db/update-workflow! id record))

(defn delete-workflow! [id]
  (db/delete-workflow! {:id id}))

(defn create-workflow! [record]
  (db/create-workflow! record))

;; --------------------- Notification Record ---------------------
(def search-notifications
  (partial
   search-entities
   {:query-func db/search-notifications
    :count-func db/get-notification-count}))

(def search-notification
  (partial
   search-entity
   {:query-func db/search-notifications
    :count-func db/get-notification-count}))

(defn update-notification! [id record]
  (update-entity! db/update-notification! id record))

(defn delete-notification! [id]
  (db/delete-notification! {:id id}))

(defn create-notification! [record]
  (db/create-notification! record))

;; --------------------- Log Record ---------------------
(def search-logs
  (partial
   search-entities
   {:query-func db/search-logs
    :count-func db/get-log-count}))

(def search-log
  (partial
   search-entity
   {:query-func db/search-logs
    :count-func db/get-log-count}))

(defn update-log! [id record]
  (update-entity! db/update-log! id record))

(defn delete-log! [id]
  (db/delete-log! {:id id}))

(defn create-log! [record]
  (db/create-log! record))

;; --------------------- Message Record ---------------------
(def search-messages
  (partial
   search-entities
   {:query-func db/search-messages
    :count-func db/get-message-count}))

(def search-message
  (partial
   search-entity
   {:query-func db/search-messages
    :count-func db/get-message-count}))

(defn update-message! [id record]
  (update-entity! db/update-message! id record))

(defn delete-message! [id]
  (db/delete-message! {:id id}))

(defn create-message! [record]
  (db/create-message! record))

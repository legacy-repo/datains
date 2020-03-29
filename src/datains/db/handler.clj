(ns datains.db.handler
  (:require
   [datains.db.core :as db]
   [clojure.tools.logging :as log]))

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

(defn- page->offset [page per-page]
  "Tranform page to offset."
  (* (- page 1) per-page))

(defn- search-entities
  ([func-map] (search-entities func-map nil 1 10))
  ([func-map page] (search-entities func-map nil page 10))
  ([func-map page per-page] (search-entities func-map nil page per-page))
  ([func-map query-map page per-page]
   (let [page     (if (nil? page) 1 page)
         per-page (if (nil? per-page) 10 per-page)
         params   {:limit     per-page
                   :offset    (page->offset page per-page)
                   :query-map (filter-query-map query-map)}
         metadata {:total    (:count ((:count-func func-map) params))
                   :page     page
                   :per-page per-page}]
     (log/info "Query db by: " params)
     (merge metadata {:data ((:query-func func-map) params)}))))

;; --------------------- App Record ---------------------
(def search-apps
  (partial
   search-entities
   {:query-func db/search-apps
    :count-func db/get-app-count}))

(defn update-app! [id record]
  (db/update-app! {:updates record
                   :id      id}))

(defn delete-app! [id]
  (db/delete-app! {:id id}))

(defn create-app! [record]
  (db/create-app! record))

;; --------------------- Report Record ---------------------
(def search-reports
  (partial
   search-entities
   {:query-func db/search-reports
    :count-func db/get-report-count}))

(defn update-report! [id record]
  (db/update-report! {:updates record
                      :id      id}))

(defn delete-report! [id]
  (db/delete-report! {:id id}))

(defn create-report! [record]
  (db/create-report! record))

;; --------------------- Project Record ---------------------
(def search-projects
  (partial
   search-entities
   {:query-func db/search-projects
    :count-func db/get-project-count}))

(defn update-project! [id record]
  (db/update-project! {:updates record
                       :id      id}))

(defn delete-project! [id]
  (db/delete-project! {:id id}))

(defn create-project! [record]
  (db/create-project! record))

;; --------------------- Workflow Record ---------------------
(def search-workflows
  (partial
   search-entities
   {:query-func db/search-workflows
    :count-func db/get-workflow-count}))

(defn update-workflow! [id record]
  (db/update-workflow! {:updates record
                        :id      id}))

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

(defn update-notification! [id record]
  (db/update-notification! {:updates record
                            :id      id}))

(defn delete-notification! [id]
  (db/delete-notification! {:id id}))

(defn create-notification! [record]
  (db/create-notification! record))
(ns datains.db.handler
  (:require
   [datains.db.core :as db]))

(defn- filter-query-map [query-map]
  "Filter unqualified attribute or value."
  ; TODO: Add filter function chain.
  query-map)

(defn- page->offset [page per-page]
  "Tranform page to offset."
  (* (- page 1) per-page))

(defn- search-entities
  ([func-map] (search-entities func-map nil 1 10))
  ([func-map page] (search-entities func-map nil page 10))
  ([func-map page per-page] (search-entities func-map nil page per-page))
  ([func-map query-map page per-page]
   (let [page (if (nil? page) 1 page)
         per-page (if (nil? per-page) 10 per-page)
         params (filter #(some? (val %))
                        {:limit per-page
                         :offset (page->offset page per-page)
                         :query-map (filter-query-map query-map)})
         metadata {:total (:count ((:count-func func-map) params))
                   :page page
                   :per-page per-page}]
     (merge metadata {:data ((:query-func func-map) params)}))))

(def search-apps
  (partial
   search-entities
   {:query-func db/search-apps
    :count-func db/get-app-count}))

(defn update-app! [id record]
  (db/update-app! {:updates record :id id}))

(defn delete-app! [id]
  (db/delete-app! {:id id}))

(defn create-app! [record]
  (db/create-app! record))
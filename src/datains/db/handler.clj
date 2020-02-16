(ns datains.db.handler
  (:require
   [datains.db.core :as db]
   [datains.utils :as utils]))


(defn- search-items
  ([func page] (search-items func {} page 10))
  ([func page per-page] (search-items func {} page per-page))
  ([func query-map page per-page] (search-items func query-map {} page per-page))
  ([func query-map like-map page per-page]
   (let [page (if (nil? page) 1 page)
         per-page (if (nil? per-page) 10 per-page)
         query-map (if (nil? query-map) {} query-map)
         like-map (if (nil? like-map) {} like-map)
         query-str (utils/make-query-str query-map like-map)
         metadata {:total (:count (db/get-app-count))
                   :page page
                   :per-page per-page}]
     (merge metadata {:data (func query-str page per-page)}))))


(defn- search-apps [query-str page per-page]
  (db/search-apps {:query-str query-str
                   :per-page per-page
                   :offset (utils/get-offset page per-page)}))


(defn- search-tags [query-str page per-page]
  (db/search-tags {:query-str query-str
                   :per-page per-page
                   :offset (utils/get-offset page per-page)}))


(def get-apps (partial search-items search-apps))
(def get-tags (partial search-items search-tags))


(defn get-app [id] (first (get-apps {:id id} 1 1)))
(defn get-tag [id] (first (get-tags {:id id} 1 1)))


(defn delete-app! [id] (db/delete-app! {:id id}))
(defn delete-tag! [id] (db/delete-tag! {:id id}))

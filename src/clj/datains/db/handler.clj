(ns datains.db.handler
  (:require
   [datains.db.core :as db]
   [datains.utils :as utils]))


(defn- search-items
  ([func page] (search-items func "" page 10))
  ([func page per-page] (search-items func "" page per-page))
  ([func query-str page per-page]
   (let [page (if (nil? page) 1 page)
         per-page (if (nil? per-page) 10 per-page)
         query-str (if (nil? query-str) "" query-str)
         metadata {:total (:count (db/get-app-count))
                   :page page
                   :per-page per-page}]
     (merge metadata {:data (func query-str page per-page)}))))


(defn- search-apps [query-str page per-page]
  (if (not= (count query-str) 0)
    (db/search-apps {:title-like (str "%" query-str "%")
                     :per-page per-page
                     :offset (utils/get-offset page per-page)})
    (db/get-apps {:per-page per-page
                  :offset (utils/get-offset page per-page)})))


(defn- search-tags [query-str page per-page]
  (if (not= (count query-str) 0)
    (db/search-tags {:name-like (str "%" query-str "%")
                     :per-page per-page
                     :offset (utils/get-offset page per-page)})
    (db/get-tags {:per-page per-page
                  :offset (utils/get-offset page per-page)})))

(def get-apps (partial search-items search-apps))
(def get-tags (partial search-items search-tags))


(defn get-app [id] (db/get-app {:id id}))
(defn get-tag [id] (db/get-tag {:id id}))


(defn delete-app! [id] (db/delete-app! {:id id}))
(defn delete-tag! [id] (db/delete-tag! {:id id}))
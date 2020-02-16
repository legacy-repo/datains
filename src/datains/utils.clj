(ns datains.utils
  (:require
   [clojure.string :as cstr]))


(defn get-offset [page per-page]
  (* (- page 1) per-page))


(defn trim [query-str]
  (cstr/trim (cstr/replace query-str #"[\"]" "")))


(defn concat-str
  "Concat coll by a specified join-str."
  [coll join-str]
  (let [val (first coll)
        ret (rest coll)]
    (if (= (count ret) 0)
      val
      (str val join-str
           (concat-str ret join-str)))))


(defn map->coll
  "Transform hashmap to coll, the item of coll is key + join-str + value, e.g. key=value"
  [hashmap join-str]
  (map (fn [v]
         (let [[key value] v]
           (str (name key) join-str (str "'" value "'")))) hashmap))


(defn make-query-str
  "Transform query-map and like-map to a SQL string for WHERE clause."
  [query-map like-map]
  (str (concat-str (map->coll query-map "=") " AND ")
       " AND "
       (concat-str (map->coll like-map " LIKE ") " AND ")))
(ns datains.adapters.data-commons.core
  (:require [monger.core :as mg]
            [monger.query :as q]
            [monger.collection :as mcoll]
            [monger.internal.pagination :as mp]
            [datains.config :refer [env]])
  (:import [com.mongodb DB]))

(defn- connect
  [uri]
  (mg/connect-via-uri uri))

(defn- disconnect
  [conn]
  (mg/disconnect conn))

(defn reset-db-conn!
  [db-name]
  (let [db-inst (mg/get-db @conn db-name)]
    (reset! db db-inst)))

(def ^:private conn
  (atom nil))

(def ^:private db
  (atom nil))

(defn setup-connection!
  []
  (let [conn-info (connect (:mongo-uri env))]
    (reset! conn (:conn conn-info))
    (reset! db (:db conn-info))))

(defn stop-connection!
  []
  (mg/disconnect @conn)
  (reset! conn nil)
  (reset! db nil))

(defn with-collection
  ([coll query-coll]
   (let [db-coll (if (string? coll)
                   (.getCollection @db coll)
                   coll)
         empty-query (q/empty-query db-coll)
         query (apply merge empty-query query-coll)]
     (q/exec query)))
  ([coll] (with-collection coll [])))

(defn find-coll
  [query]
  {:query query})

(defn fields
  [flds]
  {:fields flds})

(defn sort-coll
  [srt]
  {:sort srt})

(defn skip
  [^long n]
  {:skip n})

(defn limit
  [^long n]
  {:limit n})

(defn batch-size
  [^long n]
  {:batch-size n})

(defn paginate
  [{:keys [page per-page] :or {page 1 per-page 10}}]
  {:limit per-page :skip (mp/offset-for page per-page)})

(defn hint
  [h]
  {:hint h})

(defn snapshot
  []
  {:snapshot true})

(defn count-group-by
  [coll group-name]
  (mcoll/aggregate @db coll [{"$group" {:_id (str "$" group-name)
                                        :total {"$sum" 1}}}]
                   :cursor {:batch-size 0}))

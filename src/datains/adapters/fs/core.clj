(ns datains.adapters.fs.core
  (:require [minio-clj.core :as mc]
            [clojure.tools.logging :as log]
            ; Need to import env environment
            [datains.config :refer [env]]))

(def ^:private conn
  (atom nil))

(def ^:private service
  (atom nil))

(def ^:private service-map
  {:minio {:make-bucket      mc/make-bucket
           :connect          mc/connect
           :list-buckets     mc/list-buckets
           :put-object       mc/put-object
           :get-object       mc/get-object
           :list-objects     mc/list-objects
           :remove-bucket    mc/remove-bucket!
           :remove-object    mc/remove-object!
           :get-upload-url   mc/get-upload-url
           :get-download-url mc/get-download-url
           :get-object-meta  mc/get-object-meta}})

(defn ^:private get-service
  [service]
  (let [services {:minio "minio"
                  :oss   "oss"
                  :s3    "s3"}]
    (if (services (keyword service))
      (keyword service)
      :minio)))

(defn ^:private get-fn
  [fn-keyword]
  (fn-keyword (service-map @service)))

(defn setup-connection
  []
  (reset! service (get-service (:fs-service env)))
  (reset! conn ((get-fn :connect) (:fs-endpoint env) (:fs-access-key env) (:fs-secret-key env))))

(defn make-bucket
  [^String name]
  ((get-fn :make-bucket) @conn name))

(defn list-buckets
  []
  ((get-fn :list-buckets) @conn))

(defn put-object!
  ([^String bucket ^String file-name]
   ((get-fn :put-object) @conn bucket file-name))
  ([^String bucket ^String upload-name ^String source-file-name]
   ((get-fn :put-object) @conn bucket upload-name source-file-name)))

(defn get-object
  [bucket key]
  ((get-fn :get-object) @conn bucket key))

(defn list-objects
  ([bucket]
   (list-objects bucket {}))
  ([bucket {:keys [prefix]
            :or   {prefix "second"}}]
   (filter (fn [object] (re-find (re-pattern (str "^" prefix)) (:Key object)))
           ((get-fn :list-objects) @conn bucket))))

(defn remove-bucket!
  [bucket]
  ((get-fn :remove-bucket) @conn bucket))

(defn remove-object!
  [bucket key]
  ((get-fn :remove-object) @conn bucket key))

(defn get-upload-url
  [bucket key]
  ((get-fn :get-upload-url) @conn bucket key))

(defn get-download-url
  [bucket key]
  ((get-fn :get-download-url) @conn bucket key))

(defn get-object-meta
  [bucket key]
  ((get-fn :get-object-meta) @conn bucket key))

(defn format-objects
  [bucket objects]
  (pmap (fn [object]
          (assoc object
                 :Path (str (:fs-rootdir env) "/" bucket "/" (:Key object)))) objects))
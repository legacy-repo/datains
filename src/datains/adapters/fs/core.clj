(ns datains.adapters.fs.core
  (:require [minio-clj.core :as mc]
            [oss-clj.core :as oss]
            ; [clojure.tools.logging :as log]
            [clojure.string :as str]
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
           :get-object-meta  mc/get-object-meta}
   :oss   {:make-bucket      oss/make-bucket
           :connect          oss/connect
           :list-buckets     oss/list-buckets
           :put-object       oss/put-object
           :get-object       oss/get-object
           :list-objects     oss/list-objects
           :remove-bucket    oss/remove-bucket!
           :remove-object    oss/remove-object!
           :get-upload-url   oss/get-upload-url
           :get-download-url oss/get-download-url
           :get-object-meta  oss/get-object-meta}})

(def ^:private protocol-map
  {:minio "s3://"
   :s3    "s3://"
   :oss   "oss://"})

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

(defn get-protocol
  []
  (protocol-map @service))

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
   (list-objects bucket))
  ([bucket prefix]
   ((get-fn :list-objects) @conn bucket prefix))
  ([bucket prefix recursive]
   ((get-fn :list-objects) @conn bucket prefix recursive)))

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
                 :Path (str (get-protocol) bucket "/" (:Key object)))) objects))

(defn correct-file-path
  "When you use minio service, all file paths need to reset as the local path.
   e.g. s3://bucket-name/object-key --> /datains/minio/bucket-name/object-key

   ; TODO: need to support more types for e's value.
  "
  [e]
  (let [protocol (get-protocol)
        prefix   (str/replace (:fs-rootdir env) #"([^\/])$" "$1/")
        pattern  (re-pattern protocol)
        func     (fn [string] (str/replace string pattern prefix))]
    (if (= @service :minio)
      (into {}
            (map (fn [[key value]]
                   (vector key
                           (cond
                             (map? value) (correct-file-path value)
                             (vector? value) (map #(func %) value)
                             (string? value) (func value)
                             :else value))) e))
      e)))

(defn correct-file-path-reverse
  "When you use minio service, all file paths need to reset as the local path.
   e.g. /datains/minio/bucket-name/object-key --> s3://bucket-name/object-key

   ; TODO: need to support more types for e's value.
  "
  [e]
  (let [protocol (get-protocol)
        pattern  (re-pattern (str/replace (:fs-rootdir env) #"([^\/])$" "$1/"))
        func     (fn [string] (str/replace string pattern protocol))]
    (if (= @service :minio)
      (into {}
            (map (fn [[key value]]
                   (vector key
                           (cond
                             (map? value) (correct-file-path value)
                             (vector? value) (map #(func %) value)
                             (string? value) (func value)
                             :else value))) e))
      e)))

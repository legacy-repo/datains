(ns datains.api.fs
  (:require
   [ring.util.http-response :refer [ok created no-content bad-request not-found]]
   [datains.api.fs-spec :as fs-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.adapters.fs.core :as fs]
   [datains.events :as events])
  (:import [java.io File]))

(def fs-service
  [""
   {:swagger {:tags ["File System Service"]}}

   ["/buckets"
    {:get  {:summary    "Get buckets"
            :parameters {}
            :responses  {200 {:body {:data any?}}}
            :handler    (fn [parameters] (ok {:data (fs/list-buckets)}))}

     :post {:summary    "Create a bucket."
            :parameters {:body {:name string?}}
            :responses  {201 {:body {:name string?}}}
            :handler    (fn [{{{:keys [name]} :body} :parameters}]
                          (created (str "/buckets/" name)
                                   {:name (fs/make-bucket name)}))}}]

   ["/buckets/:name"
    {:get    {:summary    "Get the objects of a bucket."
              :parameters {:path  fs-spec/bucket-name-spec
                           :query fs-spec/bucket-params-query}
              :responses  {200 {:body {:total    nat-int?
                                       :page     pos-int?
                                       :per_page pos-int?
                                       :data     any?}}}
              :handler    (fn [{{{:keys [name]}          :path
                                 {:keys [page per_page]} :query} :parameters}]
                            (let [objects  (fs/list-objects name)
                                  page     (if (nil? page) 1 page)
                                  per_page (if (nil? per_page) 10 per_page)]
                              (log/debug "page: " page, "per-page: " per_page)
                              (ok {:data     (fs/format-objects name
                                                                (->> (drop (* (- page 1) per_page) objects)
                                                                     (take per_page)))
                                   :page     page
                                   :per_page per_page
                                   :total    (count objects)})))}
     :post   {:summary    "Create an directory in a bucket."
              :parameters {:path  fs-spec/bucket-name-spec
                           :query {:key string?}}
              :responses  {201 {:body {:bucket string?
                                       :name   string?}}}
              :handler    (fn [{{{:keys [key]}  :query
                                 {:keys [name]} :path} :parameters}]
                            (let [key      (str key "/")
                                  tempfile (.getPath (File/createTempFile "tempfile" "txt"))
                                  object   (fs/put-object! name key tempfile)]
                              (created (str "/buckets/" name "/object-meta" "?key=" key) object)))}
     :delete {:summary    "Delete the bucket."
              :parameters {:path fs-spec/bucket-name-spec}
              :responses  {204 {:body any?}
                           400 {:body {:message string?}}}
              :handler    (fn [{{{:keys [name]} :path} :parameters}]
                            (try
                              (fs/remove-bucket! name)
                              (no-content)
                              (catch Exception e
                                (bad-request {:message "The bucket you tried to delete is not empty."}))))}}]

   ["/buckets/:name/object"
    {:get    {:summary    "Get the download url of object."
              :parameters {:path  fs-spec/bucket-name-spec
                           :query {:key string?}}
              :responses  {200 {:body {:download_url string?}}}
              :handler    (fn [{{{:keys [key]}  :query
                                 {:keys [name]} :path} :parameters}]
                            (ok {:download_url (fs/get-download-url name key)}))}
     :post   {:summary    "Create an upload url for an object."
              :parameters {:path  fs-spec/bucket-name-spec
                           :query {:key string?}}
              :responses  {201 {:body {:upload_url string?}}}
              :handler    (fn [{{{:keys [key]}  :query
                                 {:keys [name]} :path} :parameters}]
                            (let [upload-url (fs/get-upload-url name key)]
                              (created upload-url {:upload_url upload-url})))}
     :delete {:summary    "Delete an object."
              :parameters {:path  fs-spec/bucket-name-spec
                           :query {:key string?}}
              :responses  {204 {:body any?}}
              :handler    (fn [{{{:keys [key]}  :query
                                 {:keys [name]} :path} :parameters}]
                            (fs/remove-object! name key)
                            (no-content))}}]

   ["/buckets/:name/object-meta"
    {:get {:summary    "Get the meta of an object."
           :parameters {:path  fs-spec/bucket-name-spec
                        :query {:key string?}}
           :responses  {200 {:body {:meta any?}}
                        404 {:body {:message string?}}}
           :handler    (fn [{{{:keys [key]}  :query
                              {:keys [name]} :path} :parameters}]
                         (try
                           (ok {:meta (fs/get-object-meta name key)})
                           (catch Exception e
                             (not-found {:message "Object does not exist"}))))}}]])

(ns datains.api.log
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.log-spec :as log-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.events :as events]
   [datains.util :as util]))

(def log
  [""
   {:swagger {:tags ["Log Management"]}}

   ["/logs"
    {:get  {:summary    "Get logs."
            :parameters {:query log-spec/log-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per_page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per_page]} :query} :parameters}]
                            (log/debug "page: " page, "per-page: " per_page)
                            (ok (db-handler/search-logs {:query-map {}} page per_page)))}

     :post {:summary    "Create a log."
            :parameters {:body log-spec/log-body}
            :responses  {201 {:body {:message {:id pos-int?}}}}
            :handler    (fn [{{:keys [body]} :parameters}]
                          (log/debug "Create a log: " body)
                          (created (str "/logs/" (:id body))
                                   {:message (db-handler/create-log! body)}))}}]

   ["/logs/:id"
    {:get    {:summary    "Get a log by id."
              :parameters {:path {:id pos-int?}}
              :responses  {200 {:body map?}}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (log/debug "Get log: " id)
                            (ok (db-handler/search-logs id)))}

     :delete {:summary    "Delete a log."
              :parameters {:path {:id pos-int?}}
              :responses  {204 nil}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (db-handler/delete-log! id)
                            (no-content))}}]])
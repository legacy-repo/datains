(ns datains.api.report
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.spec :as db-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.events :as events]
   [datains.util :as util]))

(def report
  [""
   {:swagger {:tags ["Report Management"]}}

   ["/reports"
    {:get  {:summary    "Get reports."
            :parameters {:query db-spec/report-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per-page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per-page project-id rstatus report-type]} :query} :parameters}]
                          (let [query-map {:project-id  project-id
                                           :status      rstatus
                                           :report-type report-type}]
                            (log/debug "page: " page, "per-page: " per-page, "query-map: " query-map)
                            (ok (db-handler/search-reports query-map
                                                           page
                                                           per-page))))}

     :post {:summary    "Create an report."
            :parameters {:body db-spec/report-body}
            :responses  {201 {:body {:message db-spec/uuid-spec}}}
            :handler    (fn [{{:keys [body]} :parameters}]
                          (let [body (merge body {:id (util/uuid)})]
                            (log/debug "Create an report: " body)
                            (created (str "/reports/" (:id body))
                                     {:message (db-handler/create-report! body)})))}}]

   ["/reports/::uuid"
    {:get    {:summary    "Get a report by id."
              :parameters {:path db-spec/uuid-spec}
              :responses  {200 {:body map?}}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (log/debug "Get report: " id)
                            (ok (db-handler/search-report id)))}

     :delete {:summary    "Delete a report."
              :parameters {:path db-spec/uuid-spec}
              :responses  {204 nil}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (db-handler/delete-report! id)
                            (no-content))}}]])
(ns datains.api.project
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.spec :as db-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.events :as events]
   [datains.util :as util]))

(def project
  [""
   {:swagger {:tags ["Project Management"]}}

   ["/projects"
    {:get  {:summary    "Get projects."
            :parameters {:query db-spec/project-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per-page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per-page app-id author status]} :query} :parameters}]
                          (let [query-map {:app-id app-id
                                           :status status
                                           :author author}]
                            (log/debug "page: " page, "per-page: " per-page, "query-map: " query-map)
                            (ok (db-handler/search-projects query-map
                                                            page
                                                            per-page))))}

     :post {:summary    "Create an project."
            :parameters {:body db-spec/project-body}
            :responses  {201 {:body {:message {:id string?}}}}
            :handler    (fn [{{{:keys [body]} :body} :parameters}]
                          (let [body (merge body {:id (util/uuid)})]
                            (log/debug "Create an project: " body)
                            (created (str "/projects/" (:id body))
                                     {:message (db-handler/create-project! body)})))}}]

   ["/projects/::uuid"
    {:get    {:summary    "Get a project by id."
              :parameters {:path db-spec/uuid-spec}
              :responses  {200 {:body map?}}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (log/debug "Get project: " id)
                            (ok (db-handler/search-project id)))}

     :put    {:summary    "Modify a project record."
              :parameters {:path db-spec/uuid-spec
                           :body db-spec/project-body}
              :responses  {204 nil}
              :handler    (fn [{{{:keys [finished-time status]} :body} :parameters}
                               {{{:keys [id]} :path} :parameters}]
                            (db-handler/update-project! id {:finished-time finished-time
                                                            :status        status}))}

     :delete {:summary    "Delete a project."
              :parameters {:path db-spec/uuid-spec}
              :responses  {204 nil}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (db-handler/delete-project! id)
                            (no-content))}}]])
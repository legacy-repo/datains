(ns datains.api.project
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.project-spec :as project-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.events :as events]
   [datains.util :as util]))

(def project
  [""
   {:swagger {:tags ["Project Management"]}}

   ["/projects"
    {:get  {:summary    "Get projects."
            :parameters {:query project-spec/project-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per-page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per-page app-id author status project-name]} :query} :parameters}]
                          (let [query-map {:app_id       app-id
                                           :status       status
                                           :author       author
                                           :project_name project-name}]
                            (log/debug "page: " page, "per-page: " per-page, "query-map: " query-map)
                            (ok (db-handler/search-projects query-map
                                                            page
                                                            per-page))))}

     :post {:summary    "Create an project."
            :parameters {:body project-spec/project-body}
            :responses  {201 {:body {:message {:id string?}}}}
            :handler    (fn [{{:keys [body]} :parameters}]
                          (let [body (util/merge-diff-map body {:id            (util/uuid)
                                                                :description   ""
                                                                :group-name    "Choppy Team"
                                                                :status        "Submitted"
                                                                :finished-time nil
                                                                :percentage    0
                                                                :started-time  (util/time->int (util/now))})]
                            (log/debug "Create an project: " body)
                            (created (str "/projects/" (:id body))
                                     {:message (db-handler/create-project-workflow! body)})))}}]

   ["/projects/:uuid"
    {:get    {:summary    "Get a project by id."
              :parameters {:path project-spec/uuid-spec}
              :responses  {200 {:body map?}}
              :handler    (fn [{{{:keys [uuid]} :path} :parameters}]
                            (log/debug "Get project: " uuid)
                            (ok (db-handler/search-project uuid)))}

     :put    {:summary    "Modify a project record."
              :parameters {:path project-spec/uuid-spec
                           :body project-spec/project-put-body}
              :responses  {204 nil}
              :handler    (fn [{{:keys [body path]} :parameters}]
                            (let [uuid (:uuid path)]
                              (log/debug "Update project: " uuid body)
                              (db-handler/update-project! uuid body)
                              (no-content)))}

     :delete {:summary    "Delete a project."
              :parameters {:path project-spec/uuid-spec}
              :responses  {204 nil}
              :handler    (fn [{{{:keys [uuid]} :path} :parameters}]
                            (db-handler/delete-project! uuid)
                            (no-content))}}]])
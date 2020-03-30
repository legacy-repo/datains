(ns datains.api.workflow
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.workflow-spec :as workflow-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.events :as events]
   [datains.util :as util]))

(def workflow
  [""
   {:swagger {:tags ["Workflow Management"]}}

   ["/workflows"
    {:get  {:summary    "Get workflows."
            :parameters {:query workflow-spec/workflow-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per-page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per-page project-id status]} :query} :parameters}]
                          (let [query-map {:project_id project-id
                                           :status     status}]
                            (log/debug "page: " page, "per-page: " per-page, "query-map: " query-map)
                            (ok (db-handler/search-workflows query-map
                                                             page
                                                             per-page))))}

     :post {:summary    "Create an workflow."
            :parameters {:body workflow-spec/workflow-body}
            :responses  {201 {:body {:message workflow-spec/workflow-id}}}
            :handler    (fn [{{:keys [body]} :parameters}]
                          (let [body (merge body {:id (util/uuid)})]
                            (log/debug "Create an workflow: " body)
                            (created (str "/workflows/" (:id body))
                                     {:message (db-handler/create-workflow! body)})))}}]

   ["/workflows/:id"
    {:get    {:summary    "Get a workflow by id."
              :parameters {:path workflow-spec/workflow-id}
              :responses  {200 {:body map?}}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (log/debug "Get workflow: " id)
                            (ok (db-handler/search-workflow id)))}

     :put    {:summary    "Modify a workflow record."
              :parameters {:path workflow-spec/workflow-id
                           :body workflow-spec/workflow-put-body}
              :responses  {204 nil}
              :handler    (fn [{{:keys [body path]} :parameters}]
                            (let [id (:id path)]
                              (log/debug "Update workflow: " id body)
                              (db-handler/update-workflow! id body)
                              (no-content)))}

     :delete {:summary    "Delete a workflow."
              :parameters {:path workflow-spec/workflow-id}
              :responses  {204 nil}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (db-handler/delete-workflow! id)
                            (no-content))}}]])
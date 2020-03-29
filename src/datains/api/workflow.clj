(ns datains.api.workflow
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.spec :as db-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.events :as events]
   [datains.util :as util]))

(def workflow
  [""
   {:swagger {:tags ["Workflow Management"]}}

   ["/workflows"
    {:get  {:summary    "Get workflows."
            :parameters {:query db-spec/workflow-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per-page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per-page project-id status]} :query} :parameters}]
                          (let [query-map {:project-id project-id
                                           :status     status}]
                            (log/debug "page: " page, "per-page: " per-page, "query-map: " query-map)
                            (ok (db-handler/search-workflows query-map
                                                             page
                                                             per-page))))}

     :post {:summary    "Create an workflow."
            :parameters {:body db-spec/workflow-body}
            :responses  {201 {:body {:message {:id util/uuid?}}}}
            :handler    (fn [{{:keys [body]} :parameters}]
                          (let [body (merge body {:id (util/uuid)})]
                            (log/debug "Create an workflow: " body)
                            (created (str "/workflows/" (:id body))
                                     {:message (db-handler/create-workflow! body)})))}}]

   ["/workflows/:id"
    {:get    {:summary    "Get a workflow by id."
              :parameters {:path {:id util/uuid?}}
              :responses  {200 {:body map?}}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (let [query-map {:id id}]
                              (log/debug "Get workflow: " id)
                              (ok (first (:data (db-handler/search-workflows query-map 1 1))))))}

     :delete {:summary    "Delete a workflow."
              :parameters {:path {:id util/uuid?}}
              :responses  {204 nil}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (db-handler/delete-workflow! id))}}]])
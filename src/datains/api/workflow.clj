(ns datains.api.workflow
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.workflow-spec :as workflow-spec]
   [clojure.tools.logging :as log]
   [datains.config :refer [env]]
   [datains.adapters.cromwell.core :as cromwell]
   [clj-filesystem.core :as fs]
   [datains.util :as util]))

(def workflow
  [""
   {:swagger {:tags ["Workflow Management"]}}

   ["/workflows"
    {:get  {:summary    "Get workflows."
            :parameters {:query workflow-spec/workflow-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per_page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per_page project_id status]} :query} :parameters}]
                          (let [query-map {:project_id project_id
                                           :status     status}]
                            (log/debug "page: " page, "per-page: " per_page, "query-map: " query-map)
                            (ok (db-handler/search-workflows {:query-map query-map}
                                                             page
                                                             per_page))))}

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
                            ;; TODO: Need to fix the bug - cromwell workflow id is not suitable with search-workflow
                            ;;       Add another search-workflow function or just let it go?
                            (ok (merge (db-handler/search-workflow id)
                                       ; Need to correct filepath when on local mode and datains-workdir must be the same with fs service.
                                       (fs/correct-file-path-reverse (cromwell/workflow-output id) (:datains-workdir env)))))}

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
                            (no-content))}}]

   ["/workflows/:id/logs"
    {:get {:summary    "Get all logs by workflow id."
           :parameters {:path workflow-spec/workflow-id}
           :responses  {200 {:body map?}}
           :handler    (fn [{{{:keys [id]} :path} :parameters}]
                         (log/debug "Get workflow logs: " id)
                         (let [logs (cromwell/list-task-logs id)]
                           (ok (if (nil? logs) {:message "Not Found"} logs))))}}]

   ["/workflows/:id/filecontent"
    {:get {:summary "Get content of a file that is related with a specified workflow."
           :parameters {:path workflow-spec/workflow-id
                        :query {:path string?}}
           :responses {200 {:body map?}}
           :handler (fn [{{{:keys [id]} :path
                           {:keys [path]} :query} :parameters}]
                      (log/info "Get workflow's log file: " id path)
                      ;; Need more restricted condition
                      (let [{:keys [service bucket object-key]} (util/parse-path path)]
                        (log/info "Parse path: " service bucket object-key)
                        (ok
                         (if (and service bucket object-key)
                           {:message (slurp (fs/with-conn service (fs/get-object bucket object-key)))}
                           {:message "Not Found"}))))}}]])
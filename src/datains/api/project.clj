(ns datains.api.project
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.db.core :as db]
   [honeysql.core :as sql]
   [datains.api.project-spec :as project-spec]
   [clojure.tools.logging :as log]
   [clojure.string :as clj-str]
   [datains.util :as util]))

(def project
  [""
   {:swagger {:tags ["Project Management"]}}

   ["/projects"
    {:get  {:summary    "Get projects."
            :parameters {:query project-spec/project-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per_page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per_page app_id author status project_name]} :query} :parameters
                              {:as headers} :headers}]
                          (let [query-map {:app_id       app_id
                                           :status       status
                                           :author       author
                                           :project_name project_name}
                                auth-users (get headers "x-auth-users")
                                authors (if auth-users (clj-str/split auth-users #",") nil)
                                where-clause (db-handler/make-where-clause "datains-project"
                                                                           query-map
                                                                           [:in :datains-project.author authors])
                                query-clause (if authors
                                               {:where-clause
                                                (sql/format {:where where-clause})}
                                               {:query-map query-map})]
                            (log/info "page: " page, "per-page: " per_page, "query-clause: " where-clause)
                            (ok (db-handler/search-projects query-clause page per_page))))}

     :post {:summary    "Create an project."
            :parameters {:body project-spec/project-body}
            :responses  {201 {:body {:message {:id string?}}}}
            :handler    (fn [{{:keys [body]} :parameters}]
                          (let [body (util/merge-diff-map body {:id            (util/uuid)
                                                                :description   ""
                                                                :group_name    "Choppy Team"
                                                                :author        "Choppy"
                                                                :status        "Submitted"
                                                                :finished_time nil
                                                                :percentage    0
                                                                :started_time  (util/time->int (util/now))})]
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
                            (no-content))}}]

   ["/projects/:uuid/stats"
    {:get {:summary    "Get a project's stats by id."
           :parameters {:path project-spec/uuid-spec}
           :responses  {200 {:body map?}}
           :handler    (fn [{{{:keys [uuid]} :path} :parameters}]
                         (log/debug "Get project stats: " uuid)
                         (ok (let [resp (db-handler/count-workflow-with-status uuid)]
                               (if (nil? resp)
                                 {}
                                 resp))))}}]])
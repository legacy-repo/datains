(ns datains.api.choppy-app
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.spec :as db-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.events :as events]))

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
            :responses  {201 {:body {:message int?}}}
            :handler    (fn [{{:body } :parameters}]
                          (log/debug "Create an project: " body)
                          (created (str "/projects/" (:id body))
                                   {:message (db-handler/create-project! body)}))}}]])
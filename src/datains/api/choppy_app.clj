(ns datains.api.choppy-app
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.spec :as db-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.events :as events]))

(def app
  [""
   {:swagger {:tags ["App Store"]}}

   ["/apps"
    {:get {:summary "Get apps."
           :parameters {:query db-spec/params-query}
           :responses {200 {:body {:total nat-int?
                                   :page pos-int?
                                   :per-page pos-int?
                                   :data any?}}}
           :handler (fn [{{{:keys [page per-page query-map]} :query} :parameters}]
                      (log/debug "page: " page, "per-page: " per-page, "query-map: " query-map)
                      (events/publish-event! :app-update "Choppy apps are synced from choppy.3steps.cn")
                      (ok (db-handler/search-apps query-map
                                                  page
                                                  per-page)))}

     :post {:summary "Create an app."
            :parameters {:body db-spec/app-body}
            :responses {201 {:body {:message int?}}}
            :handler (fn [{{{:keys [id title description repo_url cover icon author rate]} :body} :parameters}]
                       (created (str "/apps/" id)
                                {:message (db-handler/create-app! {:id id
                                                                   :title title
                                                                   :description description
                                                                   :repo-url repo_url
                                                                   :cover cover
                                                                   :icon icon
                                                                   :author author
                                                                   :rate rate})}))}}]])
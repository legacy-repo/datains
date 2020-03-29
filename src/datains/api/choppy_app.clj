(ns datains.api.choppy-app
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.spec :as db-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]
   [datains.events :as events]
   [digest :as digest]
   [datains.adapters.app-store.core :as app-store]))

(def app
  [""
   {:swagger {:tags ["App Store"]}}

   ["/apps"
    {:get  {:summary    "Get apps."
            :parameters {:query db-spec/app-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per-page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per-page title valid author]} :query} :parameters}]
                          (let [query-map {:title  title
                                           :valid  valid
                                           :author author}]
                            (log/debug "page: " page, "per-page: " per-page, "query-map: " query-map)
                            (ok (db-handler/search-apps query-map
                                                        page
                                                        per-page))))}

     :post {:summary    "Create an app."
            :parameters {:body db-spec/app-body}
            :responses  {201 {:body {:message {:id string?}}}}
            :handler    (fn [{{{:keys [id title description repo_url cover icon author rate valid]} :body} :parameters}]
                          (created (str "/apps/" id)
                                   {:message (db-handler/create-app! {:id          id
                                                                      :title       title
                                                                      :description description
                                                                      :repo-url    repo_url
                                                                      :cover       cover
                                                                      :icon        icon
                                                                      :author      author
                                                                      :rate        rate
                                                                      :valid       valid})}))}}]
   ["/installed-apps"
    {:get {:summary    "Get installed apps."
           :parameters {}
           :responses  {200 {:body {:total nat-int?
                                    :data  any?}}}
           :handler    (fn [parameters]
                         (let [apps (app-store/get-installed-apps (app-store/get-app-workdir))]
                           (ok {:data  (map #(assoc {} :name % :id (digest/md5 %)) apps)
                                :total (count apps)})))}}]])
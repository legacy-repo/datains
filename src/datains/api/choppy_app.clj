(ns datains.api.choppy_app
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.db.core :as db]
   [datains.api.spec :as db-spec]
   [clojure.tools.logging :as log]
   [datains.api.response :as response]))

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
           :handler (fn [{{{:keys [page per-page name query-map]} :query} :parameters}]
                      (log/debug "page: " page, "per-page: " per-page, "name: " name, "query-map: " query-map)
                      (ok (db-handler/get-apps query-map 
                                               (if name 
                                                 {:name (str "%" name "%")}
                                                 nil)
                                               page 
                                               per-page)))}

     :post {:summary "Create an app."
            :parameters {:body db-spec/app-body}
            :responses {201 {:body {:message int?}}}
            :handler (fn [{{{:keys [id title description repo_url cover icon author rate]} :body} :parameters}]
                       (created (str "/apps/" id)
                                {:message (db/create-app! {:id id
                                                           :title title
                                                           :description description
                                                           :repo_url repo_url
                                                           :cover cover
                                                           :icon icon
                                                           :author author
                                                           :rate rate})}))}}]
   ["/apps/:id"
    {:get {:summary "Get an app record given the id."
           :parameters {:path {:id string?}}
           :responses {200 {:body map?}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (log/debug "Queried app id: " id)
                      (response/ok-or-not-found (db-handler/get-app id)))}
     :delete {:summary "Delete an app record given the id."
              :parameters {:path {:id string?}}
              :responses {204 {:body string?}}
              :handler (fn [{{{:keys [id]} :path} :parameters}]
                         (log/debug "App id: " id)
                         (db-handler/delete-app! id)
                         (no-content))}}]])

(def tag
  [""
   {:swagger {:tags ["App Store"]}}

   ["/tags"
    {:get {:summary "Get tags."
           :parameters {:query db-spec/params-query}
           :responses {200 {:body {:total nat-int?
                                   :page pos-int?
                                   :per-page pos-int?
                                   :data any?}}}
           :handler (fn [{{{:keys [page per-page name query-map]} :query} :parameters}]
                      (log/debug "page: " page, "per-page: " per-page, "name: " name, "query-map: " query-map)
                      (ok (db-handler/get-tags query-map {:name (str "%" name "%")} page per-page)))}

     :post {:summary "Create an tag."
            :parameters {:body db-spec/tag-body}
            :responses {201 {:body {:message int?}}}
            :handler (fn [{{{:keys [name category]} :body} :parameters}]
                       (created "" {:message (db/create-tag! {:name name
                                                              :category category})}))}}]

   ["/tags/:id"
    {:get {:summary "Get a tag record."
           :parameters {:path {:id int?}}
           :responses {200 {:body map?}
                       404 {:body {:message string?}}}
           :handler (fn [{{{:keys [id]} :path} :parameters}]
                      (log/debug "Queried tag id: " id)
                      (response/ok-or-not-found (db-handler/get-tag id)))}
     :delete {:summary "Delete an tag record given the id."
              :parameters {:path {:id int?}}
              :responses {204 {:body string?}}
              :handler (fn [{{{:keys [id]} :path} :parameters}]
                         (log/debug "Tag id: " id)
                         (db-handler/delete-tag! id)
                         (no-content))}}]])
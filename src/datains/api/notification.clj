(ns datains.api.notification
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.notification-spec :as notification-spec]
   [clojure.tools.logging :as log]
   [datains.util :as util]))

(def notification
  [""
   {:swagger {:tags ["Notification Management"]}}

   ["/notifications"
    {:get  {:summary    "Get notifications."
            :parameters {:query notification-spec/notification-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per_page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per_page status notification_type]} :query} :parameters}]
                          (let [query-map {:status            status
                                           :notification_type notification_type}]
                            (log/debug "page: " page, "per-page: " per_page, "query-map: " query-map)
                            (ok (db-handler/search-notifications {:query-map query-map}
                                                                 page
                                                                 per_page))))}

     :post {:summary    "Create a notification."
            :parameters {:body notification-spec/notification-body}
            :responses  {201 {:body {:message {:id pos-int?}}}}
            :handler    (fn [{{:keys [body]} :parameters}]
                          (log/debug "Create a notification: " body)
                          (let [created-time (util/time->int (util/now))
                                id (util/uuid)
                                body (util/merge-diff-map body {:create_time created-time})]
                            (created (str "/notifications/" id)
                                     {:message (db-handler/create-notification! body)})))}}]

   ["/notifications/:id"
    {:get    {:summary    "Get a notification by id."
              :parameters {:path {:id pos-int?}}
              :responses  {200 {:body map?}}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (log/debug "Get notification: " id)
                            (ok (db-handler/search-notifications id)))}

     :put    {:summary    "Modify a notification record."
              :parameters {:path {:id pos-int?}
                           :body notification-spec/notification-put-body}
              :responses  {204 nil}
              :handler    (fn [{{:keys [body path]} :parameters}]
                            (let [id (:id path)]
                              (log/debug "Update notification: " id body)
                              (db-handler/update-notification! id body)
                              (no-content)))}

     :delete {:summary    "Delete a notification."
              :parameters {:path {:id pos-int?}}
              :responses  {204 nil}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (db-handler/delete-notification! id)
                            (no-content))}}]])
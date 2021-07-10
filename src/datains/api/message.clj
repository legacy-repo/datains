(ns datains.api.message
  (:require
   [ring.util.http-response :refer [ok created no-content]]
   [datains.db.handler :as db-handler]
   [datains.api.message-spec :as message-spec]
   [clojure.tools.logging :as log]
   [datains.util :as util]))

(def message
  [""
   {:swagger {:tags ["Message Management"]}}

   ["/messages"
    {:get  {:summary    "Get messages."
            :parameters {:query message-spec/message-params-query}
            :responses  {200 {:body {:total    nat-int?
                                     :page     pos-int?
                                     :per_page pos-int?
                                     :data     any?}}}
            :handler    (fn [{{{:keys [page per_page status message_type]} :query} :parameters}]
                          (let [query-map {:status            status
                                           :message_type message_type}]
                            (log/debug "page: " page, "per-page: " per_page, "query-map: " query-map)
                            (ok (db-handler/search-messages {:query-map query-map}
                                                            page
                                                            per_page))))}

     :post {:summary    "Create a message."
            :parameters {:body message-spec/message-body}
            :responses  {201 {:body {:message {:id pos-int?}}}}
            :handler    (fn [{{:keys [body]} :parameters}]
                          (log/debug "Create a message: " body)
                          (let [created-time (util/time->int (util/now))
                                id (util/uuid)
                                body (util/merge-diff-map body {:create_time created-time})]
                            (created (str "/messages/" id)
                                     {:message (db-handler/create-message! body)})))}}]

   ["/messages/:id"
    {:get    {:summary    "Get a message by id."
              :parameters {:path {:id pos-int?}}
              :responses  {200 {:body map?}}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (log/debug "Get message: " id)
                            (ok (db-handler/search-messages id)))}

     :put    {:summary    "Modify a message record."
              :parameters {:path {:id pos-int?}
                           :body message-spec/message-put-body}
              :responses  {204 nil}
              :handler    (fn [{{:keys [body path]} :parameters}]
                            (let [id (:id path)]
                              (log/debug "Update message: " id body)
                              (db-handler/update-message! id body)
                              (no-content)))}

     :delete {:summary    "Delete a message."
              :parameters {:path {:id pos-int?}}
              :responses  {204 nil}
              :handler    (fn [{{{:keys [id]} :path} :parameters}]
                            (db-handler/delete-message! id)
                            (no-content))}}]])
(ns datains.events.notification-dingtalk
  (:require [clojure.core.async :as async]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [selmer.parser :refer [render]]
            [datains.events :as events]
            [datains.adapters.dingtalk :as dingtalk]))

(def ^:const notifications-topics
  "The `Set` of event topics which are subscribed to for use in notifications tracking."
  #{:app-update
    :request-materials-dingtalk})

(def ^:private notifications-channel
  "Channel for receiving event notifications we want to subscribe to for notifications events."
  (async/chan))

;;; ------------------------------------------------ Event Processing ------------------------------------------------
(defn get-template
  []
  (slurp "http://chinese-quartet.org/dingtalk-templates/request-materials.md"))

(defn format-email-content
  [{:keys [requestor-email requestor-title requestor-name organization materials-type tubes notes]}]
  (render (get-template)
          {:info {:requestor_email requestor-email
                  :requestor_title requestor-title
                  :requestor_name requestor-name
                  :organization organization
                  :materials_type materials-type
                  :tubes tubes
                  :notes notes}}))

(defn- send-notification! [title object]
  (log/info title object (dingtalk/string-to-sign (System/currentTimeMillis)))
  (dingtalk/send-markdown-msg! title object))

(defn- process-notifications-event!
  "Handle processing for a single event notification received on the notifications-channel"
  [notification-event]
  ;; try/catch here to prevent individual topic processing exceptions from bubbling up.  better to handle them here.
  (try
    (when-let [{topic :topic object :item} notification-event]
      ;; TODO: only if the definition changed??
      (case (events/topic->model topic)
        "app"  (send-notification! "App Synced" object))
      (case topic
        :request-materials-dingtalk (send-notification! "Request Materials" (format-email-content object))))
    (catch Throwable e
      (log/warn (format "Failed to process notifications event. %s" (:topic notification-event)) e))))

;;; --------------------------------------------------- Lifecycle ----------------------------------------------------

(defn events-init
  "Automatically called during startup; start event listener for notifications events."
  []
  (events/start-event-listener! notifications-topics notifications-channel process-notifications-event!))

(ns datains.events.dingtalk
  (:require [clojure.core.async :as async]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [datains.events :as events]
            [datains.adapters.dingtalk :as dingtalk]))

(def ^:const notifications-topics
  "The `Set` of event topics which are subscribed to for use in notifications tracking."
  #{:app-update})

(def ^:private notifications-channel
  "Channel for receiving event notifications we want to subscribe to for notifications events."
  (async/chan))

;;; ------------------------------------------------ Event Processing ------------------------------------------------

(defn- send-notification! [title object]
  (log/info title object (dingtalk/string-to-sign))
  (dingtalk/send-markdown-msg! title object))

(defn- process-notifications-event!
  "Handle processing for a single event notification received on the notifications-channel"
  [notification-event]
  ;; try/catch here to prevent individual topic processing exceptions from bubbling up.  better to handle them here.
  (try
    (when-let [{topic :topic object :item} notification-event]
      ;; TODO: only if the definition changed??
      (case (events/topic->model topic)
        "app"  (send-notification! "App Synced" object)))
    (catch Throwable e
      (log/warn (format "Failed to process notifications event. %s" (:topic notification-event)) e))))

;;; --------------------------------------------------- Lifecycle ----------------------------------------------------

(defn events-init
  "Automatically called during startup; start event listener for notifications events."
  []
  (events/start-event-listener! notifications-topics notifications-channel process-notifications-event!))

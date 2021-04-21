(ns datains.events.notification-email
  (:require [postal.core :refer [send-message]]
            [clojure.core.async :as async]
            [clojure.tools.logging :as log]
            [datains.events :as events]))

(def ^:const notifications-topics
  "The `Set` of event topics which are subscribed to for use in notifications tracking."
  #{:request-materials
    :request-data})

(def ^:private notifications-channel
  "Channel for receiving event notifications we want to subscribe to for notifications events."
  (async/chan))

;;; ------------------------------------------------ Event Processing ------------------------------------------------

(defn- send-notification! [name object]
  (name object))

(defn- process-notifications-event!
  "Handle processing for a single event notification received on the notifications-channel"
  [notification-event]
  ;; try/catch here to prevent individual topic processing exceptions from bubbling up.  better to handle them here.
  (try
    (when-let [{topic :topic object :item} notification-event]
      ;; TODO: only if the definition changed??
      (case topic
        :request-materials (send-notification! "materials" object)
        :request-data (send-notification! "data" object)))
    (catch Throwable e
      (log/warn (format "Failed to process notifications event. %s" (:topic notification-event)) e))))

;;; --------------------------------------------------- Lifecycle ----------------------------------------------------

(defn events-init
  "Automatically called during startup; start event listener for notifications events."
  []
  (events/start-event-listener! notifications-topics notifications-channel process-notifications-event!))


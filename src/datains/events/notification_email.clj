(ns datains.events.notification-email
  (:require [postal.core :refer [send-message]]
            [clojure.core.async :as async]
            [datains.config :refer [env]]
            [clojure.tools.logging :as log]
            [datains.events :as events]))

(defn email-setting
  []
  (:email env))

(def ^:const notifications-topics
  "The `Set` of event topics which are subscribed to for use in notifications tracking."
  #{:request-materials
    :request-data})

(def ^:private notifications-channel
  "Channel for receiving event notifications we want to subscribe to for notifications events."
  (async/chan))

;;; ------------------------------------------------ Event Processing ------------------------------------------------
(defn format-materials-content
  [{:keys [requestor-email requestor-title requestor-name organization materials-type tubes notes]}]
  (str (format "Requestor: %s %s\n" requestor-title requestor-name)
       (format "Requestor Email: %s\n" requestor-email)
       (format "Organization: %s\n" organization)
       (format "Materials Type: %s\n" materials-type)
       (format "Tubes: %s\n" tubes)
       (format "Notes: %s\n" notes)))

(defn- send-notification! 
  "Example:
   {:requestor-name \"Yang\", 
    :tubes 2, 
    :requestor-title \"Dr.\", 
    :requestor-email \"yjcyxky@163.com\", 
    :purpose \"This is a test\", 
    :materials-type \"DNA\", 
    :organization \"Fudan University\", 
    :notes \"This is a test\"}"
  [name object]
  (let [setting (email-setting)]
    (log/info (format "%s %s %s" name object setting))
    (send-message setting {:from (:user setting)
                           :to (:requestor-email object)
                           :subject (format "[Request %s] %s" name (:purpose object))
                           :body (format-materials-content object)})))

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


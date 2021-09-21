(ns datains.events.notification-email
  (:require [postal.core :refer [send-message]]
            [clojure.core.async :as async]
            [datains.config :refer [env]]
            [selmer.parser :refer [render]]
            [clojure.tools.logging :as log]
            [datains.events :as events]))

(defn email-setting
  []
  (:email env))

(def ^:const notifications-topics
  "The `Set` of event topics which are subscribed to for use in notifications tracking."
  #{:request-materials})

(def ^:private notifications-channel
  "Channel for receiving event notifications we want to subscribe to for notifications events."
  (async/chan))

;;; ------------------------------------------------ Event Processing ------------------------------------------------
(defn get-template
  []
  (slurp "http://chinese-quartet.org/email-templates/request-materials.html"))

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

(defn- send-notification!
  [object & {:keys [quartet-email]
             :or {quartet-email "quartet@fudan.edu.cn"}}]
  (let [setting (email-setting)]
    (log/debug (format "%s %s %s" object setting quartet-email))
    (send-message setting {:from    (:user setting)
                           :to      (:requestor-email object)
                           :cc      quartet-email
                           :subject (format "[Request Materials] %s-%s" (:organization object) (:purpose object))
                           :body    [{:type "text/html"
                                      :content (format-email-content object)}]})))

(defn- process-notifications-event!
  "Handle processing for a single event notification received on the notifications-channel"
  [notification-event]
  ;; try/catch here to prevent individual topic processing exceptions from bubbling up.  better to handle them here.
  (try
    (when-let [{topic :topic object :item} notification-event]
      ;; TODO: only if the definition changed??
      (case topic
        :request-materials (send-notification! object)))
    (catch Throwable e
      (log/warn (format "Failed to process notifications event. %s" (:topic notification-event)) e))))

;;; --------------------------------------------------- Lifecycle ----------------------------------------------------

(defn events-init
  "Automatically called during startup; start event listener for notifications events."
  []
  (events/start-event-listener! notifications-topics notifications-channel process-notifications-event!))


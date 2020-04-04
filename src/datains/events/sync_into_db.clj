(ns datains.events.sync-into-db
  (:require [clojure.core.async :as async]
            [clojure.set :as set]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [datains.events :as events]
            [datains.db.handler :as db-handler]))

(def ^:const notifications-topics
  "The `Set` of event topics which are subscribed to for use in notifications tracking."
  #{:report/record-update})

(def ^:private notifications-channel
  "Channel for receiving event notifications we want to subscribe to for notifications events."
  (async/chan))

;;; ------------------------------------------------ Event Processing ------------------------------------------------

(defn- sync-into-report-table!
  "Create a report record or update it."
  [record]
  (when-let [{project-id :project-id
              record-id  :id} record]
    (if project-id
      (db-handler/update-report! record-id record)
      (db-handler/create-report! record))))

(defn- sync-into-project-table!
  "Create a project record or update it."
  [record]
  (when-let [{record-id  :id} record]
    (if record-id
      (db-handler/update-project! record-id record)
      (db-handler/create-project! record))))

(defn- process-sync-db-event!
  "Handle processing for a single event notification received on the notifications-channel"
  [notification-event]
  ;; try/catch here to prevent individual topic processing exceptions from bubbling up.  better to handle them here.
  (try
    (when-let [{topic  :topic
                object :item} notification-event]
      ;; TODO: only if the definition changed??
      (case (events/topic->model topic)
        "report/record"  (sync-into-report-table! object)
        "project/record" (sync-into-project-table! object)))
    (catch Throwable e
      (log/warn (format "Failed to process notifications event. %s" (:topic notification-event)) e))))

;;; --------------------------------------------------- Lifecycle ----------------------------------------------------

(defn events-init
  "Automatically called during startup; start event listener for notifications events."
  []
  (events/start-event-listener! notifications-topics notifications-channel process-sync-db-event!))

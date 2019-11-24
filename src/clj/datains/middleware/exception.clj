(ns datains.middleware.exception
  (:require [clojure.tools.logging :as log]
            [expound.alpha :as expound]
            [reitit.coercion :as coercion]
            [reitit.ring.middleware.exception :as exception])
  (:import [org.postgresql.util PSQLException]))

(defn coercion-error-handler [status]
  (let [printer (expound/custom-printer {:print-specs? false})]
    (fn [exception request]
      {:status status
       :headers {"Content-Type" "text/html"}
       :body (with-out-str (printer (-> exception ex-data :problems)))})))

(defn handler [message exception request]
  {:status 500
   :body {:message message
          :exception (.toString exception)
          :data (ex-data exception)
          :uri (:uri request)}})

(def exception-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {;; log stack-traces for all exceptions
     ::exception/wrap (fn [handler e request]
                        (log/error e (.getMessage e))
                        (handler e request))
     ;; SQL Exception
     ;; TODO: Can't catch PSQLException, Only support to catch Exception class.
     ; PSQLException (partial handler "postresql-exception")
     
     ;; All other exceptions
     Exception (partial handler "exception")

     ;; human-optimized validation messages
     ::coercion/request-coercion (coercion-error-handler 400)
     ::coercion/response-coercion (coercion-error-handler 500)})))

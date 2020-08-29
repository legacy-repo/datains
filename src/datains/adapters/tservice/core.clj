(ns datains.adapters.tservice.core
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]))

(def ^:private tservice
  (atom nil))

(defn setup-connection
  [base-url]
  (reset! tservice base-url))

(defn get-local-auth-header
  "TODO: Maybe need to support auth."
  []
  nil)

(defn request-json
  "Response to REQUEST with :body parsed as JSON."
  [request]
  (let [{:keys [body]
         :as   response} (http/request request)]
    (assoc response :body (json/read-str body :key-fn keyword))))

(defn submit-report
  "Post a request."
  [name body]
  (-> {:method          :post
       :content-type    :application/json
       :socket-timeout 1000
       :connection-timeout 1000
       :headers         (get-local-auth-header)
       :body            (json/write-str body)
       :url             (str @tservice "/api/" name)}
      (request-json)
      :body))

(defn sync-report
  "Sync report's status"
  [report-id]
  (-> {:method          :post
       :content-type    :application/json
       :socket-timeout 1000
       :connection-timeout 1000
       :headers         (get-local-auth-header)
       :url             (str @tservice "/api/status/" report-id)}
      (request-json)
      :body))
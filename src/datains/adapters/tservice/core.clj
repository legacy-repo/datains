(ns datains.adapters.tservice.core
  (:require [clojure.data.json :as json]
            [clj-http.client :as http]
            [datains.config :refer [env]]))

(def ^:private tservice
  (atom nil))

(defn setup-connection
  []
  (reset! tservice (:tservice env)))

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
  (-> {:method       :post
       :content-type :application/json
       :headers      (get-local-auth-header)
       :body         (json/write-str body)
       :url          (str @tservice "/api/" name)}
      (request-json)
      :body))

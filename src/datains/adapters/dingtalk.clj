(ns datains.adapters.dingtalk
  (:require [datains.config :refer [env]]
            [lambdaisland.uri :as uri-lib]
            [clj-http.client :as client]
            [clojure.tools.logging :as log]
            [clojure.data.json :as json])
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)
           (org.apache.commons.codec.binary Base64)
           (java.net URLEncoder)))

(defn access-token
  []
  (get-in env [:dingtalk-access-token]))

(defn secret
  []
  (get-in env [:dingtalk-secret]))

(defn timestamp
  []
  (System/currentTimeMillis))

(defn string-to-sign
  [timestamp]
  (str timestamp "\n" (secret)))

(defn secretKeyInst [secret mac]
  (SecretKeySpec. (.getBytes secret "UTF-8") (.getAlgorithm mac)))

(defn sign
  "Returns the signature of a string with a given
   secret, using a SHA-256 HMAC."
  [secret string]
  (let [mac       (Mac/getInstance "HmacSHA256")
        secretKey (secretKeyInst secret mac)]
    (-> (doto mac
          (.init secretKey)
          (.update (.getBytes string "UTF-8")))
        .doFinal)))

(defn encode
  [sign-data]
  (-> sign-data
      (Base64/encodeBase64)
      (String.)
      (URLEncoder/encode "UTF-8")))

(defn gen-signed-string
  [timestamp]
  (->> (string-to-sign timestamp)
       (sign (secret))
       (encode)))

(defn make-webhook-url
  ([] (let [timestamp (timestamp)]
        (make-webhook-url (access-token) timestamp (gen-signed-string timestamp))))
  ([query]
   (str (assoc (uri-lib/uri "https://oapi.dingtalk.com/robot/send")
               :query query)))
  ([access-token timestamp sign]
   (make-webhook-url (format "access_token=%s&timestamp=%s&sign=%s" access-token timestamp sign))))

(defn send-msg!
  [msg]
  (let [ret-msg (client/post (make-webhook-url)
                             {:body               msg
                              :content-type       :json
                              :socket-timeout     1000      ;; in milliseconds
                              :connection-timeout 1000      ;; in milliseconds
                              :accept             :json})]
    (log/info "Send message to dingtalk: " (:body ret-msg))))

(defn markdown-msg
  [title content]
  (json/write-str
   {:msgtype  "markdown"
    :markdown {:title title
               :text  content}}))

(defn send-markdown-msg!
  [title content]
  (send-msg! (markdown-msg title content)))

(defn text-msg
  [content]
  (json/write-str
   {:msgtype "text"
    :text    {:content content}}))

(defn send-text-msg!
  [content]
  (send-msg! (text-msg content)))

(defn link-msg
  [title content pic-url msg-url]
  (json/write-str {:msgtype "link"
                   :link    {:text       content
                             :title      title
                             :picUrl     pic-url
                             :messageUrl msg-url}}))

(defn send-link-msg!
  [title content pic-url msg-url]
  (send-msg! (link-msg title content pic-url msg-url)))

(defn action-card
  [title content single-title single-url]
  (json/write-str {:msgtype    "actionCard"
                   :actionCard {:title          title
                                :text           content
                                :hideAvatar     "0"
                                :btnOrientation "0"
                                :singleTitle    single-title
                                :singleURL      single-url}}))

(defn send-action-card!
  [title content single-title single-url]
  (send-msg! (action-card title content single-title single-url)))
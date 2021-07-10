(ns datains.api.message-spec
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]))

(s/def ::page
  (st/spec
   {:spec                nat-int?
    :type                :long
    :description         "Page, From 1."
    :swagger/default     1
    :reason              "The page parameter can't be none."}))

(s/def ::per_page
  (st/spec
   {:spec                nat-int?
    :type                :long
    :description         "Num of items per page."
    :swagger/default     10
    :reason              "The per-page parameter can't be none."}))

;; -------------------------------- Message Spec --------------------------------
(s/def ::message_type
  (st/spec
   {:spec                #(#{"request-materials", "request-data"} %)
    :type                :string
    :description         "Filter results by message field."
    :swagger/default     "default"
    :reason              "Not valid message type, only support request-materials, request-data"}))

(s/def ::title
  (st/spec
   {:spec                string?
    :type                :string
    :description         "Title of the record."
    :swagger/default     ""
    :reason              "Not a valid title."}))

(s/def ::description
  (st/spec
   {:spec                string?
    :type                :string
    :description         "Description of the record"
    :swagger/default     ""
    :reason              "Not a valid description."}))

(s/def ::payload
  (st/spec
   {:spec                map?
    :description         "The More details with message."
    :swagger/default     {}
    :reason              "Not a valid message payload."}))

(s/def ::created_time
  (st/spec
   {:spec                nat-int?
    :type                :integer
    :description         "The created-time of the project."
    :swagger/default     0
    :reason              "Not a valid created-time."}))

(s/def ::status
  (st/spec
   {:spec                #(#{"Unread" "Read"} %)
    :description         "Filter results by status field."
    :swagger/default     "Unread"
    :reason              "Not valid status, only support Unread, Read."}))

(def message-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per_page ::message_type ::status]))

(def message-body
  (s/keys  :req-un [::title ::message_type ::payload ::status]
           :opt-un [::created_time ::description]))

(def message-put-body
  (s/keys  :req-un [::status]
           :opt-un []))

(ns datains.api.log-spec
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

;; -------------------------------- Notification Spec --------------------------------
(s/def ::title
  (st/spec
   {:spec                string?
    :type                :string
    :description         "Title of the record."
    :swagger/default     "stderr"
    :reason              "Not a valid title."}))

(s/def ::content
  (st/spec
   {:spec            string?
    :type            :string
    :description     "Log content."
    :swagger/default "This is a log."
    :reason          "Not a valid content."}))

(s/def ::created_time
  (st/spec
   {:spec                nat-int?
    :type                :integer
    :description         "The created-time of the log."
    :swagger/default     0
    :reason              "Not a valid created-time."}))

(s/def ::entity_type
  (st/spec
   {:spec            #(#{"Workflow"} %)
    :description     "For filtering log records."
    :swagger/default "Link"
    :reason          "Not valid entity-type, only support Workflow."}))

(s/def ::entity_id
  (st/spec
   {:spec            #(re-find #"^[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}$" %)
    :type            :string
    :description     "uuid string"
    :swagger/default "40644dec-1abd-489f-a7a8-1011a86f40b0"
    :reason          "Not valid a entity-id."}))

(s/def ::log_type
  (st/spec
   {:spec                #(#{"Link" "Content"} %)
    :description         "Specify the type of the content field."
    :swagger/default     "Link"
    :reason              "Not valid log-type, only support Link, Content"}))

(def log-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per_page ::log_type ::entity_type ::entity_id]))

(def log-body
  (s/keys  :req-un [::title ::content ::log_type ::entity_id ::entity_type]
           :opt-un [::created_time]))

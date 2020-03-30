(ns datains.api.notification-spec
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]))

(s/def ::page
  (st/spec
   {:spec                nat-int?
    :type                :long
    :description         "Page, From 1."
    :swagger/default     1
    :reason              "The page parameter can't be none."}))

(s/def ::per-page
  (st/spec
   {:spec                nat-int?
    :type                :long
    :description         "Num of items per page."
    :swagger/default     10
    :reason              "The per-page parameter can't be none."}))

;; -------------------------------- Notification Spec --------------------------------
(s/def ::notification-type
  (st/spec
   {:spec                #(#{"default"} %)
    :description         "Filter results by notification field."
    :swagger/default     "default"
    :reason              "Not valid notification, only support default"}))

(s/def ::title
  (st/spec
   {:spec                string?
    :type                :string
    :description         "Title of the record."
    :swagger/default     "huangyechao/annovar"
    :reason              "Not a valid title."}))

(s/def ::description
  (st/spec
   {:spec                string?
    :type                :string
    :description         "Description of the record"
    :swagger/default     ""
    :reason              "Not a valid description."}))

(s/def ::created-time
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

(def notification-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per-page ::notification-type ::status]))

(def notification-body
  (s/keys  :req-un [::title ::notification-type ::status]
           :opt-un [::created-time ::description]))

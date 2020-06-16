(ns datains.api.app-spec
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

(s/def ::uuid
  (st/spec
   {:spec                #(some? (re-matches #"[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}" %))
    :type                :string
    :description         "uuid string"
    :swagger/default     "40644dec-1abd-489f-a7a8-1011a86f40b0"
    :reason              "Not valid a uuid."}))

;; -------------------------------- App Spec --------------------------------
(s/def ::author
  (st/spec
   {:spec                string?
    :type                :string
    :description         "Author name that you want to query."
    :swagger/default     "huangyechao"
    :reason              "Not a valid author name."}))

(s/def ::valid
  (st/spec
   {:spec                boolean?
    :type                :string
    :description         "Filter results by valid field."
    :swagger/default     false
    :reason              "Valid field must be false or true."}))

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

(s/def ::repo_url
  (st/spec
   {:spec                #(some? (re-matches #"[a-zA-z]+://[^\s]*" %))
    :type                :string
    :description         "The url of the repo."
    :swagger/default     "http://choppy.3steps.cn/huangyechao/annovar"
    :reason              "Not a valid repo url."}))

(s/def ::cover
  (st/spec
   {:spec                #(some? (re-matches #"[a-zA-z]+://[^\s]*" %))
    :type                :string
    :description         "The url of the cover."
    :swagger/default     "https://clojure.org/images/clojure-logo-120b.png"
    :reason              "Not a valid cover url."}))

(s/def ::icon
  (st/spec
   {:spec                #(some? (re-matches #"[a-zA-z]+://[^\s]*" %))
    :type                :string
    :description         "The url of the icon."
    :swagger/default     "https://clojure.org/images/clojure-logo-120b.png"
    :reason              "Not a valid icon url."}))

(s/def ::rate
  (st/spec
   {:spec                nat-int?
    :type                :integer
    :description         "Rate of the repo."
    :swagger/default     0
    :reason              "Not a valid rate."}))

(def app-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per_page ::author ::valid ::title]))

(def app-body
  {:id          ::uuid
   :title       ::title
   :description ::description
   :repo_url    ::repo_url
   :cover       ::cover
   :icon        ::icon
   :author      ::author
   :rate        ::rate
   :valid       ::valid})

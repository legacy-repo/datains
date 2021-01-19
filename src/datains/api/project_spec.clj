(ns datains.api.project-spec
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
   {:spec                #(re-find #"^[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}$" %)
    :type                :string
    :description         "uuid string"
    :swagger/default     "40644dec-1abd-489f-a7a8-1011a86f40b0"
    :reason              "Not valid a uuid."}))

;; -------------------------------- Project Spec --------------------------------
(s/def ::project_name
  (st/spec
   {:spec                #(re-find #"^[a-zA-Z_][a-zA-Z0-9_]{4,63}$" %)  ; 不超过 64 个字符
    :type                :string
    :description         "The name of the project."
    :swagger/default     "annovar_test"
    :reason              "Not a valid project_name, regex: '^[a-zA-Z_][a-zA-Z0-9_]{4,63}$'."}))

(s/def ::description
  (st/spec
   {:spec                string?
    :type                :string
    :description         "Description of the record"
    :swagger/default     ""
    :reason              "Not a valid description."}))

(s/def ::author
  (st/spec
   {:spec                #(re-find #"^[a-zA-Z_][a-zA-Z0-9_]{4,31}$" %)
    :type                :string
    :description         "Author name that you want to query."
    :swagger/default     "huangyechao"
    :reason              "Not a valid author name, regex: '^[a-zA-Z_][a-zA-Z0-9_]{4,31}$'."}))

(s/def ::status
  (st/spec
   {:spec                #(#{"Submitted" "Running" "Failed" "Aborting" "Aborted" "Succeeded" "On Hold"} %)
    :type                :string
    :description         "The status of the project."
    :swagger/default     "Submitted"
    :reason              "Only support the one of Submitted, Running, Failed, Aborting, Aborted, Succeeded, On Hold"}))

(s/def ::percentage
  (st/spec
   {:spec            (s/and nat-int? #(< % 100))
    :type            :long
    :description     "Percentage."
    :swagger/default 0
    :reason          "The percentage parameter can't be negative integer."}))

(s/def ::app_id
  (st/spec
   {:spec                #(re-find #"^[a-z0-9]{32}$" %)  ; md5sum Regex
    :type                :string    
    :description         "The app-id of the related project."
    :swagger/default     "ec5b89dc49c433a9521a13928c032129"
    :reason              "Not valid app-id"}))

(s/def ::app_name
  (st/spec
   {:spec                #(re-find #"^[a-zA-Z_][a-zA-Z0-9/_-.]{4,255}$" %)
    :type                :string
    :description         "The name of the app."
    :swagger/default     "huangyechao/annovar"
    :reason              "Not a valid app-name, regex: '^[a-zA-Z_][a-zA-Z0-9/_.]{4,255}$'"}))

(s/def ::group_name
  (st/spec
   {:spec                #(re-find #"^[a-z0-9A-Z _-]{5,32}$" %)
    :type                :string
    :description         "The name of the group."
    :swagger/default     "Choppy Team"
    :reason              "Not a valid group-name, regex: '^[a-z0-9A-Z _-]{5,32}$'."}))

(s/def ::started_time
  (st/spec
   {:spec                nat-int?
    :type                :integer
    :description         "The started-time of the project."
    :swagger/default     0
    :reason              "Not a valid started-time."}))

(s/def ::finished_time
  (st/spec
   {:spec                nat-int?
    :type                :integer
    :description         "The finished-time of the project."
    :swagger/default     0
    :reason              "Not a valid finished-time."}))

(s/def ::samples
  (st/spec
   {:spec                vector?
    :type                :vector
    :description         "The samples for the project."
    :swagger/default     []
    :reason              "Not a valid samples."}))

(def uuid-spec
  (s/keys :req-un [::uuid]
          :opt-un []))

(def project-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per_page ::author ::status ::app_id ::project_name]))

(def project-body
  (s/keys :req-un [::project_name ::app_id ::app_name ::author ::samples]
          :opt-un [::description ::group_name ::started_time ::finished_time ::status]))

(def project-put-body
  (s/keys :req-un []
          :opt-un [::finished_time ::status ::percentage]))

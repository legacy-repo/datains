(ns datains.api.report-spec
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

(s/def ::id
  (st/spec
   {:spec                #(some? (re-matches #"[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}" %))
    :type                :string
    :description         "report-id"
    :swagger/default     "40644dec-1abd-489f-a7a8-1011a86f40b0"
    :reason              "Not valid a report-id."}))

;; -------------------------------- Report Spec --------------------------------
(s/def ::description
  (st/spec
   {:spec                string?
    :type                :string
    :description         "Description of the record"
    :swagger/default     ""
    :reason              "Not a valid description."}))

(s/def ::project-id
  (st/spec
   {:spec                #(some? (re-matches #"[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}" %))
    :type                :string
    :description         "project-id"
    :swagger/default     "40644dec-1abd-489f-a7a8-1011a86f40b0"
    :reason              "Not valid a project-id."}))

(s/def ::report-type
  (st/spec
   {:spec                #(#{"multiqc"} %)
    :type                :string
    :description         "Filter results by report-type field."
    :swagger/default     "multiqc"
    :reason              "Not valid report-type, only support multiqc."}))

(s/def ::status
  (st/spec
   {:spec                #(#{"Started" "Finished" "Checked" "Archived"} %)
    :type                :string
    :description         "Filter results by status field."
    :swagger/default     "Started"
    :reason              "Not valid status, only support Started, Finished, Checked, Archived."}))

(def report-id
  (s/keys :req-un [::id]
          :opt-un []))

(def report-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per-page ::project-id ::report-type ::status]))

(def report-body
  {:report-name   string?
   :project-id    ::project-id
   :script        string?
   :description   ::description
   :started-time  nat-int?
   :finished-time nat-int?
   :checked-time  nat-int?
   :archived-time nat-int?
   :report-path   string?
   :report-type   ::report-type
   :status        ::status
   :log           string?})

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

(s/def ::per_page
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

(s/def ::project_id
  (st/spec
   {:spec                #(some? (re-matches #"[a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8}" %))
    :type                :string
    :description         "project-id"
    :swagger/default     "40644dec-1abd-489f-a7a8-1011a86f40b0"
    :reason              "Not valid a project-id."}))

(s/def ::report_type
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
          :opt-un [::page ::per_page ::project_id ::report_type ::status]))

(def report-body
  {:report_name   string?
   :project_id    ::project_id
   :script        string?
   :description   ::description
   :started_time  nat-int?
   :finished_time nat-int?
   :checked_time  nat-int?
   :archived_time nat-int?
   :report_path   string?
   :report_type   ::report_type
   :status        ::status
   :log           string?})

(ns datains.api.spec
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]
            [datains.util :as util]))

(s/def ::page
  (st/spec
   {:spec nat-int?
    :description "Page, From 1."
    :swagger/default 1
    :reason "The page parameter can't be none."}))

(s/def ::per-page
  (st/spec
   {:spec nat-int?
    :description "Num of items per page."
    :swagger/default 10
    :reason "The per-page parameter can't be none."}))

;; -------------------------------- App Spec --------------------------------
(s/def ::author
  (st/spec
   {:spec string?
    :description "Author name that you want to query."
    :swagger/default ""
    :reason "Not a valid author."}))

(s/def ::valid
  (st/spec
   {:spec            boolean?
    :description     "Filter results by valid field."
    :swagger/default nil
    :reason          "Valid field must be false or true."}))

(s/def ::title
  (st/spec
   {:spec            string?
    :description     "Filter results by title field."
    :swagger/default nil
    :reason          "Not a valid title."}))

(def app-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per-page ::author ::valid ::title]))

(def app-body
  {:id          string?
   :title       string?
   :description string?
   :repo-url    string?
   :cover       string?
   :icon        string?
   :author      string?
   :rate        string?
   :valid       boolean?})

;; -------------------------------- Tag Spec --------------------------------
(def tag-body
  {:name string?
   :category string?})

;; -------------------------------- Project Spec --------------------------------
(s/def ::status
  (st/spec
   {:spec            #(#{"Submitted" "Running" "Failed" "Aborting" "Aborted" "Succeeded" "On Hold"} %)
    :description     "Filter results by title field."
    :swagger/default nil
    :reason          "Only support the one of Submitted, Running, Failed, Aborting, Aborted, Succeeded, On Hold"}))

(s/def ::app-id
  (st/spec
   {:spec            #(re-find #"^[a-z0-9]{32}$" %)  ; md5sum Regex
    :description     "Filter results by app-id field."
    :swagger/default nil
    :reason          "Not valid app-id"}))

(def project-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per-page ::author ::status ::app-id]))

(def project-body
  {:project-name  string?
   :description   string?
   :app-id        string?
   :app-name      string?
   :author        string?
   :group-name    string?
   :started-time  nat-int?
   :finished-time nat-int?
   :status        string?})

;; -------------------------------- Workflow Spec --------------------------------
(s/def ::project-id
  (st/spec
   {:spec            util/uuid?
    :description     "Filter results by project-id field."
    :swagger/default nil
    :reason          "Not valid project-id"}))

(def workflow-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per-page ::project-id ::status]))

(def workflow-body
  {:project-id     util/uuid?
   :sample-id      string?
   :submitted-time nat-int?
   :started-time   nat-int?
   :finished-time  nat-int?
   :job-params     string?
   :lables         string?
   :status         string?})

;; -------------------------------- Report Spec --------------------------------
(s/def ::project-id
  (st/spec
   {:spec            util/uuid?
    :description     "Filter results by project-id field."
    :swagger/default nil
    :reason          "Not valid project-id"}))

(s/def ::report-type
  (st/spec
   {:spec            #(#{"multiqc"} %)
    :description     "Filter results by report-type field."
    :swagger/default nil
    :reason          "Not valid report-type, only support multiqc."}))

(s/def ::rstatus
  (st/spec
   {:spec            #(#{"Started" "Finished" "Checked" "Archived"} %)
    :description     "Filter results by rstatus field."
    :swagger/default nil
    :reason          "Not valid rstatus, only support Started, Finished, Checked, Archived."}))

(def report-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per-page ::project-id ::report-type ::rstatus]))

(def report-body
  {:report-name   string?
   :project-id    util/uuid?
   :script        string?
   :description   string?
   :started-time  nat-int?
   :finished-time nat-int?
   :checked-time  nat-int?
   :archived-time nat-int?
   :report-path   string?
   :report-type   string?
   :status        string?
   :log           string?})

;; -------------------------------- Notification Spec --------------------------------
(s/def ::notification-type
  (st/spec
   {:spec            #(#{"default"} %)
    :description     "Filter results by notification field."
    :swagger/default nil
    :reason          "Not valid notification, only support default"}))

(s/def ::nstatus
  (st/spec
   {:spec            #(#{"Unread" "Read"} %)
    :description     "Filter results by nstatus field."
    :swagger/default nil
    :reason          "Not valid nstatus, only support Unread, Read."}))

(def notification-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per-page ::notification-type ::nstatus]))

(def notification-body
  {:title             string?
   :description       string?
   :notification-type string?
   :created-time      nat-int?
   :status            string?})
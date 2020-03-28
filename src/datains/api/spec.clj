(ns datains.api.spec
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]))

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
  {:id            string?
   :project-name  string?
   :description   string?
   :app-id        string?
   :app-name      string?
   :author        string?
   :group-name    string?
   :started-time  nat-int?
   :finished-time nat-int?
   :status        string?})
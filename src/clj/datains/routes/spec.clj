(ns datains.routes.spec
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

(s/def ::query-str
  (st/spec
   {:spec string?
    :description "Query string"
    :swagger/default ""
    :reason ""}))

(def params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per-page ::query-str]))

(def app-body
  {:id string?
   :title string?
   :description string?
   :repo-url string?
   :cover string?
   :icon string?
   :author string?
   :rate string?})

(def tag-body
  {:name string?
   :category string?})

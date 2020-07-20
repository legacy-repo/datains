(ns datains.api.fs-spec
  (:require [clojure.spec.alpha :as s]
            [spec-tools.core :as st]))

(s/def ::page
  (st/spec
   {:spec            nat-int?
    :type            :long
    :description     "Page, From 1."
    :swagger/default 1
    :reason          "The page parameter can't be none."}))

(s/def ::per_page
  (st/spec
   {:spec            nat-int?
    :type            :long
    :description     "Num of items per page."
    :swagger/default 10
    :reason          "The per-page parameter can't be none."}))

;; -------------------------------- FS Spec --------------------------------
(s/def ::name
  (st/spec
   {:spec            (s/and string? #(re-find #"^[a-zA-Z\-][a-zA-Z0-9\-]{2,63}$" %))  ; 不超过 64 个字符
    :type            :string
    :description     "The name of the bucket."
    :swagger/default "test"
    :reason          "Not a valid bucket name, regex: '^[a-zA-Z-][a-zA-Z0-9-]{2,63}$'."}))

(s/def ::prefix
  (st/spec
   {:spec            (s/and string? #(re-find #"^[a-zA-Z_\/][a-zA-Z0-9_\/]{0,63}$" %))  ; 不超过 64 个字符
    :type            :string
    :description     "The prefix of the object."
    :swagger/default "test"
    :reason          "Not a valid object prefix, regex: '^[a-zA-Z_][a-zA-Z0-9_/]{0,63}$'."}))

(def bucket-params-query
  "A spec for the query parameters."
  (s/keys :req-un []
          :opt-un [::page ::per_page ::prefix]))

(def bucket-name-spec
  (s/keys :req-un [::name]
          :opt-un []))

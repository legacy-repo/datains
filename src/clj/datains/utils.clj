(ns datains.utils
  (:require
   [clojure.string :as cstr]))

(defn get-offset [page per-page]
  (* (- page 1) per-page))

(defn trim [query-str]
  (cstr/trim (cstr/replace query-str #"[\"]" "")))
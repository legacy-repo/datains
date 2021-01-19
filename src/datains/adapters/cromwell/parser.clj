(ns datains.adapters.cromwell.parser
  (:require [clojure.java.io :as io]
            [clj-antlr.core :as antlr]))


(def grammar-definition-v2
  {:parser (.getFile (io/resource "v2/WdlV2Parser.g4"))
   :lexer (.getFile (io/resource "v2/WdlV2Lexer.g4"))})

(def grammar-definition-v1
  {:parser (.getFile (io/resource "v1/WdlV1Parser.g4"))
   :lexer (.getFile (io/resource "v1/WdlV1Lexer.g4"))})

(defn get-parser
  [version]
  (if (= version :v1)
    (antlr/parser (:lexer grammar-definition-v1) (:parser grammar-definition-v1) {})
    (antlr/parser (:lexer grammar-definition-v2) (:parser grammar-definition-v2) {})))

(defn parse-wdl
  [wdl-file version]
  (let [parser (get-parser version)]
    (parser wdl-file)))

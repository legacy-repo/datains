(ns datains.adapters.cromwell.util
  "Common utility functions useful throughout the codebase."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def the-name
  "Use this name to refer to this program."
  "datains")

(defn get-the-version
  "Return version information from the JAR."
  []
  (or (some-> (str/join "/" ["datains" "version.edn"])
              io/resource
              slurp
              str/trim
              edn/read-string)
      {:version "SOME BOGUS VERSION"}))

(defn keys-in
  "Return all keys used in any maps in TREE."
  [tree]
  (letfn [(follow? [node]
            (or (map? node) (vector? node) (set? node) (list? node)))]
    (reduce into #{}
            (remove nil?
                    (map (fn [node] (when (map? node) (keys node)))
                         (tree-seq follow? identity tree))))))

(defn sleep-seconds
  "Sleep for N seconds."
  [n]
  (Thread/sleep (* n 1000)))

(defn lazy-unchunk
  "Supply items from COLL one at a time to work around chunking of lazy
  sequences for HTTP pagination callbacks and so on."
  [coll]
  (lazy-seq
   (when (seq coll)
     (cons (first coll)
           (lazy-unchunk (rest coll))))))
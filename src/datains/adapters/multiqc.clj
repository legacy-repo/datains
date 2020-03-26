(ns datains.adapters.multiqc
  (:require [me.raynes.fs :as fs])
  (:use [clojure.java.shell :only [sh]]))

(defn ok?
  "True if multiqc is installed, otherwise return false."
  []
  (= 0 (:exit (sh "which" "multiqc"))))

(def ^:private report-dir (atom "~/.choppy/report"))

(defn setup-report-dir
  [new-report-dir]
  (reset! report-dir new-report-dir))

(defn get-report-dir
  []
  (fs/expand-home @report-dir))
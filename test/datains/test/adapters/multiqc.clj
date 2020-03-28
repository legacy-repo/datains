(ns datains.test.adapters.multiqc
  (:require [clojure.test :refer :all]
            [datains.config :refer [env]]
            [mount.core :as mount]
            [datains.adapters.multiqc :as multiqc]
            [datains.util :as util]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'datains.config/env)
    (multiqc/setup-report-dir (util/join-path (env :datains-workdir) "/report"))
    (f)))

(deftest test-apps
  (testing "Test multiqc module."
    (is (= (multiqc/get-report-dir) "/Users/choppy/Downloads/datains/report"))
    (is (= (multiqc/multiqc "/analysis-dir" "/outdir" true {}) 
           "multiqc --force --title 'iSEQ Analyzer Report' --comment '' --filename multiqc_report.html --outdir /outdir /analysis-dir"))))

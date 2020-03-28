(ns datains.test.adapters.app_store
  (:require [clojure.test :refer :all]
            [datains.config :refer [env]]
            [mount.core :as mount]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [datains.setup-adapters :as setup]
            [datains.adapters.app-store.core :as app-store]
            [datains.util :as util]))

(defn clean-dir
  [path]
  (try
    (util/delete-recursively path)
    (catch Exception e
      (println "Not such file: " path e))))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'datains.config/env)
    (setup/setup-app-store)
    (clean-dir "/tmp/bedtools")
    (f)))

(defn exists?
  [path]
  (.exists (io/file path)))

(deftest test-apps
  (testing "Test app-store module."
    (is (str/starts-with? (app-store/get-app-workdir) (get-in env [:datains-workdir])))
    (is (= (app-store/join-url "https" "baidu.com" 8080 "/foo" "key=value") "https://baidu.com:8080/foo?key=value"))
    (is (= (app-store/make-url "/path" "key=value") "http://choppy.3steps.cn:80/path?key=value"))
    (is (app-store/exist-file? "/junshang/annotation-test/src/branch/master/workflow.wdl"))
    (is (= false (app-store/app-is-valid? "junshang/bedtools" "")))
    (is (= true (app-store/service-is-ok?)))
    (is (> (count (app-store/get-all-apps "choppy-app")) 0))
    (is (= "/tmp/bedtools"
           (app-store/get-repo-path
            (app-store/clone!
             "http://choppy.3steps.cn/junshang/bedtools.git"
             "/tmp/bedtools"))))))

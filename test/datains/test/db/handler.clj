(ns datains.test.db.handler
  (:require
   [datains.db.handler :as db-handler]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [datains.config :refer [env]]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'datains.config/env
     #'datains.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-handler
  (is (= [:id "1"]
         (db-handler/create-app!
          {:id          "1"
           :icon        "Sam"
           :cover       "Smith"
           :title       "exceRptSmallRNA"
           :description "exceRptSmallRNA"
           :repo-url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
           :author      "chenziyin"
           :rate        "5"})))
  (is (= {:total 1
          :page 1
          :per-page 10
          :data [{:id          "1"
                  :icon        "Sam"
                  :cover       "Smith"
                  :title       "exceRptSmallRNA"
                  :description "exceRptSmallRNA"
                  :repo_url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
                  :author      "chenziyin"
                  :rate        "5"}]}
         (db-handler/search-apps))))

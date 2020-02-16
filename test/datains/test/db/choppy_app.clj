(ns datains.test.db.choppy_app
  (:require
   [datains.db.core :refer [*db*] :as db]
   [java-time.pre-java8]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [clojure.java.jdbc :as jdbc]
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

(deftest test-apps
  (jdbc/with-db-transaction [t-conn *db*]
    (jdbc/db-set-rollback-only! t-conn)
    ; db/create-app!
    (is (= [:id "1"] 
           (db/create-app!
            t-conn
            {:id          "1"
             :icon        "Sam"
             :cover       "Smith"
             :title       "exceRptSmallRNA"
             :description "exceRptSmallRNA"
             :repo_url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
             :author      "chenziyin"
             :rate        "5"})))
    ; db/search-apps
    (is (= {:id          "1"
            :icon        "Sam"
            :cover       "Smith"
            :title       "exceRptSmallRNA"
            :description "exceRptSmallRNA"
            :repo_url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
            :author      "chenziyin"
            :rate        "5"}
           (first (db/search-apps t-conn {:query-map {:id "1"} :limit 1 :offset 0}))))
    ; db/get-app-count
    (is (= {:count 1} (db/get-app-count t-conn {:query-map {:id "1"}})))
    ; not exist 
    (is (= {:count 0} (db/get-app-count t-conn {:query-map {:id "2"}})))
    ; db/update-app!
    (is (= 1 (db/update-app! t-conn {:updates {:rate "4"} :id "1"})))
    ; db/delete-app!
    (is (= 1 (db/delete-app! t-conn {:id "1"})))
    ; not exist
    (is (= {:count 0} (db/get-app-count t-conn)))))

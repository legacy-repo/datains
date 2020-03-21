(ns datains.test.db.core
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
    (is (= {:id "1"}
           (db/create-app!
            t-conn
            {:id          "1"
             :icon        "Sam"
             :cover       "Smith"
             :title       "exceRptSmallRNA"
             :description "exceRptSmallRNA"
             :repo-url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
             :author      "chenziyin"
             :rate        "5"
             :valid       true})))
    ; db/search-apps
    (is (= {:id          "1"
            :icon        "Sam"
            :cover       "Smith"
            :title       "exceRptSmallRNA"
            :description "exceRptSmallRNA"
            :repo_url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
            :author      "chenziyin"
            :rate        "5"
            :valid       true}
           (first (db/search-apps t-conn {:query-map {:id "1"}
                                          :limit     1
                                          :offset    0}))))
    ; db/search-apps-with-tags
    (is (= nil
           (first (db/search-apps-with-tags
                   t-conn
                   {:query-map {:id "1"}
                    :limit     1
                    :offset    0}))))
    ; db/get-app-count
    (is (= {:count 1} (db/get-app-count t-conn {:query-map {:id "1"}})))
    ; not exist 
    (is (= {:count 0} (db/get-app-count t-conn {:query-map {:id "2"}})))
    ; db/update-app!
    (is (= 1 (db/update-app! t-conn {:updates {:rate "4"}
                                     :id      "1"})))
    ; db/delete-app!
    (is (= 1 (db/delete-app! t-conn {:id "1"})))
    ; not exist
    (is (= {:count 0} (db/get-app-count t-conn)))
    ; db/create-tag!
    (is (= true
           (some? (first (db/create-tag!
                          t-conn
                          {:title "test"})))))
    ; db/search-tags
    (is (= {:title "test"}
           (select-keys
            (first
             (db/search-tags
              t-conn
              {:query-map {:title "test"}
               :limit     1
               :offset    0}))
            [:title])))
    ; db/get-tag-count
    (is (= {:count 1}
           (db/get-tag-count
            t-conn
            {:query-map {:title "test"}})))
    ; not exist 
    (is (= {:count 0}
           (db/get-tag-count
            t-conn
            {:query-map {:title "no valid"}})))
    ; db/delete-tag!
    (is (= 1
           (db/delete-tag!
            t-conn
            {:title "test"})))
    ; not exist
    (is (= {:count 0}
           (db/get-tag-count t-conn)))
    (is (= 1
           (count
            (do
              (db/create-app!
               t-conn
               {:id          "1"
                :icon        "Sam"
                :cover       "Smith"
                :title       "exceRptSmallRNA"
                :description "exceRptSmallRNA"
                :repo-url    "http://choppy.3steps.cn/chenziyin/exceRptSmallRNA"
                :author      "chenziyin"
                :rate        "5"
                :valid       true})
              (db/create-tag! t-conn {:title "test"})
              (db/connect-app-tag! t-conn {:tag-title     "test"
                                           :choppy-app-id "1"})
              (db/search-apps-with-tags t-conn {:query-map {:id "1"}
                                                :limit     1
                                                :offset    0})))))))

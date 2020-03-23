(ns datains.test.adapters.dingtalk
  (:require [clojure.test :refer :all]
            [datains.config :refer [env]]
            [mount.core :as mount]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [datains.adapters.dingtalk :as dingtalk]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'datains.config/env)
    (dingtalk/setup-access-token (env :dingtalk-access-token))
    (dingtalk/setup-secret (env :dingtalk-secret))
    (f)))

(deftest test-apps
  (testing "Test dingtalk module."
  (is (= "{\"errcode\":0,\"errmsg\":\"ok\"}" (:body (dingtalk/send-text-msg! "test"))))))

(ns datains.config
  (:require
   [cprop.core :refer [load-config]]
   [cprop.source :as source]
   [mount.core :refer [args defstate]]))

(def ^:private app-defaults
  "Global application defaults"
  {:datains-run-mode            "prod"})

(defstate env
  :start
  (load-config
   :merge
   [app-defaults                  ; Priority Lowest
    (args)
    (source/from-system-props)
    (source/from-env)]))          ; Priority Highest

(def ^Boolean is-dev?  "Are we running in `dev` mode (i.e. in a REPL or via `lein ring server`)?" (= :dev  (:datains-run-mode env)))
(def ^Boolean is-prod? "Are we running in `prod` mode (i.e. from a JAR)?"                         (= :prod (:datains-run-mode env)))
(def ^Boolean is-test? "Are we running in `test` mode (i.e. via `lein test`)?"                    (= :test (:datains-run-mode env)))

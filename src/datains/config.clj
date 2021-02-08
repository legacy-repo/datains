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
   :merge [app-defaults                  ; Priority Lowest
           (source/from-system-props)
           (args)
           (source/from-env)]))          ; Priority Highest

(def ^Boolean is-dev?  "Are we running in `dev` mode (i.e. in a REPL or via `lein ring server`)?" (= :dev  (:datains-run-mode env)))
(def ^Boolean is-prod? "Are we running in `prod` mode (i.e. from a JAR)?"                         (= :prod (:datains-run-mode env)))
(def ^Boolean is-test? "Are we running in `test` mode (i.e. via `lein test`)?"                    (= :test (:datains-run-mode env)))

(defn get-fs-rootdir
  "Based on fs-services and default-fs-service configuration."
  []
  (:fs-rootdir
   (first
    (filter #(= (:fs-service %)
                (:default-fs-service env))
            (:fs-services env)))))

(defn default-fs-service
  "Get default fs service."
  []
  (:default-fs-service env))

(defn in-coll?
  [key coll]
  (>= (.indexOf coll key) 0))

(defn get-whitelist
  [service]
  (let [whitelist ((keyword service) (:whitelist env))]
    (if (nil? whitelist)
      []
      whitelist)))

(defn get-blacklist
  [service]
  (let [blacklist ((keyword service) (:blacklist env))]
    (if (nil? blacklist)
      []
      blacklist)))

(defn filter-buckets
  [coll filter-list mode]
  (if (= mode "white")
    (filter (fn [item] (in-coll? (get item "Name") filter-list)) coll)
    (filter (fn [item] (not (in-coll? (get item "Name") filter-list))) coll)))

(defn filter-by-whitelist
  "Save all items in whitelist."
  [coll service]
  (filter-buckets coll (get-whitelist service) "white"))

(defn filter-by-blacklist
  "Remove all items in blacklist."
  [coll service]
  (filter-buckets coll (get-blacklist service) "black"))

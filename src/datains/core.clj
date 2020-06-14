(ns datains.core
  (:require
   [datains.handler :as handler]
   [datains.nrepl :as nrepl]
   [datains.task :as task]
   [datains.events :as events]
   [luminus.http-server :as http]
   [luminus-migrations.core :as migrations]
   [datains.config :refer [env]]
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.tools.logging :as log]
   [mount.core :as mount]
   [datains.setup-adapters :as adapters])
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate dingtalk
  :start
  (adapters/setup-dingtalk)
  :stop
  (adapters/reset-dingtalk))

(mount/defstate multiqc
  :start
  (adapters/setup-multiqc)
  :stop
  (adapters/reset-multiqc))

(mount/defstate appstore
  :start
  (adapters/setup-app-store)
  :stop
  (adapters/reset-app-store))

(mount/defstate event
  :start
  (events/initialize-events!)
  :stop
  (events/stop-events!))

(mount/defstate scheduler
  :start
  (task/start-scheduler!)
  :stop
  (task/stop-scheduler!))

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
   (-> env
       (assoc  :handler (handler/app))
       (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime)))))
       (update :port #(or (-> env :options :port) %))))
  :stop
  (http/stop http-server))

(mount/defstate ^{:on-reload :noop} repl-server
  :start
  (when (env :nrepl-port)
    (nrepl/start {:bind (env :nrepl-bind)
                  :port (env :nrepl-port)}))
  :stop
  (when repl-server
    (nrepl/stop repl-server)))

(defn init-jndi []
  (System/setProperty "java.naming.factory.initial"
                      "org.apache.naming.java.javaURLContextFactory")
  (System/setProperty "java.naming.factory.url.pkgs"
                      "org.apache.naming"))

(defn start-app [args]
  ; Start datains application
  ; Order: ["#'datains.config/env" "#'datains.db.core/*db*" "#'datains.handler/init-app" "#'datains.handler/app-routes" "#'datains.core/http-server" "#'datains.core/repl-server"]
  ; Loading Order: core -> handler -> [routes -> api] -> db -> config
  ; https://github.com/tolitius/mount#start-and-stop-order
  (init-jndi)
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  ; Run handler/destroy when the app is shutdown
  (.addShutdownHook (Runtime/getRuntime) (Thread. handler/destroy)))

(defn -main [& args]
  "Launch Datains in standalone mode."
  (log/info "Starting Datains in STANDALONE mode")
  ; Load configuration from system-props & env
  (mount/start #'datains.config/env)
  (cond
    ; When the DATABASE_URL variable has been set as "", an exception will be raised.
    ; #error: URI connection string cannot be empty!
    (or (nil? (:database-url env)) (= "" (:database-url env)))
    (do
      (log/error "Database configuration not found, :database-url environment variable must be set before running")
      (System/exit 1))
    ; Run a command like `java -jar datains.jar init-*`
    (some #{"init"} args)
    (do
      ; Initializes the database using the script specified by the :init-script key opts
      ; https://github.com/luminus-framework/luminus-migrations/blob/23a3061b5baaeb6a0ee44eb2f3df3a7fcaacf2da/src/luminus_migrations/core.clj#L55
      (when (or (nil? (:init-script env)) (= "" (:init-script env)))
        (log/error "Init script file not found, :init-script evironment variable must be set before running")
        (System/exit 1))
      (migrations/init (select-keys env [:database-url :init-script]))
      (System/exit 0))
    ; Run a command like `java -jar datains.jar migrate release-locks` or `lein run migrate release-locks`
    (migrations/migration? args)
    (do
      (migrations/migrate args (select-keys env [:database-url]))
      (System/exit 0))
    :else
    (start-app args)))   ; with no command line args just start Datains normally

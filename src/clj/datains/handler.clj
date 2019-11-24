(ns datains.handler
  (:require
   [datains.middleware :as middleware]
   [datains.routes.services :refer [service-routes]]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring :as ring]
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.webjars :refer [wrap-webjars]]
   [datains.env :refer [defaults]]
   [mount.core :as mount]
   [clojure.tools.logging :as log]
   [datains.config :refer [env]]
   [reitit.spec :as rs]
   [reitit.dev.pretty :as pretty]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(defn init
  "init will be called once when
   app is deployed as a servlet on
   an app server such as Tomcat
   put any initialization code here"
  []
  (doseq [component (:started (mount/start))]
    (log/info component "started")))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents)
  (log/info "datains has shut down!"))

(mount/defstate app-routes
  :start
  (ring/ring-handler
    (ring/router
     [["/" {:get
            {:handler (constantly {:status 301 :headers {"Location" "/api/api-docs/index.html"}})}}]
      (service-routes)]
     {:validate rs/validate
      :exception pretty/exception})
    (ring/routes
      (ring/create-resource-handler
        {:path "/"})
      (wrap-content-type (wrap-webjars (constantly nil)))
      (ring/create-default-handler))))

(defn app []
  (middleware/wrap-base #'app-routes))

(ns datains.middleware
  (:require
   [datains.env :refer [defaults]]
   [datains.config :refer [env]]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [clojure.tools.logging :as log]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.x-headers :refer [wrap-frame-options]]))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)
           (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))
      (wrap-cors :access-control-allow-origin [#"http://localhost:3001"]
                 :access-control-allow-methods [:get :put :post :delete :options])
      (wrap-frame-options {:allow-from "*"})))

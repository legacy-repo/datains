(ns datains.middleware
  (:require
   [datains.env :refer [defaults]]
   [datains.config :refer [env]]
   [ring-ttl-session.core :refer [ttl-memory-store]]
   [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.middleware.x-headers :refer [wrap-frame-options]]))

(defn enable-wrap-cors
  [handler]
  (let [origins (:cors-origins env)
        enabled-cors (:enable-cors env)]
    (if enabled-cors
      (wrap-cors handler
                 :access-control-allow-origin (if (some? origins) (map #(re-pattern %) origins) [#".*"])
                 :access-control-allow-methods [:get :put :post :delete :options])
      handler)))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)
           (assoc-in  [:session :store] (ttl-memory-store (* 60 30)))))
      (enable-wrap-cors)
      (wrap-frame-options {:allow-from "*"})))

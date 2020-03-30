(ns datains.routes
  "Main Compojure routes tables. `/api/` routes are in `datains.api.routes`."
  (:require
   [datains.api.choppy-app :as app-route]
   [datains.api.project :as project-route]
   [datains.api.workflow :as workflow-route]
   [datains.api.report :as report-route]
   [datains.api.notification :as notification-route]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [reitit.ring.coercion :as coercion]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [datains.middleware.formats :as formats]
   [datains.middleware.exception :as exception]))

(defn service-routes []
  (merge
   ["/api"
    {:coercion spec-coercion/coercion
     :muuntaja formats/instance
     :swagger {:id ::api}
     :middleware [;; query-params & form-params
                  parameters/parameters-middleware
                 ;; content-negotiation
                  muuntaja/format-negotiate-middleware
                 ;; encoding response body
                  muuntaja/format-response-middleware
                 ;; exception handling
                  exception/exception-middleware
                 ;; decoding request body
                  muuntaja/format-request-middleware
                 ;; coercing response bodys
                  coercion/coerce-response-middleware
                 ;; coercing request parameters
                  coercion/coerce-request-middleware]}

   ;; swagger documentation
    ["" {:no-doc true
         :swagger {:info {:title "Datains API Management System"
                          :description "http://datains-api.3steps.cn/"}}}

     ["/swagger.json"
      {:get (swagger/create-swagger-handler)}]

     ["/api-docs/*"
      {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]]]

   ;; The group of routes for app store
   app-route/app
   project-route/project
   workflow-route/workflow
   report-route/report
   notification-route/notification))
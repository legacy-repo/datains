(ns datains.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [datains.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[datains started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[datains has shut down successfully]=-"))
   :middleware wrap-dev})

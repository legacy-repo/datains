(ns datains.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[datains started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[datains has shut down successfully]=-"))
   :middleware identity})

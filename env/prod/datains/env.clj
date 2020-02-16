(ns datains.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "*****-=[datains started successfully]=-*****"))
   :stop
   (fn []
     (log/info "*****-=[datains has shut down successfully]=-*****"))
   :middleware identity})

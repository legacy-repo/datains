(ns datains.routes.response
  (:require
   [ring.util.http-response :refer [ok not-found]]))


(defn ok-or-not-found [data]
  (if (nil? data)
    (not-found {:message "Not found record given by id"}) (ok data)))
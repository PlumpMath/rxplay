(ns rxplay.config
  (:require [environ.core :refer [env]]
            [mount.core :refer [defstate]])
  (:gen-class))

  (defstate state :start
    {:port (or (env :port) 8080)
     :group-threads 8})

(ns rxplay.util
  (:require [ring.util.response :as ring]
            [cheshire.core :refer [generate-string]]
            [org.httpkit.server :refer [send!]])
  (:gen-class))

(defn send-http!
  "Send an HTTP response on an http-kit async channel."
  [chan body & [{:keys [status] :or {status 200} :as opts}]]
  (send! chan
         (-> body
             generate-string
             ring/response
             (ring/content-type "application/json")
             (ring/status status))))

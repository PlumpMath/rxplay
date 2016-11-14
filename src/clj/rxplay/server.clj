(ns rxplay.server
  (:require [rxplay.tasks :as tasks]
            [rxplay.config :as config]
            [clojure.java.io :as io]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [org.httpkit.server :refer [run-server]]
            [mount.core :refer [defstate start]])
  (:gen-class))

(defn handle-post-group
  [{:keys [body chan] :as req}]
  (let [group (assoc body :chan chan)]
    (.onNext tasks/group-subject group)))

(defroutes routes
  (GET "/" _
    {:status 200
     :headers {"Content-Type" "text/html; charset=utf-8"}
     :body (io/input-stream (io/resource "public/index.html"))})
  (POST "/group" _ handle-post-group)
  (resources "/"))

(def http-handler
  (-> routes
      (wrap-defaults api-defaults)
      (wrap-json-body {:keywords? true})
      wrap-with-logger
      wrap-json-response
      wrap-gzip))

(defstate http-server
  :start
  #(run-server http-handler {:port (:port config/state) :join? false}))

(defn -main []
  (start))

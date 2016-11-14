(ns rxplay.tasks
  (:require [rxplay.util :as util]
            [rxplay.config :as config]
            [rx.lang.clojure.core :as rx]
            [mount.core :refer [defstate]])
  (:import [java.util.concurrent Executors TimeUnit]
           [rx.subjects PublishSubject SerializedSubject]
           [rx.schedulers Schedulers])
  (:gen-class))

(defstate group-state :start (atom {:last-id 0 :groups {}}))
(defstate group-thread-pool
  :start (Executors/newFixedThreadPool (config/state :group-threads)))

(defn alloc-group-id! []
  (-> group-state
      (swap! update :last-id inc)
      :last-id))

(defn update-group!
  [{:keys [id] :as group}]
  (swap! group-state assoc-in [:groups id] group))

(defn create-group-subject []
  (->> (PublishSubject/create)
       (SerializedSubject.)
       (rx/subscribe-on (Schedulers/from group-thread-pool))
       (rx/map #(assoc % :id (alloc-group-id!)))
       (rx/do #(util/send-http! (:chan %) %))
       (rx/do update-group!)))

(defstate group-subject :start #(create-group-subject))

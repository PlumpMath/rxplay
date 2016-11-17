(ns rxplay.tasks
  (:require [rxplay.util :as util]
            [rxplay.config :as config]
            [rx.lang.clojure.core :as rx]
            [mount.core :refer [defstate]])
  (:import [java.util.concurrent Executors TimeUnit]
           [rx.subjects PublishSubject SerializedSubject]
           [rx.schedulers Schedulers])
  (:gen-class))

(def tasks-db
  {1 {:id 1 :description "Task 1" :duration 3}
   2 {:id 2 :description "Task 2" :duration 10}})

(defstate group-state :start (atom {}))
(defstate group-thread-pool
  :start (Executors/newFixedThreadPool (config/state :group-threads))
  :stop (.shutdown group-thread-pool))

(defstate task-state :start (atom {}))
(defstate task-thread-pool
  :start (Executors/newFixedThreadPool (config/state :task-threads))
  :stop (.shutdown task-thread-pool))

(defstate id-observable :start (.serialize (rx/seq->o (iterate inc 1))))
(defstate group-subject :start (SerializedSubject. (PublishSubject/create)))

(defn create-group-observable []
  (->> (rx/map #(assoc %1 :id %2)
               (rx/subscribe-on (Schedulers/from group-thread-pool) group-subject)
               id-observable)
       (rx/do #(swap! group-state assoc (:id %) %))
       (rx/do #(util/send-http! (:chan %) %))))

(defstate group-observable :start (create-group-observable))

(defn make-group-tasks [{:keys [tasks args id] :as group}]
  (rx/seq->o
    (for [tid tasks arg args :let [task (tasks-db tid)]]
      (-> task
          (dissoc :id)
          (assoc :task-id tid :arg arg :group-id id)))))

(defn create-task-observable []
  (->> (rx/map #(assoc %1 :id %2)
               (->> group-observable
                    (rx/subscribe-on (Schedulers/from task-thread-pool))
                    (rx/flatmap make-group-tasks))
               id-observable)
       (rx/map #(assoc % :state :waiting))
       (rx/do #(swap! task-state assoc (:id %) %))
       (rx/do #(swap! group-state assoc-in [(:group-id %) :task-runs (:id %)] (:state %)))
       (rx/flatmap #(.delay (rx/return %) (:duration %) TimeUnit/SECONDS))
       (rx/map #(assoc % :state :done))
       (rx/do #(swap! task-state assoc (:id %) %))
       (rx/do #(swap! group-state assoc-in [(:group-id %) :task-runs (:id %)] (:state %)))))

(defstate task-observable :start (create-task-observable))

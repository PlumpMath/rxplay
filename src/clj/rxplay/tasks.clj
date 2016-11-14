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
  {1: {:id 1 :description "Task 1"}
   2: {:id 2 :description "Task 2"}})

(defstate group-state :start (atom {:last-id 0 :groups {}}))
(defstate group-thread-pool
  :start (Executors/newFixedThreadPool (config/state :group-threads)))

(defstate task-state :start (atom {:last-id 0 :tasks {}}))
(defstate task-thread-pool
  :start (Executors/newFixedThreadPool (config/state :task-threads)))

(defn alloc-id! [state]
  (-> state (swap! update :last-id inc) :last-id))

(defn update-state!
  [state data-path {:keys [id] :as data}]
  (swap! state assoc-in (concat data-path id) data))

(defn create-group-subject [] (SerializedSubject. (PublishSubject/create)))

(defn create-group-observable [group-subj]
  (->> group-subj
       (rx/subscribe-on (Schedulers/from group-thread-pool))
       (rx/map #(assoc % :id (alloc-id! group-state)))
       (rx/do #(update-state! group-state [:groups] %))
       (rx/do #(util/send-http! (:chan %) %))))

(defstate group-subject :start (create-group-subject))
(defstate group-observable :start (create-group-observable group-subject))

(defn make-group-tasks
  [{:keys [tasks args] :as group}]
  (for [tid tasks arg args] 
    (rx/return (assoc (tasks-db tid) :arg arg))))

(defn make-task-observable [group-obs]
  (->> group-obs
       (rx/subscribe-on (Schedulers/from task-thread-pool))
       (rx/flatmap make-group-tasks)
       (rx/do #(update-state task-state [:tasks] %))))
       

(ns rxplay.tasks
  (:require [rx.lang.clojure.core :as rx])
  (:import [java.util.concurrent TimeUnit])
  (:gen-class))

(defonce tasks
  {1 {:id 1 :duration 1000}
   2 {:id 2 :duration 3000}
   3 {:id 3 :duration 20000}})

(defn do-tasks!
  [tsq]
  (->> [(map rx/return tsq) (map :duration tsq)]
       (apply map #(.delay %1 %2 TimeUnit/MILLISECONDS))
       (apply rx/merge)))

(defn tst []
  (-> tasks vals do-tasks! (rx/subscribe prn)))

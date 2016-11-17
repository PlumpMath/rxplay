(ns rxplay.example-test
  (:require [clojure.test :refer :all]
            [rxplay.tasks]
            [mount.core :as mount]
            [rx.lang.clojure.core :as rx]
            [rxplay.server :as server]
            [rx.lang.clojure.blocking :as rx.blocking])
  (:gen-class))

(deftest task-observable-task-creation
  (let [cfg (-> (mount/except [#'server/http-server])
                (mount/swap {#'rxplay.tasks/group-observable
                             (rx/return {:id 1 :tasks [2] :args [42]})
                             #'rxplay.tasks/task-state
                             (atom {})
                             #'rxplay.tasks/group-state
                             (atom {})}))
        exp (merge (rxplay.tasks/tasks-db 2)
                   {:id 1 :group-id 1 :task-id 2 :arg 42 :state :done})]
    (mount/start cfg)
    (is (= exp (rx.blocking/first rxplay.tasks/task-observable)))
    (is (= {1 exp} @rxplay.tasks/task-state))
    (is (= {1 {:task-runs {1 :done}}} @rxplay.tasks/group-state))
    (mount/stop)))

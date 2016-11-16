(ns rxplay.example-test
  (:require [clojure.test :refer :all]
            [rxplay.tasks :as tasks]
            [mount.core :as mount]
            [rx.lang.clojure.core :as rx]
            [rxplay.server :as server]
            [rx.lang.clojure.blocking :refer [first] :rename {first bf}]))

(deftest task-creation
  (let [grp (rx/return {:id 1 :tasks [1] :args [42]})
        ts (atom {})
        gs (atom {})
        cfg (-> (mount/except [#'rxplay.server/http-server])
                (mount/swap {#'rxplay.tasks/group-observable grp
                             #'rxplay.tasks/task-state ts
                             #'rxplay.tasks/group-state gs})
                (mount/start))
        res (bf tasks/task-observable)
        _ (mount/stop)
        exp (merge {:id 1 :group-id 1 :task-id 1 :arg 42 :state :done} (tasks/tasks-db 1))]
    (is (= res exp))
    (is (= @ts {1 exp}))
    (is (= @gs {1 {:task-runs {1 :done}}}))))

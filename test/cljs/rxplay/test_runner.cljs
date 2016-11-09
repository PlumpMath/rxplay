(ns rxplay.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [rxplay.core-test]
   [rxplay.common-test]))

(enable-console-print!)

(doo-tests 'rxplay.core-test
           'rxplay.common-test)

(ns io.github.rutledgepaulv.transiting.core-test
  (:require [clojure.test :refer :all])
  (:require [io.github.rutledgepaulv.transiting.core :as transiting])
  (:import (java.io PipedInputStream PipedOutputStream)
           (java.sql Date)
           (java.time Instant)
           (java.util.regex Pattern)))

(defn round-trip [data]
  (let [piped-output  (PipedOutputStream.)
        piped-input   (PipedInputStream. piped-output)
        encoding-task (future (transiting/encode data piped-output))]
    (transiting/decode piped-input)))

(deftest round-tripping-test
  (are [x] (let [y x z (round-trip y)]
             (if (instance? Pattern y)
               (= (str y) (str z))
               (= y z)))
    "1"
    (Instant/now)
    (random-uuid)
    (keyword "some" "thing with spaces")
    [1 2 3]
    ['a 'b 'c]
    #{:r :e :s :p :c :t}
    {:a 1 :b 2 :c 3}
    {[1 2 3] #{4 5 6}}
    #"this is a regex"
    (Date/from (Instant/now))
    12345
    12345.6789
    true
    false
    (tagged-literal 'something "something else")
    (list :x :y :z)))

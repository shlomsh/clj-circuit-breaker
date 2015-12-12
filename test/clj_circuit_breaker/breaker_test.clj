(ns clj-circuit-breaker.breaker-test
  (:use
    [com.stuartsierra.component :as component])
  (:require [clojure.test :refer :all]
            [clj-circuit-breaker.protocol :as breaker]
            [clj-circuit-breaker.breaker :as breakerc]
            [clj-circuit-breaker.breaker-factory :as breaker-factory]))

(defonce DEFAULT_FAIL_THRESHOLD 100)
(defonce DEFAULT_TIMEOUT_SEC 60)
(defonce MAX_ALLOWED_TIMESTAMP_SEC 300)

(deftest test-sanity
  (testing "tripped? on default config"
    (let [endpoint "www.circuit-breaker.com"
          sys (component/system-map :cb (breaker-factory/create-circuit-breaker nil DEFAULT_TIMEOUT_SEC DEFAULT_FAIL_THRESHOLD MAX_ALLOWED_TIMESTAMP_SEC))
          main-sys (component/start sys)
          cb (:cb main-sys)]
      (is (false? (breaker/tripped? cb endpoint)))
      (doseq [_ (range 21)] (breaker/record-failure! cb endpoint))
      (is (false? (breaker/tripped? cb endpoint)))
      (doseq [_ (range 80)] (breaker/record-failure! cb endpoint))
      (is (breaker/tripped? cb endpoint))
      (breaker/reset-all-circuits! cb)
      (is (not (breaker/tripped? cb endpoint)))))

  (testing "tripped? on given config"
    (let [endpoint "www.circuit-breaker.com"
          sys (component/system-map :cb (breaker-factory/create-circuit-breaker {endpoint {:threshold 20}} DEFAULT_TIMEOUT_SEC DEFAULT_FAIL_THRESHOLD MAX_ALLOWED_TIMESTAMP_SEC))
          main-sys (component/start sys)
          cb (:cb main-sys)]
      (is (not (breaker/tripped? cb endpoint)))
      (doseq [_ (range 21)] (breaker/record-failure! cb endpoint))
      (is (breaker/tripped? cb endpoint))
      (breaker/record-success! cb endpoint)
      (is (not (breaker/tripped? cb endpoint)))
      (breaker/reset-all-circuits! cb)))

  (testing "single success closed the circuit break"
    (let [endpoint "www.circuit-breaker.com"
          sys (component/system-map :cb (breaker-factory/create-circuit-breaker {endpoint {:threshold 20}} DEFAULT_TIMEOUT_SEC DEFAULT_FAIL_THRESHOLD MAX_ALLOWED_TIMESTAMP_SEC))
          main-sys (component/start sys)
          cb (:cb main-sys)]
      (is (not (breaker/tripped? cb endpoint)))
      (doseq [_ (range 21)] (breaker/record-failure! cb endpoint))
      (is (breaker/tripped? cb endpoint))
      (breaker/record-success! cb endpoint)
      (is (not (breaker/tripped? cb endpoint)))))

  (testing "tripped? on given timeout config"
    (let [endpoint "www.circuit-breaker.com"
          timeout 1
          sys (component/system-map :cb (breaker-factory/create-circuit-breaker {endpoint {:timeout timeout :threshold 20}} DEFAULT_TIMEOUT_SEC DEFAULT_FAIL_THRESHOLD MAX_ALLOWED_TIMESTAMP_SEC))
          main-sys (component/start sys)
          cb (:cb main-sys)]
      (is (not (breaker/tripped? cb endpoint)))
      (doseq [_ (range 21)] (breaker/record-failure! cb endpoint))
      (is (breaker/tripped? cb endpoint))
      (Thread/sleep (* 1000 timeout))
      (is (not (breaker/tripped? cb endpoint)))))

  (testing "extending the timeout of consecutive failures"
    (let [endpoint "www.circuit-breaker.com"
          timeout 1
          sys (component/system-map :cb (breaker-factory/create-circuit-breaker {endpoint {:timeout timeout :threshold 20}} DEFAULT_TIMEOUT_SEC DEFAULT_FAIL_THRESHOLD MAX_ALLOWED_TIMESTAMP_SEC))
          main-sys (component/start sys)
          cb (:cb main-sys)]
      (is (not (breaker/tripped? cb endpoint)))
      (doseq [_ (range 21)] (breaker/record-failure! cb endpoint))
      (is (breaker/tripped? cb endpoint))
      ; record another failure after half a sec
      (Thread/sleep (* 500 timeout))
      (breaker/record-failure! cb endpoint)
      ; complete the 1 sec of timeout
      (Thread/sleep (* 500 timeout))
      ; circuit should be still opened because of the consecutive failure, it was extended
      (is (breaker/tripped? cb endpoint))
      ; sleep for a second since last failure
      (Thread/sleep (* 500 timeout))
      (is (not (breaker/tripped? cb endpoint)))))

  (testing "test rare invoking, yet dead endpoint"
    (let [endpoint "www.circuit-breaker.com"
          timeout 1
          sys (component/system-map :cb (breaker-factory/create-circuit-breaker {endpoint {:timeout timeout :threshold 20}} DEFAULT_TIMEOUT_SEC DEFAULT_FAIL_THRESHOLD MAX_ALLOWED_TIMESTAMP_SEC))
          main-sys (component/start sys)
          cb (:cb main-sys)]
      (is (not (breaker/tripped? cb endpoint)))
      (doseq [_ (range 21)] (breaker/record-failure! cb endpoint))
      (is (breaker/tripped? cb endpoint))
      (Thread/sleep (* 2000 timeout))
      ; now 2 sec has passed, circuit closes again on timeout
      (is (not (breaker/tripped? cb endpoint)))
      ; keeping like that will never prevent the failures from blocking
      (breaker/record-failure! cb endpoint)
      ; should try again but now with a longer timeout
      (Thread/sleep (* 1500 timeout))
      ; expect the circuit to be still tripped so the algorithm prevents that although the configu is 1 sec
      (is (breaker/tripped? cb endpoint))
      ; sleep again for just a bit
      (Thread/sleep (* 500 timeout))
      (is (not (breaker/tripped? cb endpoint)))))

  (testing "test high throughput invoking, another thread already updated"
    (let [endpoint "www.circuit-breaker.com"
          timeout 1
          sys (component/system-map :cb (breaker-factory/create-circuit-breaker {endpoint {:timeout timeout :threshold 20}} DEFAULT_TIMEOUT_SEC DEFAULT_FAIL_THRESHOLD MAX_ALLOWED_TIMESTAMP_SEC))
          main-sys (component/start sys)
          cb (:cb main-sys)]
      (is (not (breaker/tripped? cb endpoint)))
      (doseq [_ (range 21)] (breaker/record-failure! cb endpoint))
      (is (breaker/tripped? cb endpoint))
      (Thread/sleep (* 2000 timeout))
      ; now 2 sec has passed, circuit closes again on timeout
      (is (not (breaker/tripped? cb endpoint)))
      (breaker/record-failure! cb endpoint)
      (is (>= 2 (:timeout (breakerc/get-circuit-config cb endpoint)))))))
(ns clj-circuit-breaker.breaker-factory
  (:require [clj-circuit-breaker.breaker :refer [->CircuitBreakerComponent]]))

(defn create-circuit-breaker
  "a factory method to create a circuit breaker component, please note the system should not require the component but just the factory and protocol.
  there is a function overload incase you want to use your own circuit breaker strategy"
  [circuit-breakers-config default-timeout default-fail-threshold max-allowed-timeout]
  (->CircuitBreakerComponent circuit-breakers-config default-timeout default-fail-threshold max-allowed-timeout))
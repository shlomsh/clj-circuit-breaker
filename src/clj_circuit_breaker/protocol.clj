(ns clj-circuit-breaker.protocol)

; @auhor Shlomi Shemesh
(defprotocol CircuitBreaker
  "The Circuit breaker protocol
  (if (breaker/tripped? circuit-breaker circuit-name)
        (do-fake-action)
         (try
            (do-action)
            (breaker/record-success! circuit-breaker circuit-name)
             (catch Exception e (breaker/record-failure! circuit-breaker circuit-name))))"
  (tripped? [this circuit-name]
    "should be tested before invoking the monitored operation, incase the circuit breaker is tripped the operation should be skipped
    the success or failure of the monitored operation should feedback the circuit breaker mechanism so it is up to date")
  (record-success! [this circuit-name]
    "should be invoked when the monitored operation succeed, it will delete all failure counters for the given circuit")
  (record-failure! [this circuit-name]
    "should be invoked when the monitored operation fails, it will increment the failure counters and trigger a circuit break if
    the treashold is hit")
  (reset-all-circuits! [this])
  (reset-all-circuit-counters! [this])
  ;(get-circuit-config [this circuit-name])
  )
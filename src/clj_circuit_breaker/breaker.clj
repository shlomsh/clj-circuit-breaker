(ns clj-circuit-breaker.breaker
  (:use
    [com.stuartsierra.component :as component]
    [clj-circuit-breaker.protocol :as breaker])
  (:require
    [taoensso.timbre :as timbre :refer (log trace debug info warn error fatal report logf tracef debugf infof warnf errorf fatalf reportf spy get-env log-env)]
    [clj-circuit-breaker.concurrent-map :as concurrent-map]
    [clj-time.core :as time]))

(defn get-circuit-config [this circuit-name]
  (get (:circuit-breakers-config this) circuit-name))

(defn- set-circuit-config-timeout [this circuit-name timeout]
  (concurrent-map/put (:circuit-breakers-config this)
                      circuit-name
                      (assoc (get-circuit-config this circuit-name) :timeout timeout)))

(defn- get-circuit-break-timeout [this circuit-name]
  (concurrent-map/get (:circuit-breakers-open this) circuit-name))

(defn- failure-count [this circuit-name]
  (concurrent-map/get (:circuit-breakers-counters this) circuit-name 0))

(defn- inc-counter [this circuit-name]
  (let [circuit-count (concurrent-map/get (:circuit-breakers-counters this) circuit-name 0)]
    (concurrent-map/put (:circuit-breakers-counters this) circuit-name (inc circuit-count))))

(defn- record-circuit-break! [this circuit-name default-timeout max-allowed-timeout]
  "circuit breaker timestamp write"
  (let [current-break-timeout (get-circuit-break-timeout this circuit-name)
        configured-timeout (:timeout (get-circuit-config this circuit-name) default-timeout)
        current-time (time/now)
        last-fail (when current-break-timeout (time/minus current-break-timeout (time/seconds configured-timeout)))
        updated-timeout (if (and last-fail (time/before? last-fail current-time)) (min (max configured-timeout (time/in-seconds (time/interval last-fail current-time))) max-allowed-timeout) configured-timeout)
        calculated-timeout (if (> updated-timeout configured-timeout) (do (debug "new breaker timeout for" circuit-name updated-timeout) (set-circuit-config-timeout this circuit-name updated-timeout) updated-timeout) configured-timeout)
        new-cb-timeout (time/plus current-time (time/seconds calculated-timeout))]
    (concurrent-map/put (:circuit-breakers-open this) circuit-name new-cb-timeout)))

;; CircuitBreakerComponent
(defrecord CircuitBreakerComponent [base-config default-timeout default-fail-threshold max-allowed-timeout]
  component/Lifecycle breaker/CircuitBreaker

  (start [component]
    (info ";; Starting CircuitBreakerComponent")
    (let [running? (atom true)
          circuit-breakers-counters (concurrent-map/new)
          circuit-breakers-open (concurrent-map/new)
          circuit-breakers-config (concurrent-map/new)
          _ (concurrent-map/put-all base-config circuit-breakers-config)]
      (assoc component :is-running running? :circuit-breakers-counters circuit-breakers-counters :circuit-breakers-config circuit-breakers-config :circuit-breakers-open circuit-breakers-open)))

  (stop [component]
    (info ";; Stopping CircuitBreakerComponent")
    (reset! (:is-running component) false)
    (dissoc component :is-running :circuit-breakers-counters :circuit-breakers-config :circuit-breakers-open))

  (record-success! [this circuit-name]
    (when (get-circuit-break-timeout this circuit-name)
      (do (concurrent-map/remove (:circuit-breakers-open this) circuit-name)
          (warn "circuit breaker closed by success record" (failure-count this circuit-name) "for:" circuit-name)))
    (concurrent-map/put (:circuit-breakers-counters this) circuit-name 0))

  (record-failure! [this circuit-name]
    (inc-counter this circuit-name)
    (when (> (failure-count this circuit-name) (:threshold (get-circuit-config this circuit-name) default-fail-threshold))
      (record-circuit-break! this circuit-name default-timeout max-allowed-timeout)))

  (reset-all-circuit-counters! [this]
    (concurrent-map/clear (:circuit-breakers-counters this)))

  (reset-all-circuits! [this]
    (reset-all-circuit-counters! this)
    (concurrent-map/clear (:circuit-breakers-open this)))

  (tripped? [this circuit-name]
    "check if there is a circuit breaker timestamp recorded and is it not after circuit planned close timestamp already."
    (let [circuit-break-timeout (get-circuit-break-timeout this circuit-name)]
      (and (boolean circuit-break-timeout) (not (time/after? (time/now) circuit-break-timeout)))))
  )
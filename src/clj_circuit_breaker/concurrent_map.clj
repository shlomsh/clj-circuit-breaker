(ns clj-circuit-breaker.concurrent-map
  (:refer-clojure :exclude [keys get remove])
  (:import java.util.concurrent.ConcurrentHashMap))

; see rich hickey little experiment on atom vs. concurrenthashmap
; http://markmail.org/message/576pstenphf5fms3

(defn new [] (ConcurrentHashMap.))

(defn put [map key value]
  (.put map key value))

(defn put-all [source target]
  (doseq [key (clojure.core/keys source)]
    (put target key (clojure.core/get source key)))
  target)

(defn get
  ([map key]
   (.get map key))
  ([map key default]
   (or (.get map key) default)))

(defn clear [map]
  (.clear map))

(defn remove [map key]
  (.remove map key))
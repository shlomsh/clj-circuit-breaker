(defproject clj-circuit-breaker "0.4.0"
  :description "Circuit breaker pattern implementation using clojure"
  :url "http://appsflyer.com/"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.taoensso/timbre "4.1.4"]
                 [clj-time "0.11.0"]
                 [com.stuartsierra/component "0.2.2"]]
  )
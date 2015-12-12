# clj-circuit-breaker

A Clojure library that implements the circuit breaker design pattern.

## Usage

(if (breaker/tripped? circuit-breaker circuit-name)
  (do-fake-action)
   (try
      (do-action)
      (breaker/record-success! circuit-breaker circuit-name)
       (catch Exception e (breaker/record-failure! circuit-breaker circuit-name))))

## License

Copyright © 2015 

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

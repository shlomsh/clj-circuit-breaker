# clj-circuit-breaker

A Clojure library that implements the circuit breaker design pattern.

Circuit breakers are typically used when your program makes remote calls.
Remote calls can often hang for a while before they time out. If your
application makes a lot of these requests, many resources can be tied
up waiting for these time outs to occur. A circuit breaker wraps these
remote calls and will trip after a defined amount of failures or time outs
occur. When a circuit breaker is tripped any future calls will avoid making
the remote call and return an error to the caller. In the meantime, the
circuit breaker will periodically allow some calls to be tried again and
will close the circuit if those are successful.

You can read more about this pattern and how it's used at:
- [Martin Fowler's bliki](http://martinfowler.com/bliki/CircuitBreaker.html)
- [The Netflix Tech Blog](http://techblog.netflix.com/2012/02/fault-tolerance-in-high-volume.html)
- [Release It!](http://pragprog.com/book/mnee/release-it)


## Code Example
```go
(if (breaker/tripped? circuit-breaker circuit-name)
  (do-fake-action)
    (try
      (do-action)
      (breaker/record-success! circuit-breaker circuit-name)
       (catch Exception e (breaker/record-failure! circuit-breaker circuit-name))))
```

## Include in your code
[![Clojars Project](http://clojars.org/clj-circuit-breaker/latest-version.svg)](http://clojars.org/clj-circuit-breaker)

## License

Copyright © 2015 
Distributed under the Eclipse Public License either version 2.0.

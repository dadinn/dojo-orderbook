(ns dojo-orderbook.core
  (:require
   [clojure.core.async :as ca :refer [>! <! <!! >!!]]))

(defrecord Order [name price buy?])

(defrecord Trade [buy-name sell-name price])

(defn rand-bool
  "Returns a random boolean value."
  []
  (= 1 (rand-int 2)))

(defn rand-trader
  "Creates a go process which randomly generates buy or sell limit orders for *name*, with price between *min-val* and *max-val* every *msec* milliseconds and puts them on channel *ch*."
  [ch name min-val max-val msecs]
  (ca/go
    (loop [tout (ca/timeout msecs)]
      (let [price (+ min-val (rand-int (- max-val min-val)))
            order (->Order name price (rand-bool))]
        (>! ch order))
      (<! tout)
      (recur (ca/timeout msecs)))))

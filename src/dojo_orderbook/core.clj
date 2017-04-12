(ns dojo-orderbook.core
  (:require
   [clojure.core.async :as async]))

(defrecord LimitOrder [name price buy?])

(defrecord TradeMatch [buy-name sell-name price])

(defn rand-bool
  "Returns a random boolean value."
  []
  (= 1 (rand-int 2)))

(defprotocol Trader
  (start [this ch]
    "start trading by putting buy/sell limit-orders on the channel *ch*"))

(defrecord RandomAgent [name min-val max-val max-ordertime worktime]
  Trader
  (start [this ch]
    (async/go
      (loop [curr-ordertime max-ordertime]
        (let [price (+ min-val (rand-int (- max-val min-val)))
              buy-or-sell (rand-bool)
              order (->LimitOrder name price buy-or-sell)
              tout (async/timeout curr-ordertime)]
          (async/>! ch order)
          (async/<! tout)
          (when (< max-ordertime @worktime)
            (swap! worktime - curr-ordertime)
            (recur (rand-int max-ordertime))))))))

(def example-agents
  [(->RandomAgent "Adam" 50 500 5000 (atom 50000)) ; on average sends 100 orders/day every 5 secs each, works 500 secs/day
   (->RandomAgent "John" 20 100 3000 (atom 3000000)) ; on average sends 1000 orders/day every 3 secs each, works 1000 secs/day
   (->RandomAgent "James" 30 60 10000 (atom 5000000)) ; on average sends 500 orders/day every 10 secs each, works 5000 secs/day
   (->RandomAgent "Robert" 5 500 100000 (atom 3000000)) ; on average sends 30 order/day every 100 secs each, works 3000 secs/day
   (->RandomAgent "Thomas" 10 100 500000 (atom 50000000))]) ; on avg sends 100 order/day every 500 secs each, works 50,000 secs/day

(defn gen-orders
  "Returns a channel where 5 random traders are continously generating new randomized limit-orders."
  [agent-specs]
  (let [in (async/chan (count agent-specs))
        traders (vec (map #(start % in) agent-specs))]
    in))

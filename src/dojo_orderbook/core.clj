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

(defn addorder [orders price name]
  (if (seq (orders price))
    (update orders price conj name)
    (assoc orders price [name])))

(defn remorder [orders price]
  (if (< 1 (count (orders price)))
    (update orders price subvec 1)
    (dissoc orders price)))

(defn trade-matcher
  "returns a go process which takes incoming buy/sell limit-orders from the channel *in*, and places any trade matches on channel *out*"
  [in out]
  (async/go
    (loop [curr-item (async/<! in)
           buys (sorted-map-by >)
           sells (sorted-map-by <)]
      (if-let [{curr-name :name curr-price :price curr-buy? :buy?} curr-item]
        (if curr-buy?
          (let [[sell-price sell-names :as first-sell] (first sells)]
            (if (and first-sell (<= sell-price curr-price))
              (let [sell-name (first sell-names)]
                (async/>! out
                  (->TradeMatch
                    curr-name
                    sell-name
                    sell-price))
                (recur (async/<! in)
                  buys
                  (remorder sells sell-price)))
              (recur (async/<! in)
                (addorder buys curr-price curr-name)
                sells)))
          (let [[buy-price buy-names :as first-buy] (first buys)]
            (if (and first-buy (<= curr-price buy-price))
              (let [buy-name (first buy-names)]
                (async/>! out
                  (->TradeMatch
                    buy-name
                    curr-name
                    buy-price))
                (recur (async/<! in)
                  (remorder buys buy-price) sells))
              (recur (async/<! in)
                buys (addorder sells curr-price curr-name)))))
        (async/close! out)))))

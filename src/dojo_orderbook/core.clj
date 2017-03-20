(ns dojo-orderbook.core)

(defrecord Order [name price buy?])

(defrecord Trade [buy-name sell-name price])


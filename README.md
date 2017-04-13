# dojo-orderbook

Experimental Clojure app to demonstrate [core.async](https://github.com/clojure/core.async) concurrency features by implementing a limit-order matching engine, a market simulator with agents generating randomized buy/sell limit orders.

## Usage

After cloning the git repository and setting up [leiningen](https://leiningen.org/), the app can be run from the command line with the following command as an example:

    lein run 100

The main function take a single argument, the number of trades to be matched.

## License

Copyright © 2017 Daniel Dinnyes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

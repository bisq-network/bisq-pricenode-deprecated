bisq-pricenode
========


Overview
--------

The Bisq price relay node ("pricenode" for short) is a simple HTTP service that fetches data from third-party price providers and relays that data to Bisq exchange clients on request. Available prices include:

 - BTC exchange rates, available at `/getAllMarketPrices`, and
 - BTC mining fees, available at `/getFees`

Price relays are deployed as Tor hidden services. This is not because the location of these nodes needs to be kept secret, but rather so that Bisq exchange clients do not need to exit the Tor network in order to get price data.

Anyone can run a price relay, but a relay must be _discoverable_ in order for it to do any good. For exchange clients to discover your relay, its .onion address must be (a) hard coded in the Bisq exchange client's `ProviderRepository` class or (b) specified explicitly via the exchange client's `--providers` command line option.

Price relays can be run anywhere Java and Tor binaries can be run. Instructions below cover deployment on localhost and Heroku, but nothing in principle prevents these nodes from being run across a broader selection of platforms and regions. Note however that the exchange client is currently naive about selecting price relays (it does so once randomly at startup), but this could be improved in various ways to accommodate a larger number of better-distributed nodes.

Price relays should be cheap to run with regard to both time and money. The application itself is non-resource intensive and can be run on the low-end of most providers' paid tiers, and possibly even on some providers' free tiers.

A price relay operator's main responsibilities are to ensure their node(s) are available and up-to-date. Releases are currently source-only, with the assumption that most operators will favor Git-based "push to deploy" workflows. To stay up to date with releases, operators can [subscribe to this repository's releases.atom feed](https://github.com/bisq-network/pricenode/releases.atom).

Operating a production price relay is a valuable service to the Bisq network. Accordingly, operators should issue BSQ compensation requests that cover their time and money costs.


Prerequisites
--------

To run a price relay, you will need:

  - [BitcoinAverage API keys](https://bitcoinaverage.com/en/plans). Free plans are fine for local development or personal nodes; paid plans should be used for well-known production nodes.
  - [Tor Browser](https://www.torproject.org/projects/torbrowser.html.en) to verify .onion URLs.
  - JDK 8 if you want to build and run a node locally.
  - The `tor` binary (e.g. `brew install tor`) if you want to run a hidden service locally.


Deployment Instructions
--------

### On localhost

    ./gradlew installDist
    BITCOIN_AVG_PUBKEY=[your pubkey] BITCOIN_AVG_PRIVKEY=[your privkey] ./build/install/bisq-pricenode/bin/bisq-pricenode
    curl http://localhost:8080/getAllMarketPrices

To register the node as a Tor hidden service, run:

    tor -f torrc

When the process reports that it is "100% bootstrapped", leave the process running, copy your newly-generated .onion address from `build/tor-hidden-service/hostname` and then open http://$YOUR_ONION/getAllMarketPrices in your Tor Browser. You should see the same response as produced in the `curl` request above.

> NOTE: It may take a few minutes for your new .onion address to get registered and become resolvable. Registration of Tor hidden service descriptors can take some time.


### On Heroku

    heroku create
    heroku config:set BITCOIN_AVG_PUBKEY=[your pubkey] BITCOIN_AVG_PRIVKEY=[your privkey]
    git push heroku master
    curl https://your-app-123456.herokuapp.com/getAllMarketPrices

To register the node as a Tor hidden service, first install the Heroku Tor buildpack:

    heroku buildpacks:add https://github.com/hernanex3/heroku-buildpack-tor.git#c766cb6d
    git commit --allow-empty -m"Add Tor buildpack"
    git push heroku master

> NOTE: this deployment will take a while, because the new buildpack must download and build Tor from source.

Next, generate your Tor hidden service private key and .onion address:

    heroku run bash
    ./tor/bin/tor -f torrc

When the process reports that it is "100% bootstrapped", kill it, then copy the generated private key and .onion hostname values:

    cat build/tor-hidden-service/hostname
    cat build/tor-hidden-service/private_key
    exit

> IMPORTANT: Save the private key value in a secure location so that this node can be re-created elsewhere with the same .onion address in the future if necessary.

Now configure the hostname and private key values as environment variables for your Heroku app:

    heroku config:set HIDDEN=true HIDDEN_DOT_ONION=[your .onion] HIDDEN_PRIVATE_KEY="[your tor privkey]"

When the application finishes restarting, you should still be able to access it via the clearnet, e.g. with:

    curl https://your-app-123456.herokuapp.com/getAllMarketPrices

And via your Tor Browser at:

    http://$YOUR_ONION/getAllMarketPrices

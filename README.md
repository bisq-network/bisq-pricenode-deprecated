# Bisq price provider service

## Deploy locally

    ./gradlew installDist
    BITCOIN_AVERAGE_PRIVATE_KEY=[value] BITCOIN_AVERAGE_PUBLIC_KEY=[value] ./build/install/provider/bin/provider
    curl http://localhost:8080/getAllMarketPrices

## Deploy on Heroku

_The following instructions assume you have the `heroku` CLI installed (e.g. via `brew install heroku` and that you have already successfully run `heroku login`)_

    heroku create
    heroku config:set BITCOIN_AVERAGE_PRIVATE_KEY=[value]
    heroku config:set BITCOIN_AVERAGE_PUBLIC_KEY=[value]
    git push heroku master
    curl http://$HEROKU_APP_URL/getAllMarketPrices

# Bisq price provider service

    ./gradlew installDist
    BITCOIN_AVERAGE_PRIVATE_KEY=$BITCOIN_AVERAGE_PRIVATE_KEY BITCOIN_AVERAGE_PUBLIC_KEY=$BITCOIN_AVERAGE_PUBLIC_KEY ./build/install/provider/bin/provider
    curl http://localhost:8080/getAllMarketPrices

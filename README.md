# Bisq price provider service

    ./gradlew installDist
    ./build/install/provider/bin/provider $BITCOIN_AVERAGE_PRIVATE_KEY $BITCOIN_AVERAGE_PUBLIC_KEY
    curl http://localhost:8080/getAllMarketPrices

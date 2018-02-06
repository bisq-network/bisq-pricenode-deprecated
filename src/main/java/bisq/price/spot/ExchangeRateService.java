/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.price.spot;

import bisq.price.spot.providers.BitcoinAverage;
import bisq.price.spot.providers.CoinMarketCap;
import bisq.price.spot.providers.Poloniex;

import java.time.Instant;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ExchangeRateService {

    private static final long MARKET_PRICE_TTL_SEC = 1800; // 30 min

    private final BitcoinAverage.Local bitcoinAverageLocal;
    private final BitcoinAverage.Global bitcoinAverageGlobal;
    private final Poloniex poloniex;
    private final CoinMarketCap coinMarketCap;

    public ExchangeRateService(BitcoinAverage.Local bitcoinAverageLocal,
                               BitcoinAverage.Global bitcoinAverageGlobal,
                               Poloniex poloniex,
                               CoinMarketCap coinMarketCap){
        this.bitcoinAverageLocal = bitcoinAverageLocal;
        this.bitcoinAverageGlobal = bitcoinAverageGlobal;
        this.poloniex = poloniex;
        this.coinMarketCap = coinMarketCap;
    }

    public void start() throws Exception {
        bitcoinAverageLocal.start();
        bitcoinAverageGlobal.start();
        poloniex.start();
        coinMarketCap.start();
    }

    public Map<String, Object> getAllMarketPrices() {

        ExchangeRateProvider exchangeRateProvider = bitcoinAverageLocal;

        Map<? extends String, ? extends ExchangeRateData> data = exchangeRateProvider.getData();
        Collection<? extends ExchangeRateData> prices = data.values();

        String debugPrefix = exchangeRateProvider.getDebugPrefix();
        long count = prices.size();
        long timestamp = prices.stream()
                .filter(e -> exchangeRateProvider.getProviderSymbol().equals(e.getProvider()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No exchange rate data found for " + exchangeRateProvider))
                .getTimestampSec();

        Map<String, Object> allMarketPrices = new LinkedHashMap<>();
        allMarketPrices.put("btcAverageTs", timestamp);
        allMarketPrices.put(debugPrefix + "Ts", timestamp);
        allMarketPrices.put("poloniexTs", poloniex.getTimestamp());
        allMarketPrices.put("coinmarketcapTs", coinMarketCap.getTimestamp());
        allMarketPrices.put(debugPrefix + "Count", count);
        allMarketPrices.put("btcAverageGCount", bitcoinAverageGlobal.getCount());
        allMarketPrices.put("poloniexCount", poloniex.getCount());
        allMarketPrices.put("coinmarketcapCount", coinMarketCap.getCount());

        Map<String, ExchangeRateData> allData = new HashMap<>();

        // the order of the following two calls matters; we allow Global data to get overwritten by Local
        allData.putAll(bitcoinAverageGlobal.getData());
        allData.putAll(data);

        // the order of the following two calls matters; we allow CoinMarketCap data to get overwritten by Poloniex
        allData.putAll(coinMarketCap.getData());
        allData.putAll(poloniex.getData());

        allMarketPrices.put("data", removeOutdatedPrices(allData).values().toArray());

        return allMarketPrices;
    }

    private Map<String, ExchangeRateData> removeOutdatedPrices(Map<String, ExchangeRateData> map) {
        long now = Instant.now().getEpochSecond();
        long limit = now - MARKET_PRICE_TTL_SEC;
        return map.entrySet().stream()
                .filter(e -> e.getValue().getTimestampSec() > limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

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
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("btcAverageTs", bitcoinAverageLocal.getTimestamp());
        map.put("poloniexTs", poloniex.getTimestamp());
        map.put("coinmarketcapTs", coinMarketCap.getTimestamp());
        map.put("btcAverageLCount", bitcoinAverageLocal.getCount());
        map.put("btcAverageGCount", bitcoinAverageGlobal.getCount());
        map.put("poloniexCount", poloniex.getCount());
        map.put("coinmarketcapCount", coinMarketCap.getCount());

        Map<String, ExchangeRateData> data = new HashMap<>();

        // the order of the following two calls matters; we allow Global data to get overwritten by Local
        data.putAll(bitcoinAverageGlobal.getData());
        data.putAll(bitcoinAverageLocal.getData());

        // the order of the following two calls matters; we allow CoinMarketCap data to get overwritten by Poloniex
        data.putAll(coinMarketCap.getData());
        data.putAll(poloniex.getData());

        map.put("data", removeOutdatedPrices(data).values().toArray());

        return map;
    }

    private Map<String, ExchangeRateData> removeOutdatedPrices(Map<String, ExchangeRateData> map) {
        long now = Instant.now().getEpochSecond();
        long limit = now - MARKET_PRICE_TTL_SEC;
        return map.entrySet().stream()
                .filter(e -> e.getValue().getTimestampSec() > limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

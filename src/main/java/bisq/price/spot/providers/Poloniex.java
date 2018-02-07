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

package bisq.price.spot.providers;

import bisq.price.spot.ExchangeRateData;
import bisq.price.spot.support.CachingExchangeRateProvider;
import bisq.price.util.Altcoins;

import io.bisq.network.http.HttpClient;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.time.Duration;
import java.time.Instant;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Double.parseDouble;

public class Poloniex extends CachingExchangeRateProvider {

    private final HttpClient httpClient;

    public Poloniex() {
        super(
                "POLO",
                "poloniex",
                Duration.ofMinutes(1)
        );
        this.httpClient = new HttpClient("https://poloniex.com/public");
    }

    @Override
    public Map<String, ExchangeRateData> doRequestForCaching() throws IOException {
        Map<String, ExchangeRateData> marketPriceMap = new HashMap<>();
        String response = httpClient.requestWithGET("?command=returnTicker", "User-Agent", "");
        //noinspection unchecked
        LinkedTreeMap<String, Object> treeMap = new Gson().fromJson(response, LinkedTreeMap.class);
        long ts = Instant.now().getEpochSecond();
        treeMap.entrySet().stream().forEach(e -> {
            Object value = e.getValue();
            String invertedCurrencyPair = e.getKey();
            String altcoinCurrency = null;
            if (invertedCurrencyPair.startsWith("BTC")) {
                String[] tokens = invertedCurrencyPair.split("_");
                if (tokens.length == 2) {
                    altcoinCurrency = tokens[1];
                    if (Altcoins.ALL_SUPPORTED.contains(altcoinCurrency)) {
                        if (value instanceof LinkedTreeMap) {
                            //noinspection unchecked
                            LinkedTreeMap<String, Object> data = (LinkedTreeMap) value;
                            marketPriceMap.put(altcoinCurrency,
                                    new ExchangeRateData(altcoinCurrency,
                                            parseDouble((String) data.get("last")),
                                            ts,
                                            getProviderSymbol())
                            );
                        }
                    }
                } else {
                    log.error("invertedCurrencyPair has invalid format: invertedCurrencyPair=" + invertedCurrencyPair);
                }
            }
        });
        return marketPriceMap;
    }
}

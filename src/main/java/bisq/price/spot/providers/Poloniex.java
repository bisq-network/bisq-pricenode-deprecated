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
import bisq.price.util.Environment;

import io.bisq.network.http.HttpClient;

import io.bisq.common.locale.CurrencyUtil;
import io.bisq.common.locale.TradeCurrency;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.time.Instant;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Double.parseDouble;

public class Poloniex extends AbstractExchangeRateProvider {

    private static final long REQUEST_INTERVAL_MS = 60_000; // 1 min
    private static final String PROVIDER_SYMBOL = "POLO";

    private final Set<String> supportedAltcoins;
    private final HttpClient httpClient;

    private Map<String, ExchangeRateData> data;

    public Poloniex() {
        this.httpClient = new HttpClient("https://poloniex.com/public");

        supportedAltcoins = CurrencyUtil.getAllSortedCryptoCurrencies().stream()
                .map(TradeCurrency::getCode)
                .collect(Collectors.toSet());
    }

    @Override
    public void configure(Environment env) {
        // no configuration necessary
    }

    @Override
    protected long getRequestIntervalMs() {
        return REQUEST_INTERVAL_MS;
    }

    @Override
    protected void requestAndCache() throws IOException {
        long ts = System.currentTimeMillis();
        data = request();
        log.info("requestAndCache took {} ms.", (System.currentTimeMillis() - ts));

        if (data.get("LTC") != null)
            log.info("Poloniex LTC (last): " + data.get("LTC").getPrice());
    }

    public Map<String, ExchangeRateData> request() throws IOException {
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
                    if (supportedAltcoins.contains(altcoinCurrency)) {
                        if (value instanceof LinkedTreeMap) {
                            //noinspection unchecked
                            LinkedTreeMap<String, Object> data = (LinkedTreeMap) value;
                            marketPriceMap.put(altcoinCurrency,
                                    new ExchangeRateData(altcoinCurrency,
                                            parseDouble((String) data.get("last")),
                                            ts,
                                            PROVIDER_SYMBOL)
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

    @Override
    public Map<? extends String, ? extends ExchangeRateData> getData() {
        return data;
    }

    @Override
    public String getProviderSymbol() {
        return PROVIDER_SYMBOL;
    }

    @Override
    public String getDebugPrefix() {
        return "poloniex";
    }
}

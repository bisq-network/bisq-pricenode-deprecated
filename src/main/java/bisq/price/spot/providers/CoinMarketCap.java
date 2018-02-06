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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Double.parseDouble;

public class CoinMarketCap extends AbstractExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(CoinMarketCap.class);
    private static final String PROVIDER_SYMBOL = "CMC";
    private static final long REQUEST_INTERVAL_MS = 300_000;  // 5 min: large data structure; don't request too often

    private final Set<String> supportedAltcoins;
    private final HttpClient httpClient;

    private Map<String, ExchangeRateData> data;

    public CoinMarketCap() {
        this.httpClient = new HttpClient("https://api.coinmarketcap.com/");
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
            log.info("Coinmarketcap LTC (last): " + data.get("LTC").getPrice());
    }

    public Map<String, ExchangeRateData> request() throws IOException {
        Map<String, ExchangeRateData> marketPriceMap = new HashMap<>();
        String response = httpClient.requestWithGET("v1/ticker/?limit=200", "User-Agent", "");
        //noinspection unchecked
        List<LinkedTreeMap<String, Object>> list = new Gson().fromJson(response, ArrayList.class);
        long ts = Instant.now().getEpochSecond();
        list.stream().forEach(treeMap -> {
            String code = (String) treeMap.get("symbol");
            if (supportedAltcoins.contains(code)) {
                double price_btc = parseDouble((String) treeMap.get("price_btc"));
                marketPriceMap.put(code, new ExchangeRateData(code, price_btc, ts, PROVIDER_SYMBOL));
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
    public String getMetadataPrefix() {
        return "coinmarketcap";
    }

    @Override
    public int getOrder() {
        return 3;
    }
}

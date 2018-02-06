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
import bisq.price.spot.ExchangeRateProvider;
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
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Double.parseDouble;

public class CoinMarketCap implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(CoinMarketCap.class);
    private static final String PROVIDER_SYMBOL = "CMC";
    private static final long REQUEST_INTERVAL_MS = 300_000;  // 5 min: large data structure; don't request too often

    private final Timer timer = new Timer();
    private final Set<String> supportedAltcoins;
    private final HttpClient httpClient;

    private Map<String, ExchangeRateData> data;
    private long timestamp;

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

    public void start() throws IOException {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    requestAndCache();
                } catch (IOException e) {
                    log.warn(e.toString());
                    e.printStackTrace();
                }
            }
        }, REQUEST_INTERVAL_MS, REQUEST_INTERVAL_MS);

        requestAndCache();
    }

    private void requestAndCache() throws IOException {
        long ts = System.currentTimeMillis();
        data = request();
        log.info("requestAndCache took {} ms.", (System.currentTimeMillis() - ts));
        //removeOutdatedPrices(poloniexMap); // FIXME
        //removeOutdatedPrices(allPricesMap); // FIXME
        // we don't replace prices which we got form the Poloniex request, just in case the Coinmarketcap data are
        // received earlier at startup we allow them but Poloniex will overwrite them.
        // map.entrySet().stream()
                //.filter(e -> poloniexMap == null || !poloniexMap.containsKey(e.getKey())) // FIXME
                // .forEach(e -> allPricesMap.put(e.getKey(), e.getValue()));
        timestamp = Instant.now().getEpochSecond();

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

    public long getTimestamp() {
        return timestamp;
    }

    public long getCount() {
        return data.size();
    }

    public Map<? extends String, ? extends ExchangeRateData> getData() {
        return data;
    }
}

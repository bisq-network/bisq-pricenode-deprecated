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

package io.bisq.provider.price.providers;

import io.bisq.provider.price.PriceData;

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

public class CoinmarketcapProvider {

    private static final Logger log = LoggerFactory.getLogger(CoinmarketcapProvider.class);
    private final Set<String> supportedAltcoins;

    private final HttpClient httpClient;

    public CoinmarketcapProvider() {
        this.httpClient = new HttpClient("https://api.coinmarketcap.com/");
        supportedAltcoins = CurrencyUtil.getAllSortedCryptoCurrencies().stream()
                .map(TradeCurrency::getCode)
                .collect(Collectors.toSet());
    }

    public Map<String, PriceData> request() throws IOException {
        Map<String, PriceData> marketPriceMap = new HashMap<>();
        String response = httpClient.requestWithGET("v1/ticker/?limit=200", "User-Agent", "");
        //noinspection unchecked
        List<LinkedTreeMap<String, Object>> list = new Gson().fromJson(response, ArrayList.class);
        long ts = Instant.now().getEpochSecond();
        list.stream().forEach(treeMap -> {
            String code = (String) treeMap.get("symbol");
            if (supportedAltcoins.contains(code)) {
                double price_btc = parseDouble((String) treeMap.get("price_btc"));
                marketPriceMap.put(code, new PriceData(code,
                        price_btc,
                        ts));
            }
        });
        return marketPriceMap;
    }
}

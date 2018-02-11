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

import bisq.price.spot.ExchangeRate;
import bisq.price.spot.support.CachingExchangeRateProvider;

import io.bisq.network.http.HttpClient;

import org.knowm.xchange.bitcoinaverage.dto.marketdata.BitcoinAverageTicker;
import org.knowm.xchange.bitcoinaverage.dto.marketdata.BitcoinAverageTickers;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

import java.io.IOException;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * See the BitcoinAverage API documentation at https://apiv2.bitcoinaverage.com/#ticker-data-all
 */
public abstract class BitcoinAverage extends CachingExchangeRateProvider {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = new HttpClient("https://apiv2.bitcoinaverage.com/");
    private final String symbolSet;

    /**
     * @param symbolSet "global" or "local"; see https://apiv2.bitcoinaverage.com/#supported-currencies
     */
    public BitcoinAverage(String name, String prefix, Duration ttl, String symbolSet) {
        super(name, prefix, ttl);
        this.symbolSet = symbolSet;
    }

    @Override
    public Set<ExchangeRate> doGetForCache() throws IOException {

        return getTickers().entrySet().stream()
            .filter(e -> supportedCurrency(e.getKey()))
            .map(e ->
                new ExchangeRate(
                    e.getKey(),
                    e.getValue().getLast(),
                    e.getValue().getTimestamp(),
                    this.getName()
                )
            )
            .collect(Collectors.toSet());
    }

    private boolean supportedCurrency(String currencyCode) {
        // ignore Venezuelan bolivars as the "official" exchange rate is just wishful thinking
        // we should use this API with a custom provider instead: http://api.bitcoinvenezuela.com/1
        return !"VEF".equals(currencyCode);
    }

    private Map<String, BitcoinAverageTicker> getTickers() throws IOException {
        String path = String.format("indices/%s/ticker/short?crypto=BTC", symbolSet);
        String json = httpClient.requestWithGETNoProxy(path, "User-Agent", "");
        BitcoinAverageTickers value = mapper.readValue(json, BitcoinAverageTickers.class);
        return rekey(value.getTickers());
    }

    private Map<String, BitcoinAverageTicker> rekey(Map<String, BitcoinAverageTicker> tickers) {
        // go from a map with keys like "BTCUSD", "BTCVEF" to one with keys like "USD", "VEF"
        return tickers.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().substring(3), Map.Entry::getValue));
    }


    public static class Global extends BitcoinAverage {
        public Global() {
            super("BTCA_G", "btcAverageG", Duration.ofMinutes(3).plusSeconds(30), "global");
        }
    }


    public static class Local extends BitcoinAverage {
        public Local() {
            super("BTCA_L", "btcAverageL", Duration.ofMinutes(1).plusSeconds(30), "local");
        }
    }
}

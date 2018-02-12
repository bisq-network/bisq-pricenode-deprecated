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
import bisq.price.util.Altcoins;

import io.bisq.network.http.HttpClient;

import org.knowm.xchange.coinmarketcap.dto.marketdata.CoinMarketCapTicker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

import java.io.IOException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CoinMarketCap extends CachingExchangeRateProvider {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = new HttpClient("https://api.coinmarketcap.com/");

    public CoinMarketCap() {
        super("CMC", "coinmarketcap", Duration.ofMinutes(5)); // large data structure, so don't request it too often
    }

    @Override
    public Set<ExchangeRate> doGetForCache() throws IOException {

        return getTickers()
            .filter(t -> Altcoins.ALL_SUPPORTED.contains(t.getIsoCode()))
            .map(t ->
                new ExchangeRate(
                    t.getIsoCode(),
                    t.getPriceBTC(),
                    t.getLastUpdated(),
                    this.getName()
                )
            )
            .collect(Collectors.toSet());
    }

    private Stream<CoinMarketCapTicker> getTickers() throws IOException {
        TypeReference typeReference = new TypeReference<List<CoinMarketCapTicker>>() {
        };
        return mapper.<List<CoinMarketCapTicker>>readValue(getTickersJson(), typeReference).stream();
    }

    private String getTickersJson() throws IOException {
        return httpClient.requestWithGET("v1/ticker/?limit=200", "User-Agent", "");
    }
}

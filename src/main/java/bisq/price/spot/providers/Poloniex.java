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

import org.knowm.xchange.currency.Currency;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.poloniex.dto.marketdata.PoloniexMarketData;
import org.knowm.xchange.poloniex.dto.marketdata.PoloniexTicker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Poloniex extends CachingExchangeRateProvider {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = new HttpClient("https://poloniex.com/public");

    public Poloniex() {
        super("POLO", "poloniex", Duration.ofMinutes(1));
    }

    @Override
    public Map<String, ExchangeRateData> doRequestForCaching() throws IOException {

        Map<String, ExchangeRateData> exchangeRates = new HashMap<>();

        long now = Instant.now().getEpochSecond();

        getTickers().forEach(ticker -> {

            if (!ticker.getCurrencyPair().base.equals(Currency.BTC))
                return;

            String altcoin = ticker.getCurrencyPair().counter.getCurrencyCode();

            if (!Altcoins.ALL_SUPPORTED.contains(altcoin))
                return;

            exchangeRates.put(
                altcoin, new ExchangeRateData(
                    altcoin,
                    ticker.getPoloniexMarketData().getLast().doubleValue(),
                    now,
                    this.getProviderSymbol()
                )
            );
        });

        return exchangeRates;
    }

    private Stream<PoloniexTicker> getTickers() throws IOException {
        TypeReference typeReference = new TypeReference<HashMap<String, PoloniexMarketData>>() {
        };

        Map<String, PoloniexMarketData> tickers = mapper.readValue(getTickersJson(), typeReference);

        return tickers.entrySet().stream()
            .map(e -> {
                String pair = e.getKey();
                PoloniexMarketData data = e.getValue();
                String[] symbols = pair.split("_");
                return new PoloniexTicker(data, new CurrencyPair(symbols[0], symbols[1]));
            });
    }

    private String getTickersJson() throws IOException {
        return httpClient.requestWithGET("?command=returnTicker", "User-Agent", "");
    }
}

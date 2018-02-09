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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Duration;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

public class CoinMarketCap extends CachingExchangeRateProvider {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = new HttpClient("https://api.coinmarketcap.com/");

    public CoinMarketCap() {
        super("CMC", "coinmarketcap", Duration.ofMinutes(5)); // large data structure, so don't request it too often
    }

    @Override
    public Map<String, ExchangeRate> doRequestForCaching() throws IOException {
        Map<String, ExchangeRate> exchangeRates = new HashMap<>();

        String json = httpClient.requestWithGET("v1/ticker/?limit=200", "User-Agent", "");

        CoinMarketCapTicker[] tickers = mapper.readValue(json, CoinMarketCapTicker[].class);

        for (CoinMarketCapTicker ticker : tickers) {
            String altcoinCode = ticker.getName();

            if (unsupportedAltcoin(altcoinCode))
                continue;

            exchangeRates.put(
                altcoinCode, new ExchangeRate(
                    altcoinCode,
                    ticker.getPriceBTC().doubleValue(),
                    ticker.getLastUpdated().getTime(),
                    getProviderSymbol()
                )
            );
        }

        return exchangeRates;
    }

    private static boolean unsupportedAltcoin(String altcoinCode) {
        return !Altcoins.ALL_SUPPORTED.contains(altcoinCode);
    }
}

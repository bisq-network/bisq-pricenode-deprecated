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
import bisq.price.spot.support.CachingExchangeRateProvider;

import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExchangeRateService {

    private final List<ExchangeRateProvider> providers;

    public ExchangeRateService(List<ExchangeRateProvider> providers) {
        this.providers = providers;
    }

    public void start() throws Exception {
        for (ExchangeRateProvider provider : providers) {
            if (provider instanceof CachingExchangeRateProvider) {
                ((CachingExchangeRateProvider) provider).start();
            }
        }
    }

    public Map<String, Object> getAllMarketPrices() throws IOException {
        Map<String, Object> allMarketPrices = new LinkedHashMap<>();

        addMetadata(allMarketPrices);
        addExchangeRates(allMarketPrices);

        return allMarketPrices;
    }

    private void addMetadata(Map<String, Object> allMarketPrices) throws IOException {
        for (ExchangeRateProvider provider : providers) {
            Collection<ExchangeRate> prices = provider.request().values();

            String debugPrefix = provider.getMetadataPrefix();
            long count = prices.size();
            long timestamp = findFirstTimestampForProvider(prices, provider.getName());

            if (provider instanceof BitcoinAverage.Local) {
                // `git log --grep btcAverageTs` for details on this special case
                allMarketPrices.put("btcAverageTs", timestamp);
            }

            allMarketPrices.put(debugPrefix + "Ts", timestamp);
            allMarketPrices.put(debugPrefix + "Count", count);
        }
    }

    private void addExchangeRates(Map<String, Object> allMarketPrices) throws IOException {
        Map<String, ExchangeRate> exchangeRates = new HashMap<>();

        for (ExchangeRateProvider provider : providers) {
            exchangeRates.putAll(provider.request());
        }

        allMarketPrices.put("data", exchangeRates.values().toArray());
    }

    private long findFirstTimestampForProvider(Collection<ExchangeRate> prices, String providerName) {
        return prices.stream()
            .filter(e -> providerName.equals(e.getProvider()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No exchange rate data found for " + providerName))
            .getTimestamp();
    }
}

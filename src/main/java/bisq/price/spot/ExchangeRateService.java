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

import java.time.Instant;

import java.io.IOException;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExchangeRateService {

    private static final long MARKET_PRICE_TTL_SEC = 1800; // 30 min

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
        addData(allMarketPrices);

        return allMarketPrices;
    }

    private void addMetadata(Map<String, Object> allMarketPrices) throws IOException {
        for (ExchangeRateProvider provider : providers) {
            Collection<ExchangeRateData> prices = provider.request().values();

            String debugPrefix = provider.getMetadataPrefix();
            long count = prices.size();
            long timestamp = findFirstTimestampForProvider(prices, provider.getProviderSymbol());

            if (provider instanceof BitcoinAverage.Local) {
                // `git log --grep btcAverageTs` for details on this special case
                allMarketPrices.put("btcAverageTs", timestamp);
            }

            allMarketPrices.put(debugPrefix + "Ts", timestamp);
            allMarketPrices.put(debugPrefix + "Count", count);
        }
    }

    private void addData(Map<String, Object> allMarketPrices) throws IOException {
        Map<String, ExchangeRateData> allData = new HashMap<>();

        for (ExchangeRateProvider provider : providers) {
            allData.putAll(provider.request());
        }

        allMarketPrices.put("data", removeOutdatedPrices(allData).values().toArray());
    }

    private long findFirstTimestampForProvider(Collection<ExchangeRateData> prices, String providerSymbol) {
        return prices.stream()
            .filter(e -> providerSymbol.equals(e.getProvider()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No exchange rate data found for " + providerSymbol))
            .getTimestampSec();
    }

    private Map<String, ExchangeRateData> removeOutdatedPrices(Map<String, ExchangeRateData> map) {
        long now = Instant.now().getEpochSecond();
        long limit = now - MARKET_PRICE_TTL_SEC;
        return map.entrySet().stream()
            .filter(e -> e.getValue().getTimestampSec() > limit)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

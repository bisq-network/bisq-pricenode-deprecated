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

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ExchangeRateService {

    private final List<ExchangeRateProvider> providers;

    public ExchangeRateService(List<ExchangeRateProvider> providers) {
        this.providers = providers;
    }

    public void start() {
        for (ExchangeRateProvider provider : providers) {
            if (provider instanceof CachingExchangeRateProvider) {
                ((CachingExchangeRateProvider) provider).start();
            }
        }
    }

    public void stop() {
        for (ExchangeRateProvider provider : providers) {
            if (provider instanceof CachingExchangeRateProvider) {
                ((CachingExchangeRateProvider) provider).stop();
            }
        }
    }

    public Map<String, Object> getAllMarketPrices() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        List<ExchangeRate> allExchangeRates = new ArrayList<>();

        providers.forEach(p -> {
            Set<ExchangeRate> exchangeRates = p.get();
            metadata.putAll(getMetadata(p, exchangeRates));
            allExchangeRates.addAll(exchangeRates);
        });

        return new LinkedHashMap<String, Object>() {{
            putAll(metadata);
            put("data", allExchangeRates);
        }};
    }

    private Map<String, Object> getMetadata(ExchangeRateProvider provider, Set<ExchangeRate> exchangeRates) {
        Map<String, Object> metadata = new LinkedHashMap<>();

        long timestamp = getTimestamp(provider, exchangeRates);

        if (provider instanceof BitcoinAverage.Local) {
            metadata.put("btcAverageTs", timestamp);
        }

        String prefix = provider.getPrefix();
        metadata.put(prefix + "Ts", timestamp);
        metadata.put(prefix + "Count", exchangeRates.size());

        return metadata;
    }

    private long getTimestamp(ExchangeRateProvider provider, Set<ExchangeRate> exchangeRates) {
        return exchangeRates.stream()
            .filter(e -> provider.getName().equals(e.getProvider()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No exchange rate data found for " + provider.getName()))
            .getTimestamp();
    }
}

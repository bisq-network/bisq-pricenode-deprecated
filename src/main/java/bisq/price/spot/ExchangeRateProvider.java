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

import bisq.price.util.Environment;

import java.io.IOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public interface ExchangeRateProvider {

    Map<String, ExchangeRate> request() throws IOException;

    void configure(Environment env);

    String getName();

    String getPrefix();

    /**
     * Load, configure and return a list of {@link ExchangeRateProvider} implementations
     * ordered according to their position in the {@code META-INF/services}
     * provider-configuration file.
     * <p>
     * Ordering is significant because more than one provider may return exchange rate
     * data about a given currency, and it is important that precedence is predictable.
     * Wherever this list is iterated through, it is assumed that the last provider wins.
     * For example, if {@link bisq.price.spot.providers.CoinMarketCap} is ordered before
     * {@link bisq.price.spot.providers.Poloniex}, and both providers return exchange rate
     * data about Litecoin (LTC), then the Poloniex data will overwrite the CoinMarketCap
     * data.
     *
     * @param env for configuring each provider, e.g. for accessing environment variables
     * @see ServiceLoader
     */
    static List<ExchangeRateProvider> loadAll(Environment env) {
        List<ExchangeRateProvider> providers = new ArrayList<>();

        for (ExchangeRateProvider provider : ServiceLoader.load(ExchangeRateProvider.class)) {
            provider.configure(env);
            providers.add(provider);
        }

        return providers;
    }
}

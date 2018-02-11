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

package bisq.price.spot.support;

import bisq.price.spot.ExchangeRate;

import java.time.Duration;

import java.io.IOException;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public abstract class CachingExchangeRateProvider extends AbstractExchangeRateProvider {

    private Set<ExchangeRate> cachedExchangeRates;

    private final Duration ttl;

    public CachingExchangeRateProvider(String name, String prefix, Duration ttl) {
        super(name, prefix);
        this.ttl = ttl;
        log.info("will refresh exchange rate data every {}", ttl);
    }

    @Override
    public final Set<ExchangeRate> doGet() {
        return cachedExchangeRates;
    }

    public final void start() throws IOException {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    getAndCache();
                } catch (IOException e) {
                    log.warn(e.toString());
                    e.printStackTrace();
                }
            }
        }, ttl.toMillis(), ttl.toMillis());

        getAndCache();
    }

    private void getAndCache() throws IOException {
        long ts = System.currentTimeMillis();

        cachedExchangeRates = doGetForCache();

        cachedExchangeRates.stream()
            .filter(e -> "USD".equals(e.getCurrency()) || "LTC".equals(e.getCurrency()))
            .forEach(e -> log.info("BTC/{}: {}", e.getCurrency(), e.getPrice()));

        log.info("getAndCache took {} ms.", (System.currentTimeMillis() - ts));
    }

    protected abstract Set<ExchangeRate> doGetForCache() throws IOException;
}

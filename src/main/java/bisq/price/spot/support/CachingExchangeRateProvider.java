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

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public abstract class CachingExchangeRateProvider extends AbstractExchangeRateProvider {

    private Set<ExchangeRate> cachedExchangeRates;

    private final Duration ttl;
    private final Timer timer = new Timer();

    public CachingExchangeRateProvider(String name, String prefix, Duration ttl) {
        super(name, prefix);
        this.ttl = ttl;
        log.info("will refresh exchange rate data every {}", ttl);
    }

    @Override
    public final Set<ExchangeRate> get() {
        return cachedExchangeRates;
    }

    public final void start() {
        getAndCache();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    getAndCache();
                } catch (Throwable t) {
                    log.warn("scheduled call to getAndCache failed", t);
                }
            }
        }, ttl.toMillis(), ttl.toMillis());
    }

    public void stop() {
        timer.cancel();
    }

    private void getAndCache() {
        long ts = System.currentTimeMillis();

        cachedExchangeRates = doGetForCache();

        cachedExchangeRates.stream()
            .filter(e -> "USD".equals(e.getCurrency()) || "LTC".equals(e.getCurrency()))
            .forEach(e -> log.info("BTC/{}: {}", e.getCurrency(), e.getPrice()));

        log.info("getAndCache took {} ms.", (System.currentTimeMillis() - ts));
    }

    protected abstract Set<ExchangeRate> doGetForCache();
}

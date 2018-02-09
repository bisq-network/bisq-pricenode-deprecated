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

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public abstract class CachingExchangeRateProvider extends AbstractExchangeRateProvider {

    protected Map<String, ExchangeRate> data;

    private final Duration ttl;

    public CachingExchangeRateProvider(String symbol, String metadataPrefix, Duration ttl) {
        super(symbol, metadataPrefix);
        this.ttl = ttl;
        log.info("will refresh exchange rate data every {}", ttl);
    }

    public final void start() throws IOException {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    requestAndCache();
                } catch (IOException e) {
                    log.warn(e.toString());
                    e.printStackTrace();
                }
            }
        }, ttl.toMillis(), ttl.toMillis());

        requestAndCache();
    }

    public void requestAndCache() throws IOException {
        long ts = System.currentTimeMillis();

        data = doRequestForCaching();

        if (data.get("USD") != null) {
            log.info("BTC/USD: {}", data.get("USD").getPrice());
        }
        if (data.get("LTC") != null) {
            log.info("LTC/BTC: {}", data.get("LTC").getPrice());
        }

        log.info("requestAndCache took {} ms.", (System.currentTimeMillis() - ts));
    }

    @Override
    public final Map<String, ExchangeRate> doRequest() {
        return data;
    }

    protected abstract Map<String, ExchangeRate> doRequestForCaching() throws IOException;
}

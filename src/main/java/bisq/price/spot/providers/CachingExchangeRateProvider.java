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

import java.io.IOException;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public abstract class CachingExchangeRateProvider extends AbstractExchangeRateProvider {

    private final Timer timer = new Timer();
    private final long requestIntervalMs;

    protected Map<String, ExchangeRateData> data;

    public CachingExchangeRateProvider(String symbol, String metadataPrefix, long requestIntervalMs, int order) {
        super(symbol, metadataPrefix, order);
        this.requestIntervalMs = requestIntervalMs;
    }

    public final void start() throws IOException {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    requestAndCache();
                } catch (IOException e) {
                    log.warn(e.toString());
                    e.printStackTrace();
                }
            }
        }, requestIntervalMs, requestIntervalMs);

        requestAndCache();
    }

    public void requestAndCache() throws IOException {
        long ts = System.currentTimeMillis();

        data = doRequest();

        if (data.get("USD") != null) {
            log.info("BTC/USD: {}", data.get("USD").getPrice());
        }
        if (data.get("LTC") != null) {
            log.info("LTC/BTC: {}", data.get("LTC").getPrice());
        }

        log.info("requestAndCache took {} ms.", (System.currentTimeMillis() - ts));
    }

    protected abstract Map<String, ExchangeRateData> doRequest() throws IOException;

    @Override
    public final Map<String, ExchangeRateData> request() {
        return data;
    }
}

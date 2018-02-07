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
import bisq.price.spot.ExchangeRateProvider;
import bisq.price.util.Environment;

import java.io.IOException;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractExchangeRateProvider implements ExchangeRateProvider {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Timer timer = new Timer();

    private final String symbol;
    private final String metadataPrefix;
    private final long requestIntervalMs;
    private final int order;

    protected Map<String, ExchangeRateData> data;

    public AbstractExchangeRateProvider(String symbol,
                                        String metadataPrefix,
                                        long requestIntervalMs,
                                        int order) {
        this.symbol = symbol;
        this.metadataPrefix = metadataPrefix;
        this.requestIntervalMs = requestIntervalMs;
        this.order = order;
    }

    @Override
    public void configure(Environment env) {
        // no-op at this level; subclasses can override as necessary.
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

    protected void requestAndCache() throws IOException {
        long ts = System.currentTimeMillis();

        data = request();

        if (data.get("USD") != null) {
            log.info("BTC/USD: {}", data.get("USD").getPrice());
        }
        if (data.get("LTC") != null) {
            log.info("LTC/BTC: {}", data.get("LTC").getPrice());
        }

        log.info("requestAndCache took {} ms.", (System.currentTimeMillis() - ts));
    }

    @Override
    public Map<String, ExchangeRateData> getData() {
        return data;
    }

    @Override
    public String getProviderSymbol() {
        return symbol;
    }

    @Override
    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    @Override
    public int getOrder() {
        return order;
    }
}

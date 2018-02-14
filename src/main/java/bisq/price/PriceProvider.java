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

package bisq.price;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.time.Duration;

import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PriceProvider<T> implements Supplier<T> {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private final Timer timer = new Timer();

    protected final Duration ttl;

    private T cachedResult;

    public PriceProvider(Duration ttl) {
        this.ttl = ttl;
        log.info("will refresh every {}", ttl);
    }

    @Override
    public final T get() {
        return cachedResult;
    }

    @PostConstruct
    public final void start() {
        refresh();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    refresh();
                } catch (Throwable t) {
                    log.warn("refresh failed", t);
                }
            }
        }, ttl.toMillis(), ttl.toMillis());
    }

    @PreDestroy
    public void stop() {
        timer.cancel();
    }

    private void refresh() {
        long ts = System.currentTimeMillis();

        cachedResult = doGet();

        log.info("refresh took {} ms.", (System.currentTimeMillis() - ts));

        onRefresh();
    }

    protected abstract T doGet();

    protected void onRefresh() {
    }
}
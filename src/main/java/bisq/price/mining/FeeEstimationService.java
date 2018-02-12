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

package bisq.price.mining;

import bisq.price.util.Environment;

import java.time.Instant;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeeEstimationService {

    private static final Logger log = LoggerFactory.getLogger(FeeEstimationService.class);

    public static int DEFAULT_REQUEST_INTERVAL_MS = 300_000; // 5 mins

    public static final long BTC_MIN_TX_FEE = 10; // satoshi/byte
    public static final long BTC_MAX_TX_FEE = 1000;

    private final Timer timer = new Timer();
    private final FeeEstimationProvider feeEstimationProvider;
    private final Map<String, Long> dataMap = new ConcurrentHashMap<>();

    private long requestIntervalMs = DEFAULT_REQUEST_INTERVAL_MS;
    private long bitcoinFeesTs;

    public FeeEstimationService(FeeEstimationProvider feeEstimationProvider) {
        this.feeEstimationProvider = feeEstimationProvider;

        // For now we don't need a fee estimation for LTC so we set it fixed, but we keep it in the provider to
        // be flexible if fee pressure grows on LTC
        dataMap.put("ltcTxFee", 500L /*FeeService.LTC_DEFAULT_TX_FEE*/);
        dataMap.put("dogeTxFee", 5_000_000L /*FeeService.DOGE_DEFAULT_TX_FEE*/);
        dataMap.put("dashTxFee", 50L /*FeeService.DASH_DEFAULT_TX_FEE*/);
    }

    public void configure(Environment env) {
        String[] args = env.getArgs();

        if (args.length >= 3) {
            setRequestIntervalMs(TimeUnit.MINUTES.toMillis(Long.valueOf(args[2])));
        }
    }

    public FeeEstimationProvider getFeeEstimationProvider() {
        return feeEstimationProvider;
    }

    public long getRequestIntervalMs() {
        return requestIntervalMs;
    }

    public void setRequestIntervalMs(long requestIntervalMs) {
        this.requestIntervalMs = requestIntervalMs;
    }

    public void start() {
        requestBitcoinFees();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    requestBitcoinFees();
                } catch (Throwable t) {
                    log.warn("scheduled call to requestBitcoinFees failed", t);
                }
            }
        }, requestIntervalMs, requestIntervalMs);
    }

    public void stop() {
        timer.cancel();
    }

    private void requestBitcoinFees() {
        long ts = System.currentTimeMillis();
        long btcFee = feeEstimationProvider.getFee();
        log.info("requestBitcoinFees took {} ms.", (System.currentTimeMillis() - ts));
        if (btcFee < FeeEstimationService.BTC_MIN_TX_FEE) {
            log.warn("Response for fee is lower as min fee. Fee=" + btcFee);
        } else if (btcFee > FeeEstimationService.BTC_MAX_TX_FEE) {
            log.warn("Response for fee is larger as max fee. Fee=" + btcFee);
        } else {
            bitcoinFeesTs = Instant.now().getEpochSecond();
            dataMap.put("btcTxFee", btcFee);
        }
    }

    public Map<String, Object> getFees() {
        Map<String, Object> map = new HashMap<>();
        map.put("bitcoinFeesTs", bitcoinFeesTs);
        map.put("dataMap", dataMap);
        return map;
    }

}

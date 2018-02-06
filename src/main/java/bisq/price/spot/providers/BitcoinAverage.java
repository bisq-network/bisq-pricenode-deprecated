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
import bisq.price.spot.ExchangeRateService;
import bisq.price.util.Environment;

import io.bisq.network.http.HttpClient;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.time.Instant;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BitcoinAverage implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(BitcoinAverage.class);

    protected final HttpClient httpClient;

    private String pubKey;
    private SecretKey secretKey;
    private boolean configured = false;

    public BitcoinAverage() {
        this.httpClient = new HttpClient("https://apiv2.bitcoinaverage.com/");
    }

    @Override
    public void configure(Environment env) {
        configure(
                env.getRequiredVar("BITCOIN_AVG_PUBKEY"),
                env.getRequiredVar("BITCOIN_AVG_PRIVKEY")
        );
    }

    public void configure(String pubKey, String privKey) {
        this.pubKey = pubKey;
        this.secretKey = new SecretKeySpec(privKey.getBytes(), "HmacSHA256");
        this.configured = true;
    }

    protected String getHeader() throws IOException {
        assertConfigured();

        try {
            String payload = Instant.now().getEpochSecond() + "." + pubKey;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(secretKey);
            return payload + "." + Hex.toHexString(mac.doFinal(payload.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IOException(e);
        }
    }

    protected Map<String, ExchangeRateData> getMap(String json, String provider) {
        assertConfigured();

        Map<String, ExchangeRateData> marketPriceMap = new HashMap<>();
        LinkedTreeMap<String, Object> treeMap = new Gson().<LinkedTreeMap<String, Object>>fromJson(json, LinkedTreeMap.class);
        long ts = Instant.now().getEpochSecond();
        treeMap.entrySet().stream().forEach(e -> {
            Object value = e.getValue();
            // We need to check the type as we get an unexpected "timestamp" object at the end:
            if (value instanceof LinkedTreeMap) {
                //noinspection unchecked
                LinkedTreeMap<String, Object> data = (LinkedTreeMap) value;
                String currencyCode = e.getKey().substring(3);
                // We ignore venezuelan currency as the official exchange rate is wishful thinking only....
                // We should use that api with a custom provider: http://api.bitcoinvenezuela.com/1
                if (!("VEF".equals(currencyCode))) {
                    try {
                        final Object lastAsObject = data.get("last");
                        double last = 0;
                        if (lastAsObject instanceof String)
                            last = Double.valueOf((String) lastAsObject);
                        else if (lastAsObject instanceof Double)
                            last = (double) lastAsObject;
                        else
                            log.warn("Unexpected data type: lastAsObject=" + lastAsObject);

                        marketPriceMap.put(currencyCode,
                                new ExchangeRateData(currencyCode, last, ts, provider));
                    } catch (Throwable exception) {
                        log.error("Error converting btcaverage data: " + currencyCode, exception);
                    }
                }
            }
        });
        return marketPriceMap;
    }

    private void assertConfigured() {
        if (!configured)
            throw new IllegalStateException("'configure' method was not called");
    }


    public static class Global extends BitcoinAverage {

        @Override
        public Map<String, ExchangeRateData> request() throws IOException {
            return getMap(
                    httpClient.requestWithGETNoProxy("indices/global/ticker/all?crypto=BTC", "X-signature", getHeader()),
                    ExchangeRateService.BTCAVERAGE_GLOBAL_PROVIDER);
        }
    }


    public static class Local extends BitcoinAverage {

        private static final String PROVIDER_SYMBOL = "BTCA_L";
        private static final long REQUEST_INTERVAL_MS = 90_000;      // 90 sec; 29760 requests for 31 days

        private final Timer timer = new Timer();

        private long timestamp;
        private long count;
        private Map<String, ExchangeRateData> data;

        //@Override // FIXME
        public void start() throws Exception {

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    try {
                        requestAndCache();
                    } catch (Throwable e) {
                        log.warn(e.toString());
                        e.printStackTrace();
                    }
                }
            }, REQUEST_INTERVAL_MS, REQUEST_INTERVAL_MS);

            requestAndCache();
        }

        private void requestAndCache() throws IOException {
            long ts = System.currentTimeMillis();
            data = request();

            if (data.get("USD") != null)
                log.info("BTCAverage local USD (last):" + data.get("USD").getPrice());
            log.info("requestAndCache took {} ms.", (System.currentTimeMillis() - ts));

            // removeOutdatedPrices(allPricesMap); // FIXME
            // allPricesMap.putAll(data);
            timestamp = Instant.now().getEpochSecond();
            count = data.size();
            // writeToJson();
        }


        @Override
        public Map<String, ExchangeRateData> request() throws IOException {
            return getMap(
                    httpClient.requestWithGETNoProxy("indices/local/ticker/all?crypto=BTC", "X-signature", getHeader()),
                    PROVIDER_SYMBOL);
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getCount() {
            return count;
        }

        public Map<? extends String, ? extends ExchangeRateData> getData() {
            return data;
        }
    }
}

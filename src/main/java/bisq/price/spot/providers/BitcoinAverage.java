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

public abstract class BitcoinAverage extends CachingExchangeRateProvider {

    protected final HttpClient httpClient;

    private String pubKey;
    private SecretKey secretKey;
    private boolean configured = false;

    public BitcoinAverage(String symbol,
                          String metadataPrefix,
                          long requestIntervalMs,
                          int order) {
        super(
                symbol,
                metadataPrefix,
                requestIntervalMs,
                order
        );
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

        public Global() {
            // We adjust the request interval to fit into BitcoinAverage developer plan (45k request per month).
            // We get 42514 (29760+12754) request with below numbers. See Local numbers as well.
            super(
                    "BTCA_G",
                    "btcAverageG",
                    210_000, // 3.5 min; 12754 req/mo;
                    1
            );
        }

        @Override
        public Map<String, ExchangeRateData> doRequest() throws IOException {
            return getMap(
                    httpClient.requestWithGETNoProxy("indices/global/ticker/all?crypto=BTC", "X-signature", getHeader()),
                    getProviderSymbol());
        }
    }


    public static class Local extends BitcoinAverage {

        public Local() {
            // We adjust the request interval to fit into BitcoinAverage developer plan (45k request per month).
            // We get 42514 (29760+12754) request with below numbers. See Global numbers as well.
            super(
                    "BTCA_L",
                    "btcAverageL",
                    90_000, // 90 sec; 29760 requests per month
                    2
            );
        }

        @Override
        public Map<String, ExchangeRateData> doRequest() throws IOException {
            return getMap(
                    httpClient.requestWithGETNoProxy("indices/local/ticker/all?crypto=BTC", "X-signature", getHeader()),
                    getProviderSymbol());
        }
    }
}

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
import bisq.price.spot.support.CachingExchangeRateProvider;
import bisq.price.util.Environment;

import io.bisq.network.http.HttpClient;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.time.Duration;
import java.time.Instant;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

/**
 * See the BitcoinAverage API documentation at https://apiv2.bitcoinaverage.com/#ticker-data-all
 */
public abstract class BitcoinAverage extends CachingExchangeRateProvider {

    /**
     * Max number of requests allowed per month on the BitcoinAverage developer plan.
     * Note the actual max value is 45,000; we use the more conservative value below to
     * ensure we do not exceed it. See https://bitcoinaverage.com/en/plans.
     */
    private static final double MAX_REQUESTS_PER_MONTH = 42_514;

    private final HttpClient httpClient = new HttpClient("https://apiv2.bitcoinaverage.com/");
    private final String uriPath;

    private String pubKey;
    private String privKey;

    public BitcoinAverage(String symbol,
                          String metadataPrefix,
                          double pctMaxRequests,
                          String uriPath) {
        super(
                symbol,
                metadataPrefix,
                ttlFor(pctMaxRequests)
        );

        this.uriPath = uriPath;
    }

    private static Duration ttlFor(double pctMaxRequests) {
        long requestsPerMonth = (long) (MAX_REQUESTS_PER_MONTH * pctMaxRequests);
        return Duration.ofDays(31).dividedBy(requestsPerMonth);
    }

    @Override
    public void doConfigure(Environment env) {
        this.pubKey = env.getRequiredVar("BITCOIN_AVG_PUBKEY");
        this.privKey = env.getRequiredVar("BITCOIN_AVG_PRIVKEY");
    }

    @Override
    public Map<String, ExchangeRateData> doRequestForCaching() throws IOException {
        return getExchangeRatesFrom(getAllTickerData());
    }

    /**
     * Returns original JSON data converted to a nested map structure
     */
    private LinkedTreeMap<String, Object> getAllTickerData() throws IOException {
        String json = getAllTickerDataAsJson();
        return new Gson().<LinkedTreeMap<String, Object>>fromJson(json, LinkedTreeMap.class);
    }

    /**
     * Returns raw JSON string from request to BitcoinAverage API. For an example, run:
     * curl -H "X-testing: testing" https://apiv2.bitcoinaverage.com/indices/global/ticker/all?crypto=BTC
     */
    private String getAllTickerDataAsJson() throws IOException {
        return httpClient.requestWithGETNoProxy(uriPath, "X-signature", getHeader());
    }

    private Map<String, ExchangeRateData> getExchangeRatesFrom(Map<String, Object> allTickerData) {

        Map<String, ExchangeRateData> allExchangeRates = new HashMap<>();

        long timestamp = getTimestampFromAllTickerData(allTickerData);

        allTickerData.forEach((tickerSymbol, tickerData) -> {
            // where `tickerSymbol` is like 'BTCUSD', 'BTCEUR'
            // and `tickerData` is a map of all info about that symbol unless
            if (!(tickerData instanceof LinkedTreeMap)) {
                // in which case we short circuit to skip a non-map "timestamp" object at the end
                return;
            }

            // strip leading 'BTC' from the ticker symbol
            String currencyCode = tickerSymbol.substring(3); // leaving 'USD', 'EUR' currency code

            if ("VEF".equals(currencyCode)) {
                // ignore Venezuelan currency as the "official" exchange rate is just wishful thinking
                // we should use this api with a custom provider instead: http://api.bitcoinvenezuela.com/1
                return;
            }

            // find the last price for this currency code
            double lastPrice = lastPriceFrom((LinkedTreeMap) tickerData);

            // and populate our own map with, e.g. {'USD' => ExchangeRateData} entries
            allExchangeRates.put(
                    currencyCode,
                    new ExchangeRateData(currencyCode, lastPrice, timestamp, getProviderSymbol()));
        });

        return allExchangeRates;
    }

    private Long getTimestampFromAllTickerData(Map<String, Object> allTickerData) {
        return allTickerData.entrySet().stream()
                .filter(e -> "timestamp".equals(e.getKey()))
                .map(e -> Long.valueOf((String)e.getValue()))
                .findFirst()
                .orElse(Instant.now().getEpochSecond());
    }

    private double lastPriceFrom(LinkedTreeMap<String, Object> tickerData) {
        Object lastPrice = tickerData.get("last");

        if (lastPrice instanceof String)
            return Double.valueOf((String) lastPrice);

        if (lastPrice instanceof Double)
            return (double) lastPrice;

        log.warn("Unexpected type for lastPrice: {}", lastPrice);
        return 0;
    }

    protected String getHeader() throws IOException {
        String algorithm = "HmacSHA256";
        SecretKey secretKey = new SecretKeySpec(privKey.getBytes(), algorithm);

        try {
            String payload = Instant.now().getEpochSecond() + "." + pubKey;
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKey);
            return payload + "." + Hex.toHexString(mac.doFinal(payload.getBytes()));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IOException(e);
        }
    }


    public static class Global extends BitcoinAverage {
        public Global() {
            super("BTCA_G", "btcAverageG", 0.3, "indices/global/ticker/all?crypto=BTC");
        }
    }


    public static class Local extends BitcoinAverage {
        public Local() {
            super("BTCA_L", "btcAverageL", 0.7, "indices/local/ticker/all?crypto=BTC");
        }
    }
}

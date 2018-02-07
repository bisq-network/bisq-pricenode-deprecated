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

import org.knowm.xchange.bitcoinaverage.dto.marketdata.BitcoinAverageTicker;
import org.knowm.xchange.bitcoinaverage.dto.marketdata.BitcoinAverageTickers;

import com.fasterxml.jackson.databind.ObjectMapper;

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

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = new HttpClient("https://apiv2.bitcoinaverage.com/");
    private final String symbolSet;

    private String pubKey;
    private String privKey;

    /**
     * @param symbolSet "global" or "local"; see https://apiv2.bitcoinaverage.com/#supported-currencies
     */
    public BitcoinAverage(String symbol, String metadataPrefix, double pctMaxRequests, String symbolSet) {
        super(symbol, metadataPrefix, ttlFor(pctMaxRequests));
        this.symbolSet = symbolSet;
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
        Map<String, ExchangeRateData> allExchangeRates = new HashMap<>();

        getTickers().forEach((symbol, ticker) -> {

            // strip leading 'BTC' from the ticker symbol
            String currencyCode = symbol.substring(3); // leaving 'USD', 'EUR' currency code

            if ("VEF".equals(currencyCode)) {
                // ignore Venezuelan currency as the "official" exchange rate is just wishful thinking
                // we should use this api with a custom provider instead: http://api.bitcoinvenezuela.com/1
                return;
            }

            double lastPrice = ticker.getLast().doubleValue();
            long timestamp = ticker.getTimestamp().getTime();

            // and populate our own map with, e.g. {'USD' => ExchangeRateData} entries
            allExchangeRates.put(
                    currencyCode,
                    new ExchangeRateData(currencyCode, lastPrice, timestamp, getProviderSymbol()));
        });

        return allExchangeRates;
    }

    private Map<String, BitcoinAverageTicker> getTickers() throws IOException {
        String path = String.format("indices/%s/ticker/all?crypto=BTC", symbolSet);
        String json = httpClient.requestWithGETNoProxy(path, "X-signature", getHeader());
        BitcoinAverageTickers value = mapper.readValue(json, BitcoinAverageTickers.class);
        return value.getTickers();
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
            super("BTCA_G", "btcAverageG", 0.3, "global");
        }
    }


    public static class Local extends BitcoinAverage {
        public Local() {
            super("BTCA_L", "btcAverageL", 0.7, "local");
        }
    }
}

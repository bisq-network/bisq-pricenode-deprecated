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

import bisq.price.spot.ExchangeRate;
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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
    private Mac mac;

    /**
     * @param symbolSet "global" or "local"; see https://apiv2.bitcoinaverage.com/#supported-currencies
     */
    public BitcoinAverage(String symbol, String metadataPrefix, double pctMaxRequests, String symbolSet) {
        super(symbol, metadataPrefix, ttlFor(pctMaxRequests));
        this.symbolSet = symbolSet;
    }

    @Override
    public void doConfigure(Environment env) {
        this.pubKey = env.getRequiredVar("BITCOIN_AVG_PUBKEY");
        this.mac = initMac(env.getRequiredVar("BITCOIN_AVG_PRIVKEY"));
    }

    @Override
    public Set<ExchangeRate> doRequestForCaching() throws IOException {

        return getTickers().entrySet().stream()
            .filter(e -> supportedCurrency(e.getKey()))
            .map(e ->
                new ExchangeRate(
                    e.getKey(),
                    e.getValue().getLast(),
                    e.getValue().getTimestamp(),
                    this.getName()
                )
            )
            .collect(Collectors.toSet());
    }

    private boolean supportedCurrency(String currencyCode) {
        // ignore Venezuelan bolivars as the "official" exchange rate is just wishful thinking
        // we should use this API with a custom provider instead: http://api.bitcoinvenezuela.com/1
        return !"VEF".equals(currencyCode);
    }

    private Map<String, BitcoinAverageTicker> getTickers() throws IOException {
        String path = String.format("indices/%s/ticker/all?crypto=BTC", symbolSet);
        String json = httpClient.requestWithGETNoProxy(path, "X-signature", getAuthSignature());
        BitcoinAverageTickers value = mapper.readValue(json, BitcoinAverageTickers.class);
        return rekey(value.getTickers());
    }

    private Map<String, BitcoinAverageTicker> rekey(Map<String, BitcoinAverageTicker> tickers) {
        // go from a map with keys like "BTCUSD", "BTCVEF" to one with keys like "USD", "VEF"
        return tickers.entrySet().stream()
            .collect(Collectors.toMap(e -> e.getKey().substring(3), Map.Entry::getValue));
    }

    protected String getAuthSignature() {
        String payload = String.format("%s.%s", Instant.now().getEpochSecond(), pubKey);
        return String.format("%s.%s", payload, Hex.toHexString(mac.doFinal(payload.getBytes())));
    }

    private static Mac initMac(String privKey) {
        String algorithm = "HmacSHA256";
        SecretKey secretKey = new SecretKeySpec(privKey.getBytes(), algorithm);
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKey);
            return mac;
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static Duration ttlFor(double pctMaxRequests) {
        long requestsPerMonth = (long) (MAX_REQUESTS_PER_MONTH * pctMaxRequests);
        return Duration.ofDays(31).dividedBy(requestsPerMonth);
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

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

package io.bisq.provider;

import io.bisq.provider.fee.FeeRequestService;
import io.bisq.provider.price.PriceRequestService;

import io.bisq.network.http.HttpException;

import io.bisq.common.app.Log;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

import static spark.Spark.get;
import static spark.Spark.port;

public class ProviderMain {

    private static final Logger log = LoggerFactory.getLogger(ProviderMain.class);

    public ProviderMain() {
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, InvalidKeyException, HttpException {
        Log.setup(System.getProperty("user.home") + File.separator + "provider");
        Log.setLevel(Level.INFO);

        port(System.getenv("PORT") != null ? Integer.valueOf(System.getenv("PORT")) : 8080);

        handleGetAllMarketPrices(args);
        handleGetFees();
    }

    private static void handleGetAllMarketPrices(String[] args) throws IOException, NoSuchAlgorithmException, InvalidKeyException {
        if (System.getenv("BITCOIN_AVG_PRIVKEY") != null && System.getenv("BITCOIN_AVG_PUBKEY") != null) {
            String bitcoinAveragePrivKey = System.getenv("BITCOIN_AVG_PRIVKEY");
            String bitcoinAveragePubKey = System.getenv("BITCOIN_AVG_PUBKEY");

            PriceRequestService priceRequestService =
                    new PriceRequestService(bitcoinAveragePrivKey, bitcoinAveragePubKey);

            get("/getAllMarketPrices", (req, res) -> {
                log.info("Incoming getAllMarketPrices request from: " + req.userAgent());
                return priceRequestService.getJson();
            });
        } else {
            throw new IllegalArgumentException("You need to provide the BitcoinAverage API keys. " +
                    "Private key as BITCOIN_AVG_PRIVKEY environment variable, " +
                    "public key as BITCOIN_AVG_PUBKEY environment variable");
        }
    }

    private static void handleGetFees() throws IOException {
        FeeRequestService feeRequestService = new FeeRequestService();
        get("/getFees", (req, res) -> {
            log.info("Incoming getFees request from: " + req.userAgent());
            return feeRequestService.getJson();
        });
    }
}

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

package bisq.pricenode.app;

import bisq.pricenode.fee.FeeRequestService;
import bisq.pricenode.fee.providers.BtcFeesProvider;

import io.bisq.common.app.Log;
import io.bisq.common.app.Version;
import io.bisq.common.util.Utilities;

import java.io.File;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;


public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final String VERSION = loadVersionFromJarManifest(Main.class);
    private static final int DEFAULT_PORT = 8080;

    static {
        // Need to set default locale initially otherwise we get problems at non-english OS
        Locale.setDefault(new Locale("en", Locale.getDefault().getCountry()));

        Utilities.removeCryptographyRestrictions();
    }

    public static void main(String[] args) throws Exception {
        initLog();

        Pricenode.Config config = new Pricenode.Config();

        config.capacity = BtcFeesProvider.CAPACITY;
        config.maxBlocks = BtcFeesProvider.MAX_BLOCKS;
        config.requestIntervalInMs = TimeUnit.MINUTES.toMillis(FeeRequestService.REQUEST_INTERVAL_MIN);

        if (System.getenv("BITCOIN_AVG_PRIVKEY") != null && System.getenv("BITCOIN_AVG_PUBKEY") != null) {
            config.bitcoinAveragePrivKey = System.getenv("BITCOIN_AVG_PRIVKEY");
            config.bitcoinAveragePubKey = System.getenv("BITCOIN_AVG_PUBKEY");
        } else {
            throw new IllegalArgumentException("You need to provide the BitcoinAverage API keys. " +
                    "Private key as BITCOIN_AVG_PRIVKEY environment variable, " +
                    "public key as BITCOIN_AVG_PUBKEY environment variable");
        }

        // extract command line arguments
        if (args.length >= 2) {
            config.capacity = Integer.valueOf(args[0]);
            config.maxBlocks = Integer.valueOf(args[1]);
        }
        if (args.length >= 3) {
            config.requestIntervalInMs = TimeUnit.MINUTES.toMillis(Long.valueOf(args[2]));
        }

        config.port = System.getenv("PORT") != null ? Integer.valueOf(System.getenv("PORT")) : DEFAULT_PORT;

        Pricenode pricenode = new Pricenode(config);
        pricenode.start();
    }

    private static void initLog() {
        final String logPath = System.getProperty("user.home") + File.separator + "provider";
        Log.setup(logPath);
        Log.setLevel(Level.INFO);
        log.info("Log files under: " + logPath);
        log.info("bisq-pricenode version: " + VERSION);
        log.info("bisq-exchange versions{" +
                "VERSION=" + Version.VERSION +
                ", P2P_NETWORK_VERSION=" + Version.P2P_NETWORK_VERSION +
                ", LOCAL_DB_VERSION=" + Version.LOCAL_DB_VERSION +
                ", TRADE_PROTOCOL_VERSION=" + Version.TRADE_PROTOCOL_VERSION +
                ", BASE_CURRENCY_NETWORK=NOT SET" +
                ", getP2PNetworkId()=NOT SET" +
                '}');
        Utilities.printSysInfo();
    }

    /**
     * Returns the value of 'Implementation-Version' from the application JAR's
     * MANIFEST.MF file or the value '0.0.0' if no such value could be found,
     * e.g. when running this application from source within an IDE or if
     * something has gone wrong with the build system instructions that populate
     * the manifest with this value at JAR creation time.
     */
    private static String loadVersionFromJarManifest(Class<?> clazz) {
        String version = clazz.getPackage().getImplementationVersion();
        return version != null ? version : "0.0.0";
    }
}

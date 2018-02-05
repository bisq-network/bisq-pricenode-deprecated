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

package bisq.price.app;

import bisq.price.mining.FeeEstimationService;
import bisq.price.mining.providers.BtcFeesProvider;
import bisq.price.price.PriceRequestService;
import bisq.price.util.Version;

import io.bisq.common.app.Log;
import io.bisq.common.util.Utilities;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.port;

public class Pricenode {

    private static final Logger log = LoggerFactory.getLogger(Pricenode.class);
    private static final int DEFAULT_PORT = 8080;

    private final PriceRequestService priceRequestService;
    private final FeeEstimationService feeEstimationService;
    private final Version version;

    private int port = DEFAULT_PORT;

    public Pricenode(PriceRequestService priceRequestService, FeeEstimationService feeEstimationService) {
        this.priceRequestService = priceRequestService;
        this.feeEstimationService = feeEstimationService;
        this.version = new Version(Pricenode.class);
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        initLogging();
        priceRequestService.start();
        feeEstimationService.start();
        mapRoutesAndStart();
    }

    private void mapRoutesAndStart() {
        port(port);

        before("/*", (req, res) -> log.info("Incoming {} request from: {}", req.pathInfo(), req.userAgent()));

        get("/getAllMarketPrices", (req, res) -> priceRequestService.getJson());
        get("/getFees", (req, res) -> feeEstimationService.getJson());
        get("/getVersion", (req, res) -> version.toString());
        get("/getParams", (req, res) ->
                ((BtcFeesProvider) feeEstimationService.getFeeEstimationProvider()).getCapacity() + ";" +
                ((BtcFeesProvider) feeEstimationService.getFeeEstimationProvider()).getMaxBlocks() + ";" +
                feeEstimationService.getRequestIntervalMs());
    }

    private void initLogging() {
        final String logPath = System.getProperty("user.home") + File.separator + "provider";
        Log.setup(logPath);
        Log.setLevel(Level.INFO);
        log.info("Log files under: " + logPath);
        log.info("bisq-pricenode version: " + version);
        Utilities.printSysInfo();
    }
}

package bisq.pricenode.app;

import bisq.pricenode.fee.FeeRequestService;
import bisq.pricenode.price.PriceRequestService;
import bisq.pricenode.util.Version;

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
    private final FeeRequestService feeRequestService;
    private final Version version;

    private int port = DEFAULT_PORT;

    public Pricenode(PriceRequestService priceRequestService, FeeRequestService feeRequestService) {
        this.priceRequestService = priceRequestService;
        this.feeRequestService = feeRequestService;
        this.version = new Version(Pricenode.class);
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        initLogging();
        priceRequestService.start();
        feeRequestService.start();
        mapRoutesAndStart();
    }

    private void mapRoutesAndStart() {
        port(port);

        before("/*", (req, res) -> log.info("Incoming {} request from: {}", req.pathInfo(), req.userAgent()));

        get("/getAllMarketPrices", (req, res) -> priceRequestService.getJson());
        get("/getFees", (req, res) -> feeRequestService.getJson());
        get("/getVersion", (req, res) -> version.toString());
        get("/getParams", (req, res) ->
                feeRequestService.getBtcFeesProvider().getCapacity() + ";" +
                feeRequestService.getBtcFeesProvider().getMaxBlocks() + ";" +
                feeRequestService.getRequestIntervalMs());
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

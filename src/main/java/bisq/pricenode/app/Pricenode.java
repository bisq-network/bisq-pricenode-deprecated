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

        handleGetAllMarketPrices(priceRequestService);
        handleGetFees(feeRequestService);
        handleGetVersion(version);
        handleGetParams(feeRequestService);
    }

    private static void handleGetAllMarketPrices(PriceRequestService priceRequestService) {
        get("/getAllMarketPrices", (req, res) -> {
            log.info("Incoming getAllMarketPrices request from: " + req.userAgent());
            return priceRequestService.getJson();
        });
    }

    private static void handleGetFees(FeeRequestService feeRequestService) {
        get("/getFees", (req, res) -> {
            log.info("Incoming getFees request from: " + req.userAgent());
            return feeRequestService.getJson();
        });
    }

    private static void handleGetVersion(Version version) {
        get("/getVersion", (req, res) -> {
            log.info("Incoming getVersion request from: " + req.userAgent());
            return version.toString();
        });
    }

    private static void handleGetParams(FeeRequestService feeRequestService) {
        get("/getParams", (req, res) -> {
            log.info("Incoming getParams request from: " + req.userAgent());
            return feeRequestService.getBtcFeesProvider().getCapacity() + ";" +
                    feeRequestService.getBtcFeesProvider().getMaxBlocks() + ";" +
                    feeRequestService.getRequestIntervalMs();
        });
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

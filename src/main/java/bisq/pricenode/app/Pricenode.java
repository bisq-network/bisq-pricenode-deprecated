package bisq.pricenode.app;

import bisq.pricenode.fee.FeeRequestService;
import bisq.pricenode.price.PriceRequestService;

import io.bisq.common.app.Log;
import io.bisq.common.app.Version;
import io.bisq.common.util.Utilities;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

import static spark.Spark.get;
import static spark.Spark.port;

public class Pricenode {

    private static final Logger log = LoggerFactory.getLogger(Pricenode.class);

    private final PriceRequestService priceRequestService;
    private final FeeRequestService feeRequestService;
    private final Config config;
    private final String version;

    public static class Config {
        public int port;
        public String version;
        public int capacity;
        public int maxBlocks;
        public long requestIntervalInMs;
    }

    public Pricenode(PriceRequestService priceRequestService, FeeRequestService feeRequestService, Config config) {
        this.priceRequestService = priceRequestService;
        this.feeRequestService = feeRequestService;
        this.config = config;
        this.version = loadVersionFromJarManifest(Pricenode.class);
    }

    public void start() {
        initLog();

        port(config.port);

        handleGetAllMarketPrices(priceRequestService);
        handleGetFees(feeRequestService);
        handleGetVersion(config.version);
        handleGetParams(config.capacity, config.maxBlocks, config.requestIntervalInMs);
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

    private static void handleGetVersion(String version) {
        get("/getVersion", (req, res) -> {
            log.info("Incoming getVersion request from: " + req.userAgent());
            return version;
        });
    }

    private static void handleGetParams(int capacity, int maxBlocks, long requestIntervalInMs) {
        get("/getParams", (req, res) -> {
            log.info("Incoming getParams request from: " + req.userAgent());
            return capacity + ";" + maxBlocks + ";" + requestIntervalInMs;
        });
    }

    private void initLog() {
        final String logPath = System.getProperty("user.home") + File.separator + "provider";
        Log.setup(logPath);
        Log.setLevel(Level.INFO);
        log.info("Log files under: " + logPath);
        log.info("bisq-pricenode version: " + version);
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

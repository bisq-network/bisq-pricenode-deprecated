package bisq.pricenode.app;

import bisq.pricenode.fee.FeeRequestService;
import bisq.pricenode.price.PriceRequestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.get;
import static spark.Spark.port;

public class Pricenode {

    private static final Logger log = LoggerFactory.getLogger(Pricenode.class);

    private final PriceRequestService priceRequestService;
    private final FeeRequestService feeRequestService;
    private final Config config;

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
    }

    public void start() {
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
}

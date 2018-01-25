package bisq.pricenode.app;

import bisq.pricenode.fee.FeeRequestService;
import bisq.pricenode.price.PriceRequestService;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.get;
import static spark.Spark.port;

public class Pricenode {

    private static final Logger log = LoggerFactory.getLogger(Pricenode.class);

    private final Config config;

    public static class Config {
        public int port;
        public String version;
        public String bitcoinAveragePrivKey;
        public String bitcoinAveragePubKey;
        public int capacity;
        public int maxBlocks;
        public long requestIntervalInMs;
    }

    public Pricenode(Config config) {
        this.config = config;
    }

    public void start() throws Exception {
        port(config.port);

        handleGetAllMarketPrices(config.bitcoinAveragePrivKey, config.bitcoinAveragePubKey);
        handleGetFees(config.capacity, config.maxBlocks, config.requestIntervalInMs);
        handleGetVersion(config.version);
        handleGetParams(config.capacity, config.maxBlocks, config.requestIntervalInMs);
    }

    private static void handleGetAllMarketPrices(String bitcoinAveragePrivKey, String bitcoinAveragePubKey)
            throws Exception {
        PriceRequestService priceRequestService = new PriceRequestService(bitcoinAveragePrivKey, bitcoinAveragePubKey);
        get("/getAllMarketPrices", (req, res) -> {
            log.info("Incoming getAllMarketPrices request from: " + req.userAgent());
            return priceRequestService.getJson();
        });
    }

    private static void handleGetFees(int capacity, int maxBlocks, long requestIntervalInMs) throws IOException {
        FeeRequestService feeRequestService = new FeeRequestService(capacity, maxBlocks, requestIntervalInMs);
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

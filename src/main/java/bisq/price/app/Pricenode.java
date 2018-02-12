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
import bisq.price.mining.providers.BitcoinFees;
import bisq.price.spot.ExchangeRateService;
import bisq.price.util.Environment;
import bisq.price.util.Version;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.port;

public class Pricenode {

    private static final Logger log = LoggerFactory.getLogger(Pricenode.class);
    private static final int DEFAULT_PORT = 8080;
    private static final ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();

    private final ExchangeRateService exchangeRateService;
    private final FeeEstimationService feeEstimationService;
    private final Version version;

    private int port = DEFAULT_PORT;

    public Pricenode(ExchangeRateService exchangeRateService, FeeEstimationService feeEstimationService) {
        this.exchangeRateService = exchangeRateService;
        this.feeEstimationService = feeEstimationService;
        this.version = new Version(Pricenode.class);
    }

    public void configure(Environment env) {
        env.getOptionalVar("PORT").ifPresent(port ->
            setPort(Integer.valueOf(port))
        );
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void start() {
        exchangeRateService.start();
        feeEstimationService.start();
        mapRoutesAndStart();
    }

    public void stop() {
        exchangeRateService.stop();
        feeEstimationService.stop();
    }

    private void mapRoutesAndStart() {
        port(port);

        before("/*", (req, res) -> log.info("Incoming {} request from: {}", req.pathInfo(), req.userAgent()));

        get("/getAllMarketPrices", (req, res) -> toJson(exchangeRateService.getAllMarketPrices()));
        get("/getFees", (req, res) -> toJson(feeEstimationService.getFees()));
        get("/getVersion", (req, res) -> version.toString());
        get("/getParams", (req, res) ->
            String.format("%s;%s;%s",
                ((BitcoinFees) feeEstimationService.getFeeEstimationProvider()).getCapacity(),
                ((BitcoinFees) feeEstimationService.getFeeEstimationProvider()).getMaxBlocks(),
                feeEstimationService.getRequestIntervalMs()
            )
        );
    }

    private static String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException ex) {
            throw new RuntimeException(ex);
        }
    }
}

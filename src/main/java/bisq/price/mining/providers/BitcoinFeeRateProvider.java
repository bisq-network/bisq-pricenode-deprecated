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

package bisq.price.mining.providers;

import bisq.price.PriceController;
import bisq.price.mining.FeeRate;
import bisq.price.mining.FeeRateProvider;

import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.time.Duration;
import java.time.Instant;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;

//TODO consider alternative https://www.bitgo.com/api/v1/tx/fee?numBlocks=3
@Component
class BitcoinFeeRateProvider extends FeeRateProvider {

    private static final long MIN_TX_FEE = 10; // satoshi/byte
    private static final long MAX_TX_FEE = 1000;

    private static final int DEFAULT_CAPACITY = 4; // if we request each 5 min. we take average of last 20 min.
    private static final int DEFAULT_MAX_BLOCKS = 10;
    private static final int DEFAULT_TTL = 5;

    private final RestTemplate restTemplate = new RestTemplate();
    private final LinkedList<Long> fees = new LinkedList<>();

    private final int capacity;
    private final int maxBlocks;

    // other: https://estimatefee.com/n/2
    public BitcoinFeeRateProvider(Environment env) {
        super(Duration.ofMinutes(ttl(env)));
        this.capacity = capacity(env);
        this.maxBlocks = maxBlocks(env);
    }

    protected FeeRate doGet() {
        String response = getFeeJson();

        @SuppressWarnings("unchecked")
        LinkedTreeMap<String, ArrayList<LinkedTreeMap<String, Double>>> treeMap =
            new Gson().fromJson(response, LinkedTreeMap.class);

        final long[] fee = new long[1];
        // we want a fee which is at least in 20 blocks in (21.co estimation seem to be way too high, so we get
        // prob much faster in
        treeMap.entrySet().stream()
            .flatMap(e -> e.getValue().stream())
            .forEach(e -> {
                Double maxDelay = e.get("maxDelay");
                if (maxDelay <= maxBlocks && fee[0] == 0)
                    fee[0] = e.get("maxFee").longValue();
            });
        fee[0] = Math.min(Math.max(fee[0], MIN_TX_FEE), MAX_TX_FEE);

        return new FeeRate("BTC", getAverage(fee[0]), Instant.now().getEpochSecond());
    }

    private String getFeeJson() {
        return restTemplate.exchange(
            RequestEntity
                .get(UriComponentsBuilder
                    // now using /fees/list because /fees/recommended estimates were too high
                    .fromUriString("https://bitcoinfees.earn.com/api/v1/fees/list")
                    .build().toUri())
                .header("User-Agent", "") // required to avoid 403
                .build(),
            String.class).getBody();
    }

    // We take the average of the last 12 calls (every 5 minute) so we smooth extreme values.
    // We observed very radical jumps in the fee estimations, so that should help to avoid that.
    private long getAverage(long newFee) {
        log.info("new fee " + newFee);
        fees.add(newFee);
        long average = ((Double) fees.stream().mapToDouble(e -> e).average().getAsDouble()).longValue();
        log.info("average of last {} calls: {}", fees.size(), average);
        if (fees.size() == capacity)
            fees.removeFirst();

        return average;
    }

    private static Optional<String[]> args(Environment env) {
        return Optional.ofNullable(
            env.getProperty(CommandLinePropertySource.DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME, String[].class));
    }

    private static int capacity(Environment env) {
        return args(env)
            .filter(args -> args.length >= 1)
            .map(args -> Integer.valueOf(args[0]))
            .orElse(DEFAULT_CAPACITY);
    }

    private static int maxBlocks(Environment env) {
        return args(env)
            .filter(args -> args.length >= 2)
            .map(args -> Integer.valueOf(args[1]))
            .orElse(DEFAULT_MAX_BLOCKS);
    }

    private static long ttl(Environment env) {
        return args(env)
            .filter(args -> args.length >= 3)
            .map(args -> Integer.valueOf(args[2]))
            .orElse(DEFAULT_TTL);
    }


    @RestController
    class Controller extends PriceController {

        @GetMapping(path = "/getParams")
        public String getParams() {
            return String.format("%s;%s;%s", capacity, maxBlocks, ttl.toMillis());
        }
    }
}

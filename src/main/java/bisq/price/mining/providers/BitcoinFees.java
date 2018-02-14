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

import bisq.price.mining.FeeRateProvider;

import io.bisq.network.http.HttpClient;

import io.bisq.common.util.MathUtils;

import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.time.Duration;

import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.ArrayList;
import java.util.LinkedList;

//TODO consider alternative https://www.bitgo.com/api/v1/tx/fee?numBlocks=3
@Component
public class BitcoinFees extends FeeRateProvider {

    private static final long MIN_TX_FEE = 10; // satoshi/byte
    private static final long MAX_TX_FEE = 1000;

    private static final int DEFAULT_CAPACITY = 4; // if we request each 5 min. we take average of last 20 min.
    private static final int DEFAULT_MAX_BLOCKS = 10;

    private final HttpClient httpClient;
    private final LinkedList<Long> fees = new LinkedList<>();

    private final int capacity;
    private final int maxBlocks;

    // other: https://estimatefee.com/n/2
    public BitcoinFees(Environment env) {
        super(env);

        this.httpClient = new HttpClient("https://bitcoinfees.earn.com/api/v1/fees/");

        String[] args =
            env.getProperty(CommandLinePropertySource.DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME, String[].class);

        if (args != null && args.length >= 2) {
            this.capacity = Integer.valueOf(args[0]);
            this.maxBlocks = Integer.valueOf(args[1]);
        } else {
            this.capacity = DEFAULT_CAPACITY;
            this.maxBlocks = DEFAULT_MAX_BLOCKS;
        }
    }

    public int getCapacity() {
        return capacity;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public Duration getTtl() {
        return ttl;
    }

    protected Long doGet() {
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
                    fee[0] = MathUtils.roundDoubleToLong(e.get("maxFee"));
            });
        fee[0] = Math.min(Math.max(fee[0], MIN_TX_FEE), MAX_TX_FEE);

        return getAverage(fee[0]);
    }

    private String getFeeJson() {
        try {
            // prev. used:  https://bitcoinfees.earn.com/api/v1/fees/recommended
            // but was way too high

            // https://bitcoinfees.earn.com/api/v1/fees/list
            String response = httpClient.requestWithGET("list", "User-Agent", "");
            log.info("Get recommended fee response:  " + response);
            return response;
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
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
}

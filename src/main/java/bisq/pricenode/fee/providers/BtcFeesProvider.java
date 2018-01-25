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

package bisq.pricenode.fee.providers;

import bisq.pricenode.fee.FeeRequestService;

import io.bisq.network.http.HttpClient;

import io.bisq.common.util.MathUtils;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.IOException;

import java.util.ArrayList;
import java.util.LinkedList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO consider alternative https://www.bitgo.com/api/v1/tx/fee?numBlocks=3
public class BtcFeesProvider {

    private static final Logger log = LoggerFactory.getLogger(BtcFeesProvider.class);

    public static int DEFAULT_CAPACITY = 4; // if we request each 5 min. we take average of last 20 min.
    public static int DEFAULT_MAX_BLOCKS = 10;

    private final HttpClient httpClient;
    private final LinkedList<Long> fees = new LinkedList<>();

    private int capacity = DEFAULT_CAPACITY;
    private int maxBlocks = DEFAULT_MAX_BLOCKS;

    // other: https://estimatefee.com/n/2
    public BtcFeesProvider() {
        this.httpClient = new HttpClient("https://bitcoinfees.earn.com/api/v1/fees/");
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getMaxBlocks() {
        return maxBlocks;
    }

    public void setMaxBlocks(int maxBlocks) {
        this.maxBlocks = maxBlocks;
    }

    public Long getFee() throws IOException {
        // prev. used:  https://bitcoinfees.earn.com/api/v1/fees/recommended
        // but was way too high

        // https://bitcoinfees.earn.com/api/v1/fees/list
        String response = httpClient.requestWithGET("list", "User-Agent", "");
        log.info("Get recommended fee response:  " + response);

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
        fee[0] = Math.min(Math.max(fee[0], FeeRequestService.BTC_MIN_TX_FEE), FeeRequestService.BTC_MAX_TX_FEE);

        return getAverage(fee[0]);
    }

    // We take the average of the last 12 calls (every 5 minute) so we smooth extreme values.
    // We observed very radical jumps in the fee estimations, so that should help to avoid that.
    long getAverage(long newFee) {
        log.info("new fee " + newFee);
        fees.add(newFee);
        long average = ((Double) fees.stream().mapToDouble(e -> e).average().getAsDouble()).longValue();
        log.info("average of last {} calls: {}", fees.size(), average);
        if (fees.size() == capacity)
            fees.removeFirst();

        return average;
    }
}

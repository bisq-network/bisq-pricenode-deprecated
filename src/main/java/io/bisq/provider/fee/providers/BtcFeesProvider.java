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

package io.bisq.provider.fee.providers;

import io.bisq.provider.fee.FeeRequestService;

import io.bisq.network.http.HttpClient;

import io.bisq.common.util.MathUtils;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.IOException;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BtcFeesProvider {

    private static final Logger log = LoggerFactory.getLogger(BtcFeesProvider.class);

    private final HttpClient httpClient;

    public BtcFeesProvider() {
        // we previously used https://bitcoinfees.21.co/api/v1/fees/recommended but fees were way too high
        // see also: https://estimatefee.com/n/2
        this.httpClient = new HttpClient("https://bitcoinfees.21.co/api/v1/fees/");
    }

    public Long getFee() throws IOException {
        String response = httpClient.requestWithGET("list", "User-Agent", "");
        log.info("Get recommended fee response:  " + response);

        LinkedTreeMap<String, ArrayList<LinkedTreeMap<String, Double>>> treeMap = new Gson().fromJson(response, LinkedTreeMap.class);
        final long[] fee = new long[1];

        // we want a fee that gets us in within 10 blocks max
        int maxBlocks = 10;
        treeMap.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .forEach(e -> {
                    Double maxDelay = e.get("maxDelay");
                    if (maxDelay <= maxBlocks && fee[0] == 0)
                        fee[0] = MathUtils.roundDoubleToLong(e.get("maxFee"));
                });
        fee[0] = Math.min(Math.max(fee[0], FeeRequestService.BTC_MIN_TX_FEE), FeeRequestService.BTC_MAX_TX_FEE);
        log.info("fee " + fee[0]);
        return fee[0];
    }
}

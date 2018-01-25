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

package bisq.pricenode.app;

import bisq.pricenode.fee.FeeRequestService;
import bisq.pricenode.fee.providers.BtcFeesProvider;
import bisq.pricenode.price.PriceRequestService;

import io.bisq.common.util.Utilities;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final int DEFAULT_PORT = 8080;

    static {
        // Need to set default locale initially otherwise we get problems at non-english OS
        Locale.setDefault(new Locale("en", Locale.getDefault().getCountry()));

        Utilities.removeCryptographyRestrictions();
    }

    public static void main(String[] args) throws Exception {
        Pricenode.Config config = new Pricenode.Config();

        config.capacity = BtcFeesProvider.CAPACITY;
        config.maxBlocks = BtcFeesProvider.MAX_BLOCKS;
        config.requestIntervalInMs = TimeUnit.MINUTES.toMillis(FeeRequestService.REQUEST_INTERVAL_MIN);

        Environment env = new Environment();

        PriceRequestService priceRequestService =
                new PriceRequestService(
                        env.getRequiredVar("BITCOIN_AVG_PRIVKEY"),
                        env.getRequiredVar("BITCOIN_AVG_PUBKEY"));

        // extract command line arguments
        if (args.length >= 2) {
            config.capacity = Integer.valueOf(args[0]);
            config.maxBlocks = Integer.valueOf(args[1]);
        }
        if (args.length >= 3) {
            config.requestIntervalInMs = TimeUnit.MINUTES.toMillis(Long.valueOf(args[2]));
        }

        FeeRequestService feeRequestService =
                new FeeRequestService(config.capacity, config.maxBlocks, config.requestIntervalInMs);

        config.port = System.getenv("PORT") != null ? Integer.valueOf(System.getenv("PORT")) : DEFAULT_PORT;

        Pricenode pricenode = new Pricenode(priceRequestService, feeRequestService, config);
        pricenode.start();
    }
}

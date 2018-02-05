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
import bisq.price.mining.providers.BtcFeesProvider;
import bisq.price.price.PriceRequestService;
import bisq.price.price.providers.BtcAverageProvider;
import bisq.price.price.providers.CoinmarketcapProvider;
import bisq.price.price.providers.PoloniexProvider;
import bisq.price.util.Environment;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        Environment env = new Environment();

        PriceRequestService priceRequestService = new PriceRequestService(
                new BtcAverageProvider(
                        env.getRequiredVar("BITCOIN_AVG_PRIVKEY"),
                        env.getRequiredVar("BITCOIN_AVG_PUBKEY")),
                new PoloniexProvider(),
                new CoinmarketcapProvider()
        );

        BtcFeesProvider btcFeesProvider = new BtcFeesProvider();
        if (args.length >= 2) {
            btcFeesProvider.setCapacity(Integer.valueOf(args[0]));
            btcFeesProvider.setMaxBlocks(Integer.valueOf(args[1]));
        }

        FeeEstimationService feeEstimationService = new FeeEstimationService(btcFeesProvider);
        if (args.length >= 3) {
            feeEstimationService.setRequestIntervalMs(TimeUnit.MINUTES.toMillis(Long.valueOf(args[2])));
        }

        Pricenode pricenode = new Pricenode(priceRequestService, feeEstimationService);
        env.getOptionalVar("PORT").ifPresent(port ->
                pricenode.setPort(Integer.valueOf(port))
        );

        pricenode.start();
    }
}

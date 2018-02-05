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

import bisq.price.mining.FeeEstimationProvider;
import bisq.price.mining.FeeEstimationService;
import bisq.price.spot.ExchangeRateService;
import bisq.price.spot.providers.BitcoinAverageGlobal;
import bisq.price.spot.providers.BitcoinAverageLocal;
import bisq.price.spot.providers.CoinmarketcapProvider;
import bisq.price.spot.providers.PoloniexProvider;
import bisq.price.util.Environment;

public class Main {

    public static void main(String[] args) throws Exception {
        Environment env = new Environment(args);

        String bitcoinAvgPrivkey = env.getRequiredVar("BITCOIN_AVG_PRIVKEY");
        String bitcoinAvgPubkey = env.getRequiredVar("BITCOIN_AVG_PUBKEY");

        ExchangeRateService exchangeRateService = new ExchangeRateService(
                new BitcoinAverageLocal(bitcoinAvgPrivkey, bitcoinAvgPubkey),
                new BitcoinAverageGlobal(bitcoinAvgPrivkey, bitcoinAvgPubkey),
                new PoloniexProvider(),
                new CoinmarketcapProvider()
        );

        FeeEstimationService feeEstimationService = new FeeEstimationService(FeeEstimationProvider.load(env));
        feeEstimationService.configure(env);

        Pricenode pricenode = new Pricenode(exchangeRateService, feeEstimationService);
        pricenode.configure(env);

        pricenode.start();
    }
}

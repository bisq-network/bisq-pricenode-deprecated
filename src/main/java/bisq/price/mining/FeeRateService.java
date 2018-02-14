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

package bisq.price.mining;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class FeeRateService {

    private final FeeRateProvider feeRateProvider;

    public FeeRateService(FeeRateProvider feeRateProvider) {
        this.feeRateProvider = feeRateProvider;
    }

    public Map<String, Object> getFees() {
        Map<String, Long> dataMap = new HashMap<>();
        FeeRate feeRate = feeRateProvider.get();
        dataMap.put(feeRate.getCurrency().toLowerCase() + "TxFee", feeRate.getPrice());

        // For now we don't need a fee estimation for LTC so we set it fixed, but we keep it in the provider to
        // be flexible if fee pressure grows on LTC
        dataMap.put("ltcTxFee", 500L /*FeeService.LTC_DEFAULT_TX_FEE*/);
        dataMap.put("dogeTxFee", 5_000_000L /*FeeService.DOGE_DEFAULT_TX_FEE*/);
        dataMap.put("dashTxFee", 50L /*FeeService.DASH_DEFAULT_TX_FEE*/);

        Map<String, Object> map = new HashMap<>();
        map.put("dataMap", dataMap);
        map.put(feeRate.getProvider() + "Ts", feeRate.getTimestamp());
        return map;
    }
}

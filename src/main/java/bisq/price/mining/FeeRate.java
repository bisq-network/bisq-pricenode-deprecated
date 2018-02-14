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

/**
 * A value object representing the mining fee rate for a given base currency.
 */
public class FeeRate {

    private final String currency;
    private final long price;
    private final long timestamp;
    private final String provider;

    public FeeRate(String currency, long price, long timestamp, String provider) {
        this.currency = currency;
        this.price = price;
        this.timestamp = timestamp;
        this.provider = provider;
    }

    public String getCurrency() {
        return currency;
    }

    public long getPrice() {
        return this.price;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public String getProvider() {
        return provider;
    }
}

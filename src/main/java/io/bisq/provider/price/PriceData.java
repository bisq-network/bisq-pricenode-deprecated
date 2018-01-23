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

package io.bisq.provider.price;

import java.util.Objects;

public class PriceData {
    private final String currencyCode;
    private final double price;
    private final long timestampSec;
    private final String provider;

    public PriceData(String currencyCode, double price, long timestampSec, String provider) {
        this.currencyCode = currencyCode;
        this.price = price;
        this.timestampSec = timestampSec;
        this.provider = provider;
    }

    double getPrice() {
        return this.price;
    }

    long getTimestampSec() {
        return this.timestampSec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PriceData priceData = (PriceData) o;
        return Double.compare(priceData.price, price) == 0 &&
                timestampSec == priceData.timestampSec &&
                Objects.equals(currencyCode, priceData.currencyCode) &&
                Objects.equals(provider, priceData.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currencyCode, price, timestampSec, provider);
    }

    @Override
    public String toString() {
        return "PriceData{" +
                "currencyCode='" + currencyCode + '\'' +
                ", price=" + price +
                ", timestampSec=" + timestampSec +
                ", provider=" + provider +
                '}';
    }
}

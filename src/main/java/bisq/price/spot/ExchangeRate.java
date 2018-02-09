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

package bisq.price.spot;

import java.util.Objects;

public class ExchangeRate {

    private final String currency;
    private final double price;
    private final long timestamp;
    private final String provider;

    public ExchangeRate(String currency, double price, long timestamp, String provider) {
        this.currency = currency;
        this.price = price;
        this.timestamp = timestamp;
        this.provider = provider;
    }

    public double getPrice() {
        return this.price;
    }

    long getTimestamp() {
        return this.timestamp;
    }

    public String getProvider() {
        return provider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeRate exchangeRate = (ExchangeRate) o;
        return Double.compare(exchangeRate.price, price) == 0 &&
            timestamp == exchangeRate.timestamp &&
            Objects.equals(currency, exchangeRate.currency) &&
            Objects.equals(provider, exchangeRate.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currency, price, timestamp, provider);
    }

    @Override
    public String toString() {
        return "ExchangeRate{" +
            "currency='" + currency + '\'' +
            ", price=" + price +
            ", timestamp=" + timestamp +
            ", provider=" + provider +
            '}';
    }
}

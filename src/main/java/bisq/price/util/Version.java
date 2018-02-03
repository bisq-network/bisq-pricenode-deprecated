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

package bisq.price.util;

import bisq.price.node.Pricenode;

import java.util.Optional;

public class Version {

    public static final String NONE = "0.0.0";

    private final String value;

    public Version(Class<Pricenode> clazz) {
        this(clazz.getPackage());
    }

    public Version(Package pkg) {
        this(Optional.ofNullable(pkg.getImplementationVersion()).orElse(NONE));
    }

    public Version(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}

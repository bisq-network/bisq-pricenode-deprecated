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

import bisq.price.PriceProvider;

import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.Environment;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class FeeEstimationProvider extends PriceProvider<Long> {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    public FeeEstimationProvider(Environment env) {
        super(getTtl(env));
    }

    private static Duration getTtl(Environment env) {
        String[] args =
            env.getProperty(CommandLinePropertySource.DEFAULT_NON_OPTION_ARGS_PROPERTY_NAME, String[].class);

        return (args != null && args.length >= 3) ?
            Duration.ofMinutes(Long.valueOf(args[2])) :
            Duration.ofMinutes(5);
    }
}

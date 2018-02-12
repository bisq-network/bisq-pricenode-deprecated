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

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class VersionController {

    private static final Logger log = LoggerFactory.getLogger(VersionController.class);

    private final Version version;

    public VersionController() {
        this.version = new Version(this.getClass());
    }

    @ModelAttribute
    public void logRequest(HttpServletRequest req) {
        log.info("Incoming {} request from: {}", req.getServletPath(), req.getHeader("User-Agent"));
    }

    @GetMapping(path = "/getVersion")
    public String getVersion() {
        return version.toString();
    }
}

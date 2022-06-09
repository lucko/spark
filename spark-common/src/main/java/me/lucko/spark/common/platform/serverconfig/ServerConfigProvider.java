/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.common.platform.serverconfig;

import com.google.gson.JsonElement;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Function to export server configuration files for access within the spark viewer.
 */
@FunctionalInterface
public interface ServerConfigProvider {

    /**
     * Loads a map of the server configuration files.
     *
     * <p>The key is the name of the file and the value is a
     * {@link JsonElement} of the contents.</p>
     *
     * @return the exported server configurations
     */
    Map<String, JsonElement> loadServerConfigurations();

    default Map<String, String> exportServerConfigurations() {
        return loadServerConfigurations().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toString()
                ));
    }

    /**
     * A no-op implementation
     */
    ServerConfigProvider NO_OP = Collections::emptyMap;

}

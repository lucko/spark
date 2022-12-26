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

package me.lucko.spark.fabric;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import me.lucko.spark.common.platform.serverconfig.ConfigParser;
import me.lucko.spark.common.platform.serverconfig.PropertiesConfigParser;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;

import java.util.Collection;
import java.util.Map;

public class FabricServerConfigProvider extends ServerConfigProvider {

    /** A map of provided files and their type */
    private static final Map<String, ConfigParser> FILES;
    /** A collection of paths to be excluded from the files */
    private static final Collection<String> HIDDEN_PATHS;

    public FabricServerConfigProvider() {
        super(FILES, HIDDEN_PATHS);
    }

    static {
        ImmutableSet.Builder<String> hiddenPaths = ImmutableSet.<String>builder()
                .add("server-ip")
                .add("motd")
                .add("resource-pack")
                .add("rcon<dot>password")
                .add("level-seed")
                .addAll(getSystemPropertyList("spark.serverconfigs.hiddenpaths"));

        FILES = ImmutableMap.of("server.properties", PropertiesConfigParser.INSTANCE);
        HIDDEN_PATHS = hiddenPaths.build();
    }

}

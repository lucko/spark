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

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;

import me.lucko.spark.common.platform.MetadataProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Abstract implementation of {@link MetadataProvider} which
 * provides server configuration data.
 *
 * <p>This implementation is able to delete hidden paths from
 * the configurations before they are sent to the viewer.</p>
 */
public abstract class ServerConfigProvider implements MetadataProvider {
    private final Map<String, ConfigParser> files;
    private final ExcludedConfigFilter hiddenPathFilters;

    protected ServerConfigProvider(Map<String, ConfigParser> files, Collection<String> hiddenPaths) {
        this.files = files;
        this.hiddenPathFilters = new ExcludedConfigFilter(hiddenPaths);
    }

    @Override
    public final Map<String, JsonElement> get() {
        ImmutableMap.Builder<String, JsonElement> builder = ImmutableMap.builder();

        this.files.forEach((path, parser) -> {
            try {
                JsonElement json = parser.load(path, this.hiddenPathFilters);
                if (json == null) {
                    return;
                }
                builder.put(path, json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return builder.build();
    }

    protected static List<String> getSystemPropertyList(String property) {
        String value = System.getProperty(property);
        return value == null
                ? Collections.emptyList()
                : Arrays.asList(value.split(","));
    }

}

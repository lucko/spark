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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExcludedConfigFilterTest {

    @Test
    public void testFilter() {
        Set<String> excluded = ImmutableSet.<String>builder()
                .add("database")
                .add("settings.bungeecord-addresses")
                .add("rcon<dot>password")
                .add("world-settings.*.feature-seeds")
                .add("world-settings.*.seed-*")
                .build();

        ExcludedConfigFilter filter = new ExcludedConfigFilter(excluded);

        JsonPrimitive value = new JsonPrimitive("hello");
        JsonObject before = obj(
                element("hello", value),
                element("database", obj(
                        element("username", value),
                        element("password", value)
                )),
                element("settings", obj(
                        element("hello", value),
                        element("bungeecord-addresses", value)
                )),
                element("rcon.password", value),
                element("world-settings", obj(
                        element("world1", obj(
                                element("hello", value),
                                element("feature-seeds", value),
                                element("seed-test", value)
                        )),
                        element("world2", obj(
                                element("hello", value),
                                element("feature-seeds", value),
                                element("seed-test", value)
                        ))
                ))
        );
        JsonObject after = obj(
                element("hello", value),
                element("settings", obj(
                        element("hello", value)
                )),
                element("world-settings", obj(
                        element("world1", obj(
                                element("hello", value)
                        )),
                        element("world2", obj(
                                element("hello", value)
                        ))
                ))
        );


        assertEquals(after, filter.apply(before));
    }

    @SafeVarargs
    private static JsonObject obj(Map.Entry<String, JsonElement>... elements) {
        JsonObject object = new JsonObject();
        for (Map.Entry<String, JsonElement> element : elements) {
            object.add(element.getKey(), element.getValue());
        }
        return object;
    }

    private static Map.Entry<String, JsonElement> element(String key, JsonElement value) {
        return Maps.immutableEntry(key, value);
    }

}

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
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PropertiesConfigParserTest {

    @Test
    public void testParse() throws IOException {
        String properties =
                "hello=world\n" +
                "a.b.c=1\n" +
                "foo=true\n";

        Map<String, Object> parse = PropertiesConfigParser.INSTANCE.parse(new BufferedReader(new StringReader(properties)));
        assertEquals(ImmutableMap.of(
                "hello", "world",
                "a.b.c", 1L,
                "foo", true
        ), parse);
    }

}

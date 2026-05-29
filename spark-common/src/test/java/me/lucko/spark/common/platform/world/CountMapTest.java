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

package me.lucko.spark.common.platform.world;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CountMapTest {

    @Test
    public void testSimple() {
        CountMap.Simple<String> countMap = new CountMap.Simple<>(new HashMap<>());
        assertTrue(countMap.asMap().isEmpty());

        countMap.increment("test");
        assertTrue(countMap.asMap().containsKey("test"));
        assertEquals(1, countMap.asMap().get("test").get());

        countMap.add("test", 5);
        assertEquals(6, countMap.asMap().get("test").get());

        countMap.increment("test2");

        assertEquals(7, countMap.total().get());
    }

}

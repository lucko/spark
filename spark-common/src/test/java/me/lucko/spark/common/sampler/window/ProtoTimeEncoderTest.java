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

package me.lucko.spark.common.sampler.window;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.LongAdder;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtoTimeEncoderTest {

    @Test
    public void testSimple() {
        ProtoTimeEncoder encoder = new ProtoTimeEncoder(l -> l, IntStream.of(7, 1, 3, 5));
        assertArrayEquals(new int[]{1, 3, 5, 7}, encoder.getKeys());

        assertArrayEquals(new double[]{0, 0, 0, 0}, encoder.encode(ImmutableMap.of()));
        assertArrayEquals(new double[]{0, 100, 0, 0}, encoder.encode(ImmutableMap.of(3, longAdder(100))));
        assertArrayEquals(new double[]{0, 100, 200, 0}, encoder.encode(ImmutableMap.of(3, longAdder(100), 5, longAdder(200))));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> encoder.encode(ImmutableMap.of(9, longAdder(300))));
        assertTrue(ex.getMessage().startsWith("No index for key 9"));
    }

    private static LongAdder longAdder(long l) {
        LongAdder longAdder = new LongAdder();
        longAdder.add(l);
        return longAdder;
    }

}

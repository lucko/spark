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

package me.lucko.spark.common.tick;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TickHookTest {

    @Test
    public void testAbstractHook() {
        AbstractTickHook hook = new AbstractTickHook() {
            @Override
            public void start() {

            }

            @Override
            public void close() {

            }
        };

        assertEquals(0, hook.getCurrentTick());

        List<Integer> ticks = new ArrayList<>();
        TickHook.Callback callback = ticks::add;

        hook.addCallback(callback);

        hook.onTick();
        assertEquals(1, hook.getCurrentTick());
        assertEquals(ImmutableList.of(0), ticks);

        hook.onTick();
        assertEquals(2, hook.getCurrentTick());
        assertEquals(ImmutableList.of(0, 1), ticks);

        hook.removeCallback(callback);

        hook.onTick();
        assertEquals(3, hook.getCurrentTick());
        assertEquals(ImmutableList.of(0, 1), ticks);
    }

}

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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TickReporterTest {

    @Test
    public void testAbstractReporter() {
        AbstractTickReporter reporter = new AbstractTickReporter() {
            @Override
            public void start() {

            }

            @Override
            public void close() {

            }
        };

        List<Double> durations = new ArrayList<>();
        TickReporter.Callback callback = durations::add;

        reporter.addCallback(callback);

        reporter.onTick(1.0);
        assertEquals(ImmutableList.of(1.0), durations);

        reporter.onTick(2.0);
        assertEquals(ImmutableList.of(1.0, 2.0), durations);

        reporter.removeCallback(callback);

        reporter.onTick(3.0);
        assertEquals(ImmutableList.of(1.0, 2.0), durations);
    }

    @Test
    public void testSimpleReporter() {
        SimpleTickReporter reporter = new SimpleTickReporter() {
            @Override
            public void start() {

            }
        };

        List<Double> durations = new ArrayList<>();
        TickReporter.Callback callback = durations::add;

        reporter.addCallback(callback);

        reporter.onStart();
        assertEquals(0, durations.size());

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // ignore
        }

        reporter.onEnd();

        assertEquals(1, durations.size());
        assertTrue(durations.get(0) > 0);
    }

}

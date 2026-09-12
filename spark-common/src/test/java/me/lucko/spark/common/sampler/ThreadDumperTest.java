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

package me.lucko.spark.common.sampler;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ThreadDumperTest {

    @Test
    public void testAll() {
        assertTrue(ThreadDumper.ALL.isThreadIncluded(1, "test"));
        assertTrue(ThreadDumper.ALL.isThreadIncluded(2, "test2"));
    }

    @Test
    public void testSpecific() {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100_000);
            } catch (InterruptedException e) {
                // ignore
            }
        }, "test-thread-1");
        thread.start();

        ThreadDumper.Specific specific = new ThreadDumper.Specific(thread);

        assertTrue(specific.isThreadIncluded(thread.getId(), "test-thread-1"));

        Set<Thread> threads = specific.getThreads();
        assertEquals(1, threads.size());
        assertTrue(threads.contains(thread));

        Set<String> threadNames = specific.getThreadNames();
        assertEquals(1, threadNames.size());
        assertTrue(threadNames.contains("test-thread-1"));

        thread.interrupt();
    }

    @Test
    public void testRegex() {
        ThreadDumper.Regex regex = new ThreadDumper.Regex(Collections.singleton("test-thread-.*"));

        assertTrue(regex.isThreadIncluded(1, "test-thread-1"));
        assertTrue(regex.isThreadIncluded(2, "test-thread-2"));
        assertTrue(regex.isThreadIncluded(3, "test-thread-3"));
        assertTrue(regex.isThreadIncluded(4, "test-thread-4"));

        assertFalse(regex.isThreadIncluded(5, "other-thread"));
    }

}

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

import me.lucko.spark.common.sampler.async.AsyncSampler;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.proto.SparkSamplerProtos;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;
import me.lucko.spark.test.plugin.TestCommandSender;
import me.lucko.spark.test.plugin.TestSparkPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class LockSamplerTest {

    private static boolean isAsyncSupportedOs() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ROOT).replace(" ", "");
        return os.equals("linux") || os.equals("macosx");
    }

    @Test
    public void testLockSampler(@TempDir Path directory) throws Exception {
        assumeTrue(isAsyncSupportedOs(), "async profiler is only supported on Linux and macOS");

        AtomicBoolean running = new AtomicBoolean(true);
        Object monitor = new Object();
        ReentrantLock lock = new ReentrantLock();

        Runnable monitorContender = () -> {
            while (running.get()) {
                synchronized (monitor) {
                    sleepQuietly(20);
                }
                sleepQuietly(2); // give the other thread a chance to take the monitor
            }
        };
        Runnable lockContender = () -> {
            while (running.get()) {
                lock.lock();
                try {
                    sleepQuietly(20);
                } finally {
                    lock.unlock();
                }
                sleepQuietly(2); // give the other thread a chance to take the lock
            }
        };

        Thread[] threads = new Thread[]{
                new Thread(monitorContender, "Monitor Test Thread 1"),
                new Thread(monitorContender, "Monitor Test Thread 2"),
                new Thread(lockContender, "Lock Test Thread 1"),
                new Thread(lockContender, "Lock Test Thread 2")
        };
        for (Thread thread : threads) {
            thread.setDaemon(true);
            thread.start();
        }

        try (TestSparkPlugin plugin = new TestSparkPlugin(directory)) {
            Sampler sampler = new SamplerBuilder()
                    .mode(SamplerMode.LOCK)
                    .threadDumper(ThreadDumper.ALL)
                    .threadGrouper(ThreadGrouper.BY_POOL)
                    .samplingInterval(SamplerMode.LOCK.defaultInterval())
                    .ignoreSleeping(true) // should be ignored in lock mode
                    .completeAfter(5, TimeUnit.SECONDS)
                    .start(plugin.platform());

            assertInstanceOf(AsyncSampler.class, sampler);
            assertEquals(SamplerMode.LOCK, sampler.getMode());

            sampler.getFuture().get(30, TimeUnit.SECONDS);

            Sampler.ExportProps exportProps = new Sampler.ExportProps()
                    .creator(TestCommandSender.INSTANCE.toData())
                    .classSourceLookup(() -> ClassSourceLookup.create(plugin.platform()));

            SparkSamplerProtos.SamplerData proto = sampler.toProto(plugin.platform(), exportProps);
            assertNotNull(proto);
            assertEquals(SamplerMetadata.SamplerMode.LOCK, proto.getMetadata().getSamplerMode());

            for (String name : new String[]{"Monitor Test Thread", "Lock Test Thread"}) {
                SparkSamplerProtos.ThreadNode thread = proto.getThreadsList().stream()
                        .filter(t -> t.getName().startsWith(name)) // name is suffixed with the pool size
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("Missing thread '" + name + "' in " + proto.getThreadsList().stream().map(SparkSamplerProtos.ThreadNode::getName).collect(Collectors.toList())));

                // two threads fight over one lock for ~5 seconds, so the pool as a whole should
                // have spent a good few seconds waiting. (values are in milliseconds)
                double totalTime = thread.getTimesList().stream().mapToDouble(Double::doubleValue).sum();
                System.out.println(name + " spent " + totalTime + "ms waiting for locks");
                assertTrue(totalTime > 1000, name + " total = " + totalTime);
                assertTrue(totalTime < 15000, name + " total = " + totalTime);
            }
        } finally {
            running.set(false);
        }
    }

    @Test
    public void testLockSamplerNotSupportedByJavaSampler(@TempDir Path directory) throws Exception {
        try (TestSparkPlugin plugin = new TestSparkPlugin(directory)) {
            assertThrows(UnsupportedOperationException.class, () -> new SamplerBuilder()
                    .mode(SamplerMode.LOCK)
                    .samplingInterval(SamplerMode.LOCK.defaultInterval())
                    .forceJavaSampler(true)
                    .start(plugin.platform()));
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}

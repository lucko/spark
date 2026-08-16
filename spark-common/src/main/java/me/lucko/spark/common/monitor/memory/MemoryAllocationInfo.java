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

package me.lucko.spark.common.monitor.memory;

import com.sun.management.ThreadMXBean;
import me.lucko.spark.common.monitor.Metrics;
import me.lucko.spark.common.monitor.MonitoringExecutor;
import me.lucko.spark.common.util.RollingAverage;
import me.lucko.spark.common.util.TimeUtil;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.math.BigDecimal;

/**
 * A utility for accessing memory allocation information from the JVM.
 */
public enum MemoryAllocationInfo {
    ;

    /** If the allocation info is supported */
    public static final boolean SUPPORTED;

    private static final ThreadMXBean BEAN;
    private static final Method GET_TOTAL_THREAD_ALLOCATED_BYTES_METHOD;

    /* Bytes per second - rolling averages */
    public static final RollingAverage BPS_AVERAGE_1_MIN = new RollingAverage(60);
    public static final RollingAverage BPS_AVERAGE_5_MIN = new RollingAverage(60 * 5);
    public static final RollingAverage BPS_AVERAGE_15_MIN = new RollingAverage(60 * 15);

    static {
        java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        BEAN = bean instanceof ThreadMXBean ? (ThreadMXBean) bean : null;
        SUPPORTED = BEAN != null && BEAN.isThreadAllocatedMemorySupported();

        if (SUPPORTED) {
            BEAN.setThreadAllocatedMemoryEnabled(true);
        }

        Method getTotalThreadAllocatedBytesMethod = null;
        if (BEAN != null) {
            try {
                // Java 21+
                getTotalThreadAllocatedBytesMethod = ThreadMXBean.class.getMethod("getTotalThreadAllocatedBytes");
            } catch (NoSuchMethodException e) {
                // ignore
            }
        }
        GET_TOTAL_THREAD_ALLOCATED_BYTES_METHOD = getTotalThreadAllocatedBytesMethod;

        if (SUPPORTED) {
            MonitoringExecutor.scheduleAtFixedRateMillis(new PollingTask(), Metrics.INTERVAL_MILLIS);
        }
    }

    /**
     * Ensures that the static initializer has been called.
     */
    @SuppressWarnings("EmptyMethod")
    public static void ensureMonitoring() {
        // intentionally empty
    }

    /**
     * Returns an approximation of the total amount of memory, in bytes, allocated
     * in heap memory by all threads since the Java virtual machine started.
     * The returned value is an approximation because some Java virtual machine
     * implementations may use object allocation mechanisms that result in a
     * delay between the time an object is allocated and the time its size is
     * recorded.
     *
     * @return an approximation of the total memory allocated, in bytes, in
     * heap memory since the Java virtual machine was started
     */
    public static long getTotalThreadAllocatedBytes() {
        if (!SUPPORTED) {
            throw new UnsupportedOperationException("Memory allocation info is not supported");
        }

        if (GET_TOTAL_THREAD_ALLOCATED_BYTES_METHOD != null) {
            try {
                return (long) GET_TOTAL_THREAD_ALLOCATED_BYTES_METHOD.invoke(BEAN);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }

        long[] threadIds = BEAN.getAllThreadIds();
        long[] allocatedBytes = BEAN.getThreadAllocatedBytes(threadIds);

        long total = 0;
        for (long bytes : allocatedBytes) {
            if (bytes > 0) {
                total += bytes;
            }
        }
        return total;
    }

    /**
     * Task to poll memory allocations.
     */
    private static final class PollingTask implements Runnable {
        private long previousAllocatedBytes = -1;
        private long previousTimeMillis = -1;

        @Override
        public void run() {
            long timeMillis = TimeUtil.monotonicCurrentTimeMillis();
            long totalAllocatedBytes = getTotalThreadAllocatedBytes();

            if (this.previousAllocatedBytes != -1) {
                long allocatedBytes = totalAllocatedBytes - this.previousAllocatedBytes;
                long elapsedMillis = timeMillis - this.previousTimeMillis;

                if (allocatedBytes >= 0) {
                    double allocatedBytesPerSecond = allocatedBytes / (elapsedMillis / 1000.0);
                    Metrics.MEMORY_ALLOCATION.record(timeMillis, allocatedBytesPerSecond);

                    BigDecimal allocatedBytesPerSecondDecimal = new BigDecimal(allocatedBytesPerSecond);
                    BPS_AVERAGE_1_MIN.add(allocatedBytesPerSecondDecimal);
                    BPS_AVERAGE_5_MIN.add(allocatedBytesPerSecondDecimal);
                    BPS_AVERAGE_15_MIN.add(allocatedBytesPerSecondDecimal);
                }
            }

            this.previousAllocatedBytes = totalAllocatedBytes;
            this.previousTimeMillis = timeMillis;
        }
    }
}

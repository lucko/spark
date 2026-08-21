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

package me.lucko.spark.common.util;

import com.google.common.primitives.Ints;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.common.platform.PlatformStatisticsProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.proto.SparkProtos;

import java.lang.management.MemoryUsage;
import java.time.Duration;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A metric time series backed by an array ring buffer.
 */
public class MetricSeries<T> {

    /** The retention period of the series, in milliseconds */
    private final long retentionMillis;

    /** Lock for synchronizing access to the series */
    private final ReentrantLock lock;

    /*
     * The ring buffer of samples, stored in chronological order.
     *
     * <p>The oldest samples are at index {@code head},
     * and the newest samples are at index {@code (head + size - 1) % timestamps.length}.</p>
     */
    private long[] timestamps;
    private Object[] values;

    /** Index of the oldest sample */
    private int head;

    /** Number of valid samples currently stored */
    private int size;

    /** Timestamp of the newest sample, or 0 if the series is empty */
    private volatile long newestTimestamp;

    public MetricSeries(Duration retention, int initialCapacity) {
        long retentionMillis = retention.toMillis();
        if (retentionMillis <= 0) {
            throw new IllegalArgumentException("retention must be > 0");
        }
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException("initialCapacity must be > 0");
        }

        this.retentionMillis = retentionMillis;
        this.lock = new ReentrantLock();
        this.timestamps = new long[initialCapacity];
        this.values = new Object[initialCapacity];
    }

    /**
     * Record a sample.
     *
     * <p>Samples must be supplied in chronological order.</p>
     *
     * @param timestampMillis monotonic timestamp
     * @param value metric value
     * @see TimeUtil#monotonicCurrentTimeMillis() for getting a current monotonic timestamp
     */
    public void record(long timestampMillis, T value) {
        if (timestampMillis <= 0) {
            throw new IllegalArgumentException("timestampMillis must be > 0");
        }

        this.lock.lock();
        try {
            prune(timestampMillis - this.retentionMillis);
            ensureCapacity();

            int index = (this.head + this.size) % this.timestamps.length;
            this.timestamps[index] = timestampMillis;
            this.values[index] = value;

            this.size++;
            this.newestTimestamp = timestampMillis;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Record a sample.
     *
     * @param value metric value
     */
    public void record(T value) {
        record(TimeUtil.monotonicCurrentTimeMillis(), value);
    }

    /**
     * Removes all samples older than {@code cutoff}.
     *
     * <p>Must be called with lock held.</p>
     */
    private void prune(long cutoff) {
        while (this.size > 0 && this.timestamps[this.head] < cutoff) {
            this.values[this.head] = null;
            this.head++;

            if (this.head == this.timestamps.length) {
                this.head = 0;
            }

            this.size--;
        }
    }

    /**
     * Calculates the index in the ring buffer for the given logical index.
     *
     * <p>Must be called with lock held.</p>
     *
     * @param i the logical index (0 = oldest sample, size-1 = newest sample)
     * @return the index in the ring buffer
     */
    private int indexFor(int i) {
        int index = this.head + i;
        if (index >= this.timestamps.length) {
            index -= this.timestamps.length;
        }
        return index;
    }

    /**
     * Returns the value at the given index in the ring buffer.
     *
     * <p>Must be called with lock held.</p>
     *
     * @param index the index in the ring buffer
     * @return the value at the given index
     */
    @SuppressWarnings("unchecked")
    private T valueAt(int index) {
        return (T) this.values[index];
    }

    /**
     * Grows the ring buffer if it is full.
     *
     * <p>Must be called with lock held.</p>
     */
    private void ensureCapacity() {
        if (this.size < this.timestamps.length) {
            return;
        }

        int oldCapacity = this.timestamps.length;
        if (oldCapacity > Integer.MAX_VALUE / 2) {
            throw new IllegalStateException("Metric series is too large");
        }

        int newCapacity = oldCapacity * 2;

        long[] newTimestamps = new long[newCapacity];
        Object[] newValues = new Object[newCapacity];

        // copy samples from the old ring buffer to the new one, in chronological order
        for (int i = 0; i < this.size; i++) {
            int oldIndex = (this.head + i) % oldCapacity;

            newTimestamps[i] = this.timestamps[oldIndex];
            newValues[i] = this.values[oldIndex];
        }

        this.timestamps = newTimestamps;
        this.values = newValues;
        this.head = 0;
    }

    public int size() {
        this.lock.lock();
        try {
            return this.size;
        } finally {
            this.lock.unlock();
        }
    }

    public boolean isEmpty() {
        this.lock.lock();
        try {
            return this.size == 0;
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Returns the timestamp of the oldest sample in the series, or 0 if the series is empty.
     *
     * @return the timestamp of the oldest sample, or 0 if empty
     */
    public long oldestTimestamp() {
        this.lock.lock();
        try {
            if (this.size == 0) {
                return 0;
            }
            return this.timestamps[this.head];
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Returns the timestamp of the newest sample in the series, or 0 if the series is empty.
     *
     * @return the timestamp of the newest sample, or 0 if empty
     */
    public long newestTimestamp() {
        return this.newestTimestamp;
    }

    /**
     * Iterates through samples in chronological order.
     * 
     * <p>The callback should not call record() on this same series.</p>
     */
    public void forEach(Consumer<T> consumer) {
        this.lock.lock();
        try {
            for (int i = 0; i < this.size; i++) {
                int index = indexFor(i);
                consumer.accept(this.timestamps[index], valueAt(index));
            }
        } finally {
            this.lock.unlock();
        }
    }

    public interface Consumer<T> {
        void accept(long timestampMillis, T value);
    }

    /**
     * Exports the series as a compact representation.
     *
     * @return an export of the series
     */
    public Export export() {
        this.lock.lock();
        try {
            Export export = new Export(0, new int[this.size], new Object[this.size]);

            long lastTimestamp = 0;
            for (int i = 0; i < this.size; i++) {
                int index = indexFor(i);

                if (i == 0) {
                    // first value - set the start timestamp and record a delta of 0
                    export.startTimestampMs = this.timestamps[index];
                    export.timestampDeltasMs[i] = 0;
                } else {
                    long delta = this.timestamps[index] - lastTimestamp;
                    if (delta < 0 || delta > 0xFFFFFFFFL) {
                        throw new IllegalStateException("Timestamp delta cannot be represented as uint32: " + delta);
                    }
                    export.timestampDeltasMs[i] = (int) delta;
                }

                lastTimestamp = this.timestamps[index];
                export.values[i] = this.values[index];
            }

            return export;
        } finally {
            this.lock.unlock();
        }
    }

    public static final class Export {
        private long startTimestampMs;
        private final int[] timestampDeltasMs;
        private final Object[] values;

        Export(long startTimestampMs, int[] timestampDeltasMs, Object[] values) {
            this.startTimestampMs = startTimestampMs;
            this.timestampDeltasMs = timestampDeltasMs;
            this.values = values;
        }

        public long startTimestampMs() {
            return this.startTimestampMs;
        }

        public int[] timestampDeltasMs() {
            return this.timestampDeltasMs;
        }

        public Object[] values() {
            return this.values;
        }
    }

    public static class Doubles extends MetricSeries<Double> {
        public Doubles(Duration retention, int initialCapacity) {
            super(retention, initialCapacity);
        }

        public SparkProtos.DoubleMetricSeries toProto() {
            Export export = export();
            SparkProtos.DoubleMetricSeries.Builder builder = SparkProtos.DoubleMetricSeries.newBuilder()
                    .setStartTimestampMs(export.startTimestampMs())
                    .addAllTimestampDeltasMs(Ints.asList(export.timestampDeltasMs()));
            for (Object value : export.values()) {
                builder.addValues((double) value);
            }
            return builder.build();
        }
    }

    public static class Averages extends MetricSeries<DoubleAverageInfo> {
        public Averages(Duration retention, int initialCapacity) {
            super(retention, initialCapacity);
        }

        public SparkProtos.AveragesMetricSeries toProto() {
            Export export = export();
            SparkProtos.AveragesMetricSeries.Builder builder = SparkProtos.AveragesMetricSeries.newBuilder()
                    .setStartTimestampMs(export.startTimestampMs())
                    .addAllTimestampDeltasMs(Ints.asList(export.timestampDeltasMs()));
            for (Object value : export.values()) {
                DoubleAverageInfo avgInfo = (DoubleAverageInfo) value;
                builder.addValues(PlatformStatisticsProvider.rollingAvgProto(avgInfo));
            }
            return builder.build();
        }
    }

    public static class MemoryUsages extends MetricSeries<MemoryUsage> {
        public MemoryUsages(Duration retention, int initialCapacity) {
            super(retention, initialCapacity);
        }

        public SparkProtos.MemoryUsageMetricSeries toProto() {
            Export export = export();
            SparkProtos.MemoryUsageMetricSeries.Builder builder = SparkProtos.MemoryUsageMetricSeries.newBuilder()
                    .setStartTimestampMs(export.startTimestampMs())
                    .addAllTimestampDeltasMs(Ints.asList(export.timestampDeltasMs()));
            for (Object value : export.values()) {
                MemoryUsage memoryUsage = (MemoryUsage) value;
                builder.addValues(PlatformStatisticsProvider.memoryUsageProto(memoryUsage));
            }
            return builder.build();
        }
    }

    public static class WorldInfo extends MetricSeries<WorldInfoProvider.CountsResult> {
        public WorldInfo(Duration retention, int initialCapacity) {
            super(retention, initialCapacity);
        }

        public SparkProtos.WorldInfoMetricSeries toProto() {
            Export export = export();
            SparkProtos.WorldInfoMetricSeries.Builder builder = SparkProtos.WorldInfoMetricSeries.newBuilder()
                    .setStartTimestampMs(export.startTimestampMs())
                    .addAllTimestampDeltasMs(Ints.asList(export.timestampDeltasMs()));
            for (Object value : export.values()) {
                WorldInfoProvider.CountsResult countsResult = (WorldInfoProvider.CountsResult) value;
                builder.addValues(SparkProtos.WorldInfoMetricSeries.Values.newBuilder()
                        .setPlayers(countsResult.players())
                        .setEntities(countsResult.entities())
                        .setTileEntities(countsResult.tileEntities())
                        .setChunks(countsResult.chunks())
                        .build()
                );
            }
            return builder.build();
        }
    }

}
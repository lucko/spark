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

package me.lucko.spark.common.sampler.async;

import com.google.common.collect.ImmutableList;
import me.lucko.spark.common.sampler.SamplerMode;
import me.lucko.spark.common.sampler.async.AsyncProfilerAccess.ProfilingEvent;
import me.lucko.spark.common.sampler.async.jfr.JfrReader;
import me.lucko.spark.common.sampler.async.jfr.JfrReader.AllocationSample;
import me.lucko.spark.common.sampler.async.jfr.JfrReader.ContendedLock;
import me.lucko.spark.common.sampler.async.jfr.JfrReader.Event;
import me.lucko.spark.common.sampler.async.jfr.JfrReader.ExecutionSample;

import java.util.Collection;
import java.util.Objects;

/**
 * Collects and processes sample events for a given type.
 *
 * @param <E> the event type
 */
public interface SampleCollector<E extends Event> {

    /**
     * Gets the arguments to initialise the profiler.
     *
     * @param access the async profiler access object
     * @return the initialisation arguments
     */
    Collection<String> initArguments(AsyncProfilerAccess access);

    /**
     * Gets the event class processed by this sample collector.
     *
     * @return the event class
     */
    Class<E> eventClass();

    /**
     * Gets the measurements for a given event
     *
     * @param reader the reader the event was read from
     * @param event the event
     * @return the measurement
     */
    long measure(JfrReader reader, E event);

    /**
     * Gets the mode for the collector.
     *
     * @return the mode
     */
    SamplerMode getMode();

    /**
     * Sample collector for execution (cpu time) profiles.
     */
    final class Execution implements SampleCollector<ExecutionSample> {
        private final int intervalMicroseconds; // time in microseconds

        public Execution(int intervalMicroseconds) {
            this.intervalMicroseconds = intervalMicroseconds;
        }

        @Override
        public Collection<String> initArguments(AsyncProfilerAccess access) {
            ProfilingEvent event = access.getProfilingEvent();
            Objects.requireNonNull(event, "event");

            return ImmutableList.of(
                    "event=" + event,
                    "interval=" + this.intervalMicroseconds + "us"
            );
        }

        @Override
        public Class<ExecutionSample> eventClass() {
            return ExecutionSample.class;
        }

        @Override
        public long measure(JfrReader reader, ExecutionSample event) {
            return event.value() * this.intervalMicroseconds;
        }

        @Override
        public SamplerMode getMode() {
            return SamplerMode.EXECUTION;
        }
    }

    /**
     * Sample collector for allocation (memory) profiles.
     */
    final class Allocation implements SampleCollector<AllocationSample> {
        private final int intervalBytes;
        private final boolean liveOnly;

        public Allocation(int intervalBytes, boolean liveOnly) {
            this.intervalBytes = intervalBytes;
            this.liveOnly = liveOnly;
        }

        public boolean isLiveOnly() {
            return this.liveOnly;
        }

        @Override
        public Collection<String> initArguments(AsyncProfilerAccess access) {
            ProfilingEvent event = access.getAllocationProfilingEvent();
            Objects.requireNonNull(event, "event");

            ImmutableList.Builder<String> builder = ImmutableList.builder();
            builder.add("event=" + event);
            builder.add("alloc=" + this.intervalBytes);
            if (this.liveOnly) {
                builder.add("live");
            }
            return builder.build();
        }

        @Override
        public Class<AllocationSample> eventClass() {
            return AllocationSample.class;
        }

        @Override
        public long measure(JfrReader reader, AllocationSample event) {
            return event.value();
        }

        @Override
        public SamplerMode getMode() {
            return SamplerMode.ALLOCATION;
        }
    }

    /**
     * Sample collector for lock (contention) profiles.
     *
     * <p>Measures the time threads spend waiting to enter contended monitors
     * (synchronized blocks/methods) or parked waiting for
     * {@code java.util.concurrent} locks.</p>
     */
    final class Lock implements SampleCollector<ContendedLock> {
        private final int thresholdMicroseconds; // time in microseconds

        public Lock(int thresholdMicroseconds) {
            this.thresholdMicroseconds = thresholdMicroseconds;
        }

        @Override
        public Collection<String> initArguments(AsyncProfilerAccess access) {
            ProfilingEvent event = access.getLockProfilingEvent();
            Objects.requireNonNull(event, "event");

            // async-profiler expects the lock threshold in nanoseconds
            long thresholdNanos = this.thresholdMicroseconds * 1000L;

            return ImmutableList.of(
                    "event=" + event,
                    "lock=" + thresholdNanos
            );
        }

        @Override
        public Class<ContendedLock> eventClass() {
            return ContendedLock.class;
        }

        @Override
        public long measure(JfrReader reader, ContendedLock event) {
            // the duration is recorded in ticks, convert to nanoseconds
            long ticksPerSec = reader.ticksPerSec;
            if (ticksPerSec <= 0 || ticksPerSec == 1_000_000_000L) {
                return event.value();
            }
            return (long) (event.value() * (1_000_000_000d / ticksPerSec));
        }

        @Override
        public SamplerMode getMode() {
            return SamplerMode.LOCK;
        }
    }

}

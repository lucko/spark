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

import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.platform.world.AsyncWorldInfoProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.proto.SparkProtos;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;
import java.util.logging.Level;

/**
 * Collects statistics for each profiling window.
 */
public class WindowStatisticsCollector {
    private static final SparkProtos.WindowStatistics ZERO = SparkProtos.WindowStatistics.newBuilder()
            .setDuration(ProfilingWindowUtils.WINDOW_SIZE_SECONDS * 1000)
            .build();

    /** The platform */
    private final SparkPlatform platform;

    /** Map of profiling window -> start time */
    private final Map<Integer, Long> windowStartTimes = new HashMap<>();
    /** Map of profiling window -> statistics */
    private final Map<Integer, SparkProtos.WindowStatistics> stats;

    private TickCounter tickCounter;

    public WindowStatisticsCollector(SparkPlatform platform) {
        this.platform = platform;
        this.stats = new ConcurrentHashMap<>();
    }

    /**
     * Indicates to the statistics collector that it should count the number
     * of ticks in each window using the provided {@link TickHook}.
     *
     * @param hook the tick hook
     */
    public void startCountingTicks(TickHook hook) {
        this.tickCounter = new NormalTickCounter(this.platform, hook);
    }

    /**
     * Indicates to the statistics collector that it should count the number
     * of ticks in each window, according to how many times the
     * {@link ExplicitTickCounter#increment()} method is called.
     *
     * @param hook the tick hook
     * @return the counter
     */
    public ExplicitTickCounter startCountingTicksExplicit(TickHook hook) {
        ExplicitTickCounter counter = new ExplicitTickCounter(this.platform, hook);
        this.tickCounter = counter;
        return counter;
    }

    public void stop() {
        if (this.tickCounter != null) {
            this.tickCounter.stop();
        }
    }

    /**
     * Gets the total number of ticks that have passed between the time
     * when the profiler started and stopped.
     *
     * <p>Importantly, note that this metric is different to the total number of ticks in a window
     * (which is recorded by {@link SparkProtos.WindowStatistics#getTicks()}) or the total number
     * of observed ticks if the 'only-ticks-over' aggregator is being used
     * (which is recorded by {@link SparkProtos.WindowStatistics#getTicks()}
     * and {@link ExplicitTickCounter#getTotalCountedTicks()}.</p>
     *
     * @return the total number of ticks in the profile
     */
    public int getTotalTicks() {
        return this.tickCounter == null ? -1 : this.tickCounter.getTotalTicks();
    }

    /**
     * Records the wall-clock time when a window was started.
     *
     * @param window the window
     */
    public void recordWindowStartTime(int window) {
        this.windowStartTimes.put(window, System.currentTimeMillis());
    }

    /**
     * Measures statistics for the given window if none have been recorded yet.
     *
     * @param window the window
     */
    public void measureNow(int window) {
        this.stats.computeIfAbsent(window, this::measure);
    }

    /**
     * Ensures that the exported map has statistics (even if they are zeroed) for all windows.
     *
     * @param windows the expected windows
     */
    public void ensureHasStatisticsForAllWindows(int[] windows) {
        for (int window : windows) {
            this.stats.computeIfAbsent(window, w -> ZERO);
        }
    }

    public void pruneStatistics(IntPredicate predicate) {
        this.stats.keySet().removeIf(predicate::test);
    }

    public Map<Integer, SparkProtos.WindowStatistics> export() {
        return this.stats;
    }

    /**
     * Measures current statistics, where possible averaging over the last minute. (1 min = 1 window)
     *
     * @return the current statistics
     */
    private SparkProtos.WindowStatistics measure(int window) {
        SparkProtos.WindowStatistics.Builder builder = SparkProtos.WindowStatistics.newBuilder();

        long endTime = System.currentTimeMillis();
        Long startTime = this.windowStartTimes.get(window);
        if (startTime == null) {
            this.platform.getPlugin().log(Level.WARNING, "Unknown start time for window " + window);
            startTime = endTime - (ProfilingWindowUtils.WINDOW_SIZE_SECONDS * 1000); // guess
        }

        builder.setStartTime(startTime);
        builder.setEndTime(endTime);
        builder.setDuration((int) (endTime - startTime));

        TickStatistics tickStatistics = this.platform.getTickStatistics();
        if (tickStatistics != null) {
            builder.setTps(tickStatistics.tps1Min());

            DoubleAverageInfo mspt = tickStatistics.duration1Min();
            if (mspt != null) {
                builder.setMsptMedian(mspt.median());
                builder.setMsptMax(mspt.max());
            }
        }

        if (this.tickCounter != null) {
            int ticks = this.tickCounter.getCountedTicksThisWindowAndReset();
            builder.setTicks(ticks);
        }

        builder.setCpuProcess(CpuMonitor.processLoad1MinAvg());
        builder.setCpuSystem(CpuMonitor.systemLoad1MinAvg());

        try {
            AsyncWorldInfoProvider worldInfoProvider = new AsyncWorldInfoProvider(this.platform, this.platform.getPlugin().createWorldInfoProvider());
            WorldInfoProvider.CountsResult counts = worldInfoProvider.getCounts();
            if (counts != null) {
                builder.setPlayers(counts.players());
                builder.setEntities(counts.entities());
                builder.setTileEntities(counts.tileEntities());
                builder.setChunks(counts.chunks());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return builder.build();
    }

    /**
     * Responsible for counting the number of ticks in a profile/window.
     */
    public interface TickCounter {

        /**
         * Stop the counter.
         */
        void stop();

        /**
         * Get the total number of ticks.
         *
         * <p>See {@link WindowStatisticsCollector#getTotalTicks()} for a longer explanation
         * of what this means exactly.</p>
         *
         * @return the total ticks
         */
        int getTotalTicks();

        /**
         * Gets the total number of ticks counted in the last window,
         * and resets the counter to zero.
         *
         * @return the number of ticks counted since the last time this method was called
         */
        int getCountedTicksThisWindowAndReset();
    }

    private static abstract class BaseTickCounter implements TickCounter {
        protected final SparkPlatform platform;
        protected final TickHook tickHook;

        /** The game tick when sampling first began */
        private final int startTick;

        /** The game tick when sampling stopped */
        private int stopTick = -1;

        BaseTickCounter(SparkPlatform platform, TickHook tickHook) {
            this.platform = platform;
            this.tickHook = tickHook;
            this.startTick = this.tickHook.getCurrentTick();
        }

        @Override
        public void stop() {
            this.stopTick = this.tickHook.getCurrentTick();
        }

        @Override
        public int getTotalTicks() {
            if (this.startTick == -1) {
                throw new IllegalStateException("start tick not recorded");
            }

            int stopTick = this.stopTick;
            if (stopTick == -1) {
                stopTick = this.tickHook.getCurrentTick();
            }

            return stopTick - this.startTick;
        }
    }

    /**
     * Counts the number of ticks in a window using a {@link TickHook}.
     */
    public static final class NormalTickCounter extends BaseTickCounter {
        private int last;

        NormalTickCounter(SparkPlatform platform, TickHook tickHook) {
            super(platform, tickHook);
            this.last = this.tickHook.getCurrentTick();
        }

        @Override
        public int getCountedTicksThisWindowAndReset() {
            synchronized (this) {
                int now = this.tickHook.getCurrentTick();
                int ticks = now - this.last;
                this.last = now;
                return ticks;
            }
        }
    }

    /**
     * Counts the number of ticks in a window according to the number of times
     * {@link #increment()} is called.
     *
     * Used by the {@link me.lucko.spark.common.sampler.java.TickedDataAggregator}.
     */
    public static final class ExplicitTickCounter extends BaseTickCounter {
        private final AtomicInteger counted = new AtomicInteger();
        private final AtomicInteger total = new AtomicInteger();

        ExplicitTickCounter(SparkPlatform platform, TickHook tickHook) {
            super(platform, tickHook);
        }

        public void increment() {
            this.counted.incrementAndGet();
            this.total.incrementAndGet();
        }

        public int getTotalCountedTicks() {
            return this.total.get();
        }

        @Override
        public int getCountedTicksThisWindowAndReset() {
            return this.counted.getAndSet(0);
        }
    }

}

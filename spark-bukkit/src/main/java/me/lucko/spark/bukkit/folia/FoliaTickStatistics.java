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

package me.lucko.spark.bukkit.folia;

import com.google.common.base.Suppliers;

import me.lucko.spark.api.statistic.StatisticWindow;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import me.lucko.spark.common.monitor.tick.TickStatistics;

import org.bukkit.Server;
import org.bukkit.World;

import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.ThreadedRegionizer.ThreadedRegion;
import io.papermc.paper.threadedregions.TickData;
import io.papermc.paper.threadedregions.TickData.SegmentedAverage;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import io.papermc.paper.threadedregions.TickRegions.TickRegionSectionData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class FoliaTickStatistics implements TickStatistics {

    private static final RegioniserReflection REGIONISER_REFLECTION = new RegioniserReflection();

    private final Supplier<List<ThreadedRegion<TickRegionData, TickRegionSectionData>>> regionSupplier;

    public FoliaTickStatistics(Server server) {
        this.regionSupplier = Suppliers.memoizeWithExpiration(() -> getRegions(server), 5, TimeUnit.MILLISECONDS);
    }

    @Override
    public double tps5Sec() {
        return tps(StatisticWindow.TicksPerSecond.SECONDS_5);
    }

    @Override
    public double tps10Sec() {
        return tps(StatisticWindow.TicksPerSecond.SECONDS_10);
    }

    @Override
    public double tps1Min() {
        return tps(StatisticWindow.TicksPerSecond.MINUTES_1);
    }

    @Override
    public double tps5Min() {
        return tps(StatisticWindow.TicksPerSecond.MINUTES_5);
    }

    @Override
    public double tps15Min() {
        return tps(StatisticWindow.TicksPerSecond.MINUTES_15);
    }

    @Override
    public boolean isDurationSupported() {
        return true;
    }

    @Override
    public DoubleAverageInfo duration10Sec() {
        return mspt(StatisticWindow.MillisPerTick.SECONDS_10);
    }

    @Override
    public DoubleAverageInfo duration1Min() {
        return mspt(StatisticWindow.MillisPerTick.MINUTES_1);
    }

    @Override
    public DoubleAverageInfo duration5Min() {
        return mspt(StatisticWindow.MillisPerTick.MINUTES_5);
    }

    private static List<ThreadedRegion<TickRegionData, TickRegionSectionData>> getRegions(Server server) {
        List<ThreadedRegion<TickRegionData, TickRegionSectionData>> regions = new ArrayList<>();
        for (World world : server.getWorlds()) {
            ThreadedRegionizer<TickRegionData, TickRegionSectionData> regionizer = REGIONISER_REFLECTION.getRegioniser(world);
            if (regionizer != null) {
                regionizer.computeForAllRegions(regions::add);
            }
        }
        return regions;
    }

    public double tps(StatisticWindow.TicksPerSecond window) {
        long nanoTime = System.nanoTime();
        return this.regionSupplier.get().stream()
                .map(region -> region.getData().getRegionSchedulingHandle())
                .map(handle -> switch (window) {
                    case SECONDS_5 -> handle.getTickReport5s(nanoTime);
                    case SECONDS_10 -> handle.getTickReport15s(nanoTime); // close enough!
                    case MINUTES_1 -> handle.getTickReport1m(nanoTime);
                    case MINUTES_5 -> handle.getTickReport5m(nanoTime);
                    case MINUTES_15 -> handle.getTickReport15m(nanoTime);
                })
                .filter(Objects::nonNull)
                .mapToDouble(data -> data.tpsData().segmentAll().average())
                .average()
                .orElse(20.0);
    }

    public DoubleAverageInfo mspt(StatisticWindow.MillisPerTick window) {
        long nanoTime = System.nanoTime();
        List<SegmentedAverage> averages = this.regionSupplier.get().stream()
                .map(region -> region.getData().getRegionSchedulingHandle())
                .map(handle -> switch (window) {
                    case SECONDS_10 -> handle.getTickReport15s(nanoTime); // close enough!
                    case MINUTES_1 -> handle.getTickReport1m(nanoTime);
                    case MINUTES_5 -> handle.getTickReport5m(nanoTime);
                })
                .filter(Objects::nonNull)
                .map(TickData.TickReportData::timePerTickData)
                .toList();
        return new SegmentedDoubleAverageInfo(averages);
    }

    private record SegmentedDoubleAverageInfo(List<SegmentedAverage> averages) implements DoubleAverageInfo {

        @Override
        public double mean() {
            return this.averages.stream()
                    .mapToDouble(avg -> avg.segmentAll().average() / 1.0E6)
                    .average()
                    .orElse(0);
        }

        @Override
        public double max() {
            return this.averages.stream()
                    .mapToDouble(avg -> avg.segmentAll().greatest() / 1.0E6)
                    .max()
                    .orElse(0);
        }

        @Override
        public double min() {
            return this.averages.stream()
                    .mapToDouble(avg -> avg.segmentAll().least() / 1.0E6)
                    .min()
                    .orElse(0);
        }

        @Override
        public double percentile(double percentile) {
            if (percentile == 0.50d) {
                // median
                return this.averages.stream()
                        .mapToDouble(avg -> avg.segmentAll().median() / 1.0E6)
                        .average()
                        .orElse(0);
            } else if (percentile == 0.95d) {
                // 95th percentile
                return this.averages.stream()
                        .mapToDouble(avg -> avg.segment5PercentWorst().average() / 1.0E6)
                        .average()
                        .orElse(0);
            }

            throw new UnsupportedOperationException("Unsupported percentile: " + percentile);
        }
    }

    /**
     * Ugly hack to obtain the regioniser for a {@link World} using reflection.
     */
    private static final class RegioniserReflection {
        private final AtomicBoolean initialised = new AtomicBoolean(false);
        private Method getHandleMethod;
        private Field regioniserField;

        private void initialise(Class<? extends World> craftWorldClass) {
            try {
                this.getHandleMethod = craftWorldClass.getMethod("getHandle");
                this.regioniserField = this.getHandleMethod.getReturnType().getField("regioniser");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @SuppressWarnings("unchecked")
        public ThreadedRegionizer<TickRegionData, TickRegionSectionData> getRegioniser(World world) {
            if (this.initialised.compareAndSet(false, true)) {
                initialise(world.getClass());
            }
            if (this.getHandleMethod == null || this.regioniserField == null) {
                return null;
            }

            try {
                Object handle = this.getHandleMethod.invoke(world);
                return (ThreadedRegionizer<TickRegionData, TickRegionSectionData>) this.regioniserField.get(handle);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }
}

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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.tick.TickStatistics;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Locale;
import java.util.function.BiFunction;

public enum SparkPlaceholder {

    TPS((platform, arg) -> {
        TickStatistics tickStatistics = platform.getTickStatistics();
        if (tickStatistics == null) {
            return null;
        }

        if (arg == null) {
            return Component.text()
                    .append(StatisticFormatter.formatTps(tickStatistics.tps5Sec())).append(Component.text(", "))
                    .append(StatisticFormatter.formatTps(tickStatistics.tps10Sec())).append(Component.text(", "))
                    .append(StatisticFormatter.formatTps(tickStatistics.tps1Min())).append(Component.text(", "))
                    .append(StatisticFormatter.formatTps(tickStatistics.tps5Min())).append(Component.text(", "))
                    .append(StatisticFormatter.formatTps(tickStatistics.tps15Min()))
                    .build();
        }

        switch (arg) {
            case "5s":
                return StatisticFormatter.formatTps(tickStatistics.tps5Sec());
            case "10s":
                return StatisticFormatter.formatTps(tickStatistics.tps10Sec());
            case "1m":
                return StatisticFormatter.formatTps(tickStatistics.tps1Min());
            case "5m":
                return StatisticFormatter.formatTps(tickStatistics.tps5Min());
            case "15m":
                return StatisticFormatter.formatTps(tickStatistics.tps15Min());
        }

        return null;
    }),

    TICKDURATION((platform, arg) -> {
        TickStatistics tickStatistics = platform.getTickStatistics();
        if (tickStatistics == null || !tickStatistics.isDurationSupported()) {
            return null;
        }

        if (arg == null) {
            return Component.text()
                    .append(StatisticFormatter.formatTickDurations(tickStatistics.duration10Sec())).append(Component.text(";  "))
                    .append(StatisticFormatter.formatTickDurations(tickStatistics.duration1Min()))
                    .build();
        }

        switch (arg) {
            case "10s":
                return StatisticFormatter.formatTickDurations(tickStatistics.duration10Sec());
            case "1m":
                return StatisticFormatter.formatTickDurations(tickStatistics.duration1Min());
        }

        return null;
    }),

    CPU_SYSTEM((platform, arg) -> {
        if (arg == null) {
            return Component.text()
                    .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(Component.text(", "))
                    .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(Component.text(", "))
                    .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
                    .build();
        }

        switch (arg) {
            case "10s":
                return StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad10SecAvg());
            case "1m":
                return StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad1MinAvg());
            case "15m":
                return StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad15MinAvg());
        }

        return null;
    }),

    CPU_PROCESS((platform, arg) -> {
        if (arg == null) {
            return Component.text()
                    .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(Component.text(", "))
                    .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(Component.text(", "))
                    .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
                    .build();
        }

        switch (arg) {
            case "10s":
                return StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad10SecAvg());
            case "1m":
                return StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad1MinAvg());
            case "15m":
                return StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad15MinAvg());
        }

        return null;
    });

    private final String name;
    private final BiFunction<SparkPlatform, String, TextComponent> function;

    SparkPlaceholder(BiFunction<SparkPlatform, String, TextComponent> function) {
        this.name = name().toLowerCase(Locale.ROOT);
        this.function = function;
    }

    public String getName() {
        return this.name;
    }

    public TextComponent resolve(SparkPlatform platform, String arg) {
        return this.function.apply(platform, arg);
    }

    public static TextComponent resolveComponent(SparkPlatform platform, String placeholder) {
        String[] parts = placeholder.split("_");

        if (parts.length == 0) {
            return null;
        }

        String label = parts[0];

        if (label.equals("tps")) {
            String arg = parts.length < 2 ? null : parts[1];
            return TPS.resolve(platform, arg);
        }

        if (label.equals("tickduration")) {
            String arg = parts.length < 2 ? null : parts[1];
            return TICKDURATION.resolve(platform, arg);
        }

        if (label.equals("cpu") && parts.length >= 2) {
            String type = parts[1];
            String arg = parts.length < 3 ? null : parts[2];

            if (type.equals("system")) {
                return CPU_SYSTEM.resolve(platform, arg);
            }
            if (type.equals("process")) {
                return CPU_PROCESS.resolve(platform, arg);
            }
        }

        return null;
    }

    public static String resolveFormattingCode(SparkPlatform platform, String placeholder) {
        TextComponent result = resolveComponent(platform, placeholder);
        if (result == null) {
            return null;
        }
        return LegacyComponentSerializer.legacySection().serialize(result);
    }
    
}

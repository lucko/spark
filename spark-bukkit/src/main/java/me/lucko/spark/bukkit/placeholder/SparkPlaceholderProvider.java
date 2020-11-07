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

package me.lucko.spark.bukkit.placeholder;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.modules.HealthModule;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

enum SparkPlaceholderProvider {
    ;

    public static TextComponent respondComponent(SparkPlatform platform, String placeholder) {
        if (placeholder.startsWith("tps")) {
            TickStatistics tickStatistics = platform.getTickStatistics();
            if (tickStatistics == null) {
                return null;
            }

            switch (placeholder) {
                case "tps":
                    return Component.text()
                            .append(HealthModule.formatTps(tickStatistics.tps5Sec())).append(Component.text(", "))
                            .append(HealthModule.formatTps(tickStatistics.tps10Sec())).append(Component.text(", "))
                            .append(HealthModule.formatTps(tickStatistics.tps1Min())).append(Component.text(", "))
                            .append(HealthModule.formatTps(tickStatistics.tps5Min())).append(Component.text(", "))
                            .append(HealthModule.formatTps(tickStatistics.tps15Min()))
                            .build();
                case "tps_5s":
                    return HealthModule.formatTps(tickStatistics.tps5Sec());
                case "tps_10s":
                    return HealthModule.formatTps(tickStatistics.tps10Sec());
                case "tps_1m":
                    return HealthModule.formatTps(tickStatistics.tps1Min());
                case "tps_5m":
                    return HealthModule.formatTps(tickStatistics.tps5Min());
                case "tps_15m":
                    return HealthModule.formatTps(tickStatistics.tps15Min());
            }
        }

        if (placeholder.startsWith("tickduration")) {
            TickStatistics tickStatistics = platform.getTickStatistics();
            if (tickStatistics == null || !tickStatistics.isDurationSupported()) {
                return null;
            }

            switch (placeholder) {
                case "tickduration":
                    return Component.text()
                            .append(HealthModule.formatTickDurations(tickStatistics.duration10Sec())).append(Component.text(";  "))
                            .append(HealthModule.formatTickDurations(tickStatistics.duration1Min()))
                            .build();
                case "tickduration_10s":
                    return HealthModule.formatTickDurations(tickStatistics.duration10Sec());
                case "tickduration_1m":
                    return HealthModule.formatTickDurations(tickStatistics.duration1Min());
            }
        }
        
        if (placeholder.startsWith("cpu")) {
            switch (placeholder) {
                case "cpu_system":
                    return Component.text()
                            .append(HealthModule.formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(Component.text(", "))
                            .append(HealthModule.formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(Component.text(", "))
                            .append(HealthModule.formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
                            .build();
                case "cpu_system_10s":
                    return HealthModule.formatCpuUsage(CpuMonitor.systemLoad10SecAvg());
                case "cpu_system_1m":
                    return HealthModule.formatCpuUsage(CpuMonitor.systemLoad1MinAvg());
                case "cpu_system_15m":
                    return HealthModule.formatCpuUsage(CpuMonitor.systemLoad15MinAvg());
                case "cpu_process":
                    return Component.text()
                            .append(HealthModule.formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(Component.text(", "))
                            .append(HealthModule.formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(Component.text(", "))
                            .append(HealthModule.formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
                            .build();
                case "cpu_process_10s":
                    return HealthModule.formatCpuUsage(CpuMonitor.processLoad10SecAvg());
                case "cpu_process_1m":
                    return HealthModule.formatCpuUsage(CpuMonitor.processLoad1MinAvg());
                case "cpu_process_15m":
                    return HealthModule.formatCpuUsage(CpuMonitor.processLoad15MinAvg());
            }
        }

        return null;
    }

    public static String respond(SparkPlatform platform, String placeholder) {
        TextComponent result = respondComponent(platform, placeholder);
        if (result == null) {
            return null;
        }
        return LegacyComponentSerializer.legacySection().serialize(result);
    }
    
}

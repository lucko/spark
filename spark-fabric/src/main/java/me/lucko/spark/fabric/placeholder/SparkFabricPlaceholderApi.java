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

package me.lucko.spark.fabric.placeholder;

import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.PlaceholderResult;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.util.RollingAverage;
import me.lucko.spark.common.util.StatisticFormatter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class SparkFabricPlaceholderApi {
    private final SparkPlatform platform;

    public SparkFabricPlaceholderApi(SparkPlatform platform) {
        this.platform = platform;

        PlaceholderAPI.register(
                new Identifier("spark", "tps"),
                context -> {
                    TickStatistics tickStatistics = platform.getTickStatistics();
                    if (tickStatistics == null) {
                        return PlaceholderResult.invalid();
                    }

                    if (context.hasArgument()) {
                        Double tps = switch (context.getArgument()) {
                            case "5s":
                                yield tickStatistics.tps5Sec();
                            case "10s":
                                yield tickStatistics.tps10Sec();
                            case "1m":
                                yield tickStatistics.tps1Min();
                            case "5m":
                                yield tickStatistics.tps5Min();
                            case "15m":
                                yield tickStatistics.tps15Min();
                            default:
                                yield null;
                        };

                        if (tps == null) {
                            return PlaceholderResult.invalid("Invalid argument");
                        } else {
                            return PlaceholderResult.value(toText(StatisticFormatter.formatTps(tps)));
                        }
                    } else {
                        return PlaceholderResult.value(toText(
                                Component.text()
                                        .append(StatisticFormatter.formatTps(tickStatistics.tps5Sec())).append(Component.text(", "))
                                        .append(StatisticFormatter.formatTps(tickStatistics.tps10Sec())).append(Component.text(", "))
                                        .append(StatisticFormatter.formatTps(tickStatistics.tps1Min())).append(Component.text(", "))
                                        .append(StatisticFormatter.formatTps(tickStatistics.tps5Min())).append(Component.text(", "))
                                        .append(StatisticFormatter.formatTps(tickStatistics.tps15Min()))
                                        .build()
                        ));
                    }
                }
        );

        PlaceholderAPI.register(
                new Identifier("spark", "tickduration"),
                context -> {
                    TickStatistics tickStatistics = platform.getTickStatistics();
                    if (tickStatistics == null || !tickStatistics.isDurationSupported()) {
                        return PlaceholderResult.invalid();
                    }

                    if (context.hasArgument()) {
                        RollingAverage duration = switch (context.getArgument()) {
                            case "10s":
                                yield tickStatistics.duration10Sec();
                            case "1m":
                                yield tickStatistics.duration1Min();
                            default:
                                yield null;
                        };

                        if (duration == null) {
                            return PlaceholderResult.invalid("Invalid argument");
                        } else {
                            return PlaceholderResult.value(toText(StatisticFormatter.formatTickDurations(duration)));
                        }
                    } else {
                        return PlaceholderResult.value(toText(
                                Component.text()
                                        .append(StatisticFormatter.formatTickDurations(tickStatistics.duration10Sec())).append(Component.text(";  "))
                                        .append(StatisticFormatter.formatTickDurations(tickStatistics.duration1Min()))
                                        .build()
                        ));
                    }
                }
        );

        PlaceholderAPI.register(
                new Identifier("spark", "cpu_system"),
                context -> {
                    if (context.hasArgument()) {
                        Double usage = switch (context.getArgument()) {
                            case "10s":
                                yield CpuMonitor.systemLoad10SecAvg();
                            case "1m":
                                yield CpuMonitor.systemLoad1MinAvg();
                            case "15m":
                                yield CpuMonitor.systemLoad15MinAvg();
                            default:
                                yield null;
                        };

                        if (usage == null) {
                            return PlaceholderResult.invalid("Invalid argument");
                        } else {
                            return PlaceholderResult.value(toText(StatisticFormatter.formatCpuUsage(usage)));
                        }
                    } else {
                        return PlaceholderResult.value(toText(
                                Component.text()
                                        .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(Component.text(", "))
                                        .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(Component.text(", "))
                                        .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
                                        .build()
                        ));
                    }
                }
        );

        PlaceholderAPI.register(
                new Identifier("spark", "cpu_process"),
                context -> {
                    if (context.hasArgument()) {
                        Double usage = switch (context.getArgument()) {
                            case "10s":
                                yield CpuMonitor.processLoad10SecAvg();
                            case "1m":
                                yield CpuMonitor.processLoad1MinAvg();
                            case "15m":
                                yield CpuMonitor.processLoad15MinAvg();
                            default:
                                yield null;
                        };

                        if (usage == null) {
                            return PlaceholderResult.invalid("Invalid argument");
                        } else {
                            return PlaceholderResult.value(toText(StatisticFormatter.formatCpuUsage(usage)));
                        }
                    } else {
                        return PlaceholderResult.value(toText(
                                Component.text()
                                        .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(Component.text(", "))
                                        .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(Component.text(", "))
                                        .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
                                        .build()
                        ));
                    }
                }
        );
    }

    private Text toText(Component component) {
        return Text.Serializer.fromJson(GsonComponentSerializer.gson().serialize(component));
    }
}

package me.lucko.spark.fabric.placeholder;

import eu.pb4.placeholders.PlaceholderAPI;
import eu.pb4.placeholders.PlaceholderResult;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.modules.HealthModule;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.util.RollingAverage;

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
                            return PlaceholderResult.value(toText(HealthModule.formatTps(tps)));
                        }
                    } else {
                        return PlaceholderResult.value(toText(
                                Component.text()
                                        .append(HealthModule.formatTps(tickStatistics.tps5Sec())).append(Component.text(", "))
                                        .append(HealthModule.formatTps(tickStatistics.tps10Sec())).append(Component.text(", "))
                                        .append(HealthModule.formatTps(tickStatistics.tps1Min())).append(Component.text(", "))
                                        .append(HealthModule.formatTps(tickStatistics.tps5Min())).append(Component.text(", "))
                                        .append(HealthModule.formatTps(tickStatistics.tps15Min()))
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
                            return PlaceholderResult.value(toText(HealthModule.formatTickDurations(duration)));
                        }
                    } else {
                        return PlaceholderResult.value(toText(
                                Component.text()
                                        .append(HealthModule.formatTickDurations(tickStatistics.duration10Sec())).append(Component.text(";  "))
                                        .append(HealthModule.formatTickDurations(tickStatistics.duration1Min()))
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
                            return PlaceholderResult.value(toText(HealthModule.formatCpuUsage(usage)));
                        }
                    } else {
                        return PlaceholderResult.value(toText(
                                Component.text()
                                        .append(HealthModule.formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(Component.text(", "))
                                        .append(HealthModule.formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(Component.text(", "))
                                        .append(HealthModule.formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
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
                            return PlaceholderResult.value(toText(HealthModule.formatCpuUsage(usage)));
                        }
                    } else {
                        return PlaceholderResult.value(toText(
                                Component.text()
                                        .append(HealthModule.formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(Component.text(", "))
                                        .append(HealthModule.formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(Component.text(", "))
                                        .append(HealthModule.formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
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

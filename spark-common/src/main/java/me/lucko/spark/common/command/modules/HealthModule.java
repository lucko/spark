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

package me.lucko.spark.common.command.modules;

import com.google.common.base.Strings;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.tick.TpsCalculator;
import me.lucko.spark.common.util.FormatUtil;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.format.TextDecoration;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class HealthModule implements CommandModule {

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("tps", "cpu")
                .executor((platform, sender, resp, arguments) -> {
                    TpsCalculator tpsCalculator = platform.getTpsCalculator();
                    if (tpsCalculator != null) {
                        resp.replyPrefixed(TextComponent.of("TPS from last 5s, 10s, 1m, 5m, 15m:"));
                        resp.replyPrefixed(TextComponent.builder(" ")
                                .append(formatTps(tpsCalculator.avg5Sec())).append(TextComponent.of(", "))
                                .append(formatTps(tpsCalculator.avg10Sec())).append(TextComponent.of(", "))
                                .append(formatTps(tpsCalculator.avg1Min())).append(TextComponent.of(", "))
                                .append(formatTps(tpsCalculator.avg5Min())).append(TextComponent.of(", "))
                                .append(formatTps(tpsCalculator.avg15Min()))
                                .build()
                        );
                        resp.replyPrefixed(TextComponent.empty());
                    }

                    resp.replyPrefixed(TextComponent.of("CPU usage from last 10s, 1m, 15m:"));
                    resp.replyPrefixed(TextComponent.builder(" ")
                            .append(formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(TextComponent.of(", "))
                            .append(formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(TextComponent.of(", "))
                            .append(formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
                            .append(TextComponent.of("  (system)", TextColor.DARK_GRAY))
                            .build()
                    );
                    resp.replyPrefixed(TextComponent.builder(" ")
                            .append(formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(TextComponent.of(", "))
                            .append(formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(TextComponent.of(", "))
                            .append(formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
                            .append(TextComponent.of("  (process)", TextColor.DARK_GRAY))
                            .build()
                    );
                })
                .tabCompleter(Command.TabCompleter.empty())
                .build()
        );

        consumer.accept(Command.builder()
                .aliases("healthreport", "health", "ht")
                .argumentUsage("memory", null)
                .executor((platform, sender, resp, arguments) -> {
                    resp.replyPrefixed(TextComponent.of("Generating server health report..."));
                    platform.getPlugin().runAsync(() -> {
                        List<Component> report = new LinkedList<>();
                        report.add(TextComponent.empty());

                        TpsCalculator tpsCalculator = platform.getTpsCalculator();
                        if (tpsCalculator != null) {
                            report.add(TextComponent.builder("")
                                    .append(TextComponent.builder(">").color(TextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).build())
                                    .append(TextComponent.space())
                                    .append(TextComponent.of("TPS from last 5s, 10s, 1m, 5m, 15m:", TextColor.GOLD))
                                    .build()
                            );
                            report.add(TextComponent.builder("    ")
                                    .append(formatTps(tpsCalculator.avg5Sec())).append(TextComponent.of(", "))
                                    .append(formatTps(tpsCalculator.avg10Sec())).append(TextComponent.of(", "))
                                    .append(formatTps(tpsCalculator.avg1Min())).append(TextComponent.of(", "))
                                    .append(formatTps(tpsCalculator.avg5Min())).append(TextComponent.of(", "))
                                    .append(formatTps(tpsCalculator.avg15Min()))
                                    .build()
                            );
                            report.add(TextComponent.empty());
                        }

                        report.add(TextComponent.builder("")
                                .append(TextComponent.builder(">").color(TextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).build())
                                .append(TextComponent.space())
                                .append(TextComponent.of("CPU usage from last 10s, 1m, 15m:", TextColor.GOLD))
                                .build()
                        );
                        report.add(TextComponent.builder("    ")
                                .append(formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(TextComponent.of(", "))
                                .append(formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(TextComponent.of(", "))
                                .append(formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
                                .append(TextComponent.of("  (system)", TextColor.DARK_GRAY))
                                .build()
                        );
                        report.add(TextComponent.builder("    ")
                                .append(formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(TextComponent.of(", "))
                                .append(formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(TextComponent.of(", "))
                                .append(formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
                                .append(TextComponent.of("  (process)", TextColor.DARK_GRAY))
                                .build()
                        );
                        report.add(TextComponent.empty());

                        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
                        report.add(TextComponent.builder("")
                                .append(TextComponent.builder(">").color(TextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).build())
                                .append(TextComponent.space())
                                .append(TextComponent.of("Memory usage:", TextColor.GOLD))
                                .build()
                        );
                        report.add(TextComponent.builder("    ")
                                .append(TextComponent.of(FormatUtil.formatBytes(heapUsage.getUsed()), TextColor.WHITE))
                                .append(TextComponent.space())
                                .append(TextComponent.of("/", TextColor.GRAY))
                                .append(TextComponent.space())
                                .append(TextComponent.of(FormatUtil.formatBytes(heapUsage.getMax()), TextColor.WHITE))
                                .append(TextComponent.of("   "))
                                .append(TextComponent.of("(", TextColor.GRAY))
                                .append(TextComponent.of(FormatUtil.percent(heapUsage.getUsed(), heapUsage.getMax()), TextColor.GREEN))
                                .append(TextComponent.of(")", TextColor.GRAY))
                                .build()
                        );
                        report.add(TextComponent.builder("    ").append(generateMemoryUsageDiagram(heapUsage, 40)).build());
                        report.add(TextComponent.empty());

                        if (arguments.boolFlag("memory")) {
                            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
                            report.add(TextComponent.builder("")
                                    .append(TextComponent.builder(">").color(TextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).build())
                                    .append(TextComponent.space())
                                    .append(TextComponent.of("Non-heap memory usage:", TextColor.GOLD))
                                    .build()
                            );
                            report.add(TextComponent.builder("    ")
                                    .append(TextComponent.of(FormatUtil.formatBytes(nonHeapUsage.getUsed()), TextColor.WHITE))
                                    .build()
                            );
                            report.add(TextComponent.empty());

                            List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
                            for (MemoryPoolMXBean memoryPool : memoryPoolMXBeans) {
                                if (memoryPool.getType() != MemoryType.HEAP) {
                                    continue;
                                }

                                MemoryUsage usage = memoryPool.getUsage();
                                MemoryUsage collectionUsage = memoryPool.getCollectionUsage();

                                if (usage.getMax() == -1) {
                                    usage = new MemoryUsage(usage.getInit(), usage.getUsed(), usage.getCommitted(), usage.getCommitted());
                                }

                                report.add(TextComponent.builder("")
                                        .append(TextComponent.builder(">").color(TextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).build())
                                        .append(TextComponent.space())
                                        .append(TextComponent.of(memoryPool.getName() + " pool usage:", TextColor.GOLD))
                                        .build()
                                );
                                report.add(TextComponent.builder("    ")
                                        .append(TextComponent.of(FormatUtil.formatBytes(usage.getUsed()), TextColor.WHITE))
                                        .append(TextComponent.space())
                                        .append(TextComponent.of("/", TextColor.GRAY))
                                        .append(TextComponent.space())
                                        .append(TextComponent.of(FormatUtil.formatBytes(usage.getMax()), TextColor.WHITE))
                                        .append(TextComponent.of("   "))
                                        .append(TextComponent.of("(", TextColor.GRAY))
                                        .append(TextComponent.of(FormatUtil.percent(usage.getUsed(), usage.getMax()), TextColor.GREEN))
                                        .append(TextComponent.of(")", TextColor.GRAY))
                                        .build()
                                );
                                report.add(TextComponent.builder("    ").append(generateMemoryPoolDiagram(usage, collectionUsage, 40)).build());

                                if (collectionUsage != null) {
                                    report.add(TextComponent.builder("     ")
                                            .append(TextComponent.of("-", TextColor.RED))
                                            .append(TextComponent.space())
                                            .append(TextComponent.of("Usage at last GC:", TextColor.GRAY))
                                            .append(TextComponent.space())
                                            .append(TextComponent.of(FormatUtil.formatBytes(collectionUsage.getUsed()), TextColor.WHITE))
                                            .build()
                                    );
                                }
                                report.add(TextComponent.empty());
                            }
                        }

                        try {
                            FileStore fileStore = Files.getFileStore(Paths.get("."));
                            long totalSpace = fileStore.getTotalSpace();
                            long usedSpace = totalSpace - fileStore.getUsableSpace();
                            report.add(TextComponent.builder("")
                                    .append(TextComponent.builder(">").color(TextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true).build())
                                    .append(TextComponent.space())
                                    .append(TextComponent.of("Disk usage:", TextColor.GOLD))
                                    .build()
                            );
                            report.add(TextComponent.builder("    ")
                                    .append(TextComponent.of(FormatUtil.formatBytes(usedSpace), TextColor.WHITE))
                                    .append(TextComponent.space())
                                    .append(TextComponent.of("/", TextColor.GRAY))
                                    .append(TextComponent.space())
                                    .append(TextComponent.of(FormatUtil.formatBytes(totalSpace), TextColor.WHITE))
                                    .append(TextComponent.of("   "))
                                    .append(TextComponent.of("(", TextColor.GRAY))
                                    .append(TextComponent.of(FormatUtil.percent(usedSpace, totalSpace), TextColor.GREEN))
                                    .append(TextComponent.of(")", TextColor.GRAY))
                                    .build()
                            );
                            report.add(TextComponent.builder("    ").append(generateDiskUsageDiagram(usedSpace, totalSpace, 40)).build());
                            report.add(TextComponent.empty());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        report.forEach(resp::reply);
                    });
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--memory"))
                .build()
        );
    }

    private static TextComponent formatTps(double tps) {
        TextColor color;
        if (tps > 18.0) {
            color = TextColor.GREEN;
        } else if (tps > 16.0) {
            color = TextColor.YELLOW;
        } else {
            color = TextColor.RED;
        }

        return TextComponent.of( (tps > 20.0 ? "*" : "") + Math.min(Math.round(tps * 100.0) / 100.0, 20.0), color);
    }

    private static TextComponent formatCpuUsage(double usage) {
        TextColor color;
        if (usage > 0.9) {
            color = TextColor.RED;
        } else if (usage > 0.65) {
            color = TextColor.YELLOW;
        } else {
            color = TextColor.GREEN;
        }

        return TextComponent.of(FormatUtil.percent(usage, 1d), color);
    }

    private static TextComponent generateMemoryUsageDiagram(MemoryUsage usage, int length) {
        double used = usage.getUsed();
        double committed = usage.getCommitted();
        double max = usage.getMax();

        int usedChars = (int) ((used * length) / max);
        int committedChars = (int) ((committed * length) / max);

        TextComponent.Builder line = TextComponent.builder(Strings.repeat("/", usedChars)).color(TextColor.GRAY);
        if (committedChars > usedChars) {
            line.append(TextComponent.of(Strings.repeat(" ", (committedChars - usedChars) - 1)));
            line.append(TextComponent.of("|", TextColor.YELLOW));
        }
        if (length > committedChars) {
            line.append(TextComponent.of(Strings.repeat(" ", (length - committedChars))));
        }

        return TextComponent.builder("")
                .append(TextComponent.of("[", TextColor.DARK_GRAY))
                .append(line.build())
                .append(TextComponent.of("]", TextColor.DARK_GRAY))
                .build();
    }

    private static TextComponent generateMemoryPoolDiagram(MemoryUsage usage, MemoryUsage collectionUsage, int length) {
        double used = usage.getUsed();
        double collectionUsed = used;
        if (collectionUsage != null) {
            collectionUsed = collectionUsage.getUsed();
        }
        double committed = usage.getCommitted();
        double max = usage.getMax();

        int usedChars = (int) ((used * length) / max);
        int collectionUsedChars = (int) ((collectionUsed * length) / max);
        int committedChars = (int) ((committed * length) / max);

        TextComponent.Builder line = TextComponent.builder(Strings.repeat("/", collectionUsedChars)).color(TextColor.GRAY);

        if (usedChars > collectionUsedChars) {
            line.append(TextComponent.of("|", TextColor.RED));
            line.append(TextComponent.of(Strings.repeat("/", (usedChars - collectionUsedChars) - 1), TextColor.GRAY));
        }
        if (committedChars > usedChars) {
            line.append(TextComponent.of(Strings.repeat(" ", (committedChars - usedChars) - 1)));
            line.append(TextComponent.of("|", TextColor.YELLOW));
        }
        if (length > committedChars) {
            line.append(TextComponent.of(Strings.repeat(" ", (length - committedChars))));
        }

        return TextComponent.builder("")
                .append(TextComponent.of("[", TextColor.DARK_GRAY))
                .append(line.build())
                .append(TextComponent.of("]", TextColor.DARK_GRAY))
                .build();
    }

    private static TextComponent generateDiskUsageDiagram(double used, double max, int length) {
        int usedChars = (int) ((used * length) / max);
        String line = Strings.repeat("/", usedChars) + Strings.repeat(" ", length - usedChars);
        return TextComponent.builder("")
                .append(TextComponent.of("[", TextColor.DARK_GRAY))
                .append(TextComponent.of(line, TextColor.GRAY))
                .append(TextComponent.of("]", TextColor.DARK_GRAY))
                .build();
    }

}

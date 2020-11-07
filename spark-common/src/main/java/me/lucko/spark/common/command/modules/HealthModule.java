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
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.util.FormatUtil;
import me.lucko.spark.common.util.RollingAverage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

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

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.TextDecoration.*;

public class HealthModule implements CommandModule {

    private static final int MSPT_95_PERCENTILE = 95;

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("tps", "cpu")
                .executor((platform, sender, resp, arguments) -> {
                    TickStatistics tickStatistics = platform.getTickStatistics();
                    if (tickStatistics != null) {
                        resp.replyPrefixed(text("TPS from last 5s, 10s, 1m, 5m, 15m:"));
                        resp.replyPrefixed(text()
                                .content(" ")
                                .append(formatTps(tickStatistics.tps5Sec())).append(text(", "))
                                .append(formatTps(tickStatistics.tps10Sec())).append(text(", "))
                                .append(formatTps(tickStatistics.tps1Min())).append(text(", "))
                                .append(formatTps(tickStatistics.tps5Min())).append(text(", "))
                                .append(formatTps(tickStatistics.tps15Min()))
                                .build()
                        );
                        resp.replyPrefixed(empty());

                        if (tickStatistics.isDurationSupported()) {
                            resp.replyPrefixed(text("Tick durations (min/med/95%ile/max ms) from last 10s, 1m:"));
                            resp.replyPrefixed(text()
                                    .content(" ")
                                    .append(formatTickDurations(tickStatistics.duration10Sec())).append(text(";  "))
                                    .append(formatTickDurations(tickStatistics.duration1Min()))
                                    .build()
                            );
                            resp.replyPrefixed(empty());
                        }
                    }

                    resp.replyPrefixed(text("CPU usage from last 10s, 1m, 15m:"));
                    resp.replyPrefixed(text()
                            .content(" ")
                            .append(formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(text(", "))
                            .append(formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(text(", "))
                            .append(formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
                            .append(text("  (system)", DARK_GRAY))
                            .build()
                    );
                    resp.replyPrefixed(text()
                            .content(" ")
                            .append(formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(text(", "))
                            .append(formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(text(", "))
                            .append(formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
                            .append(text("  (process)", DARK_GRAY))
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
                    resp.replyPrefixed(text("Generating server health report..."));
                    platform.getPlugin().executeAsync(() -> {
                        List<Component> report = new LinkedList<>();
                        report.add(empty());

                        TickStatistics tickStatistics = platform.getTickStatistics();
                        if (tickStatistics != null) {
                            report.add(text()
                                    .append(text(">", DARK_GRAY, BOLD))
                                    .append(space())
                                    .append(text("TPS from last 5s, 10s, 1m, 5m, 15m:", GOLD))
                                    .build()
                            );
                            report.add(text()
                                    .content("    ")
                                    .append(formatTps(tickStatistics.tps5Sec())).append(text(", "))
                                    .append(formatTps(tickStatistics.tps10Sec())).append(text(", "))
                                    .append(formatTps(tickStatistics.tps1Min())).append(text(", "))
                                    .append(formatTps(tickStatistics.tps5Min())).append(text(", "))
                                    .append(formatTps(tickStatistics.tps15Min()))
                                    .build()
                            );
                            report.add(empty());

                            if (tickStatistics.isDurationSupported()) {
                                report.add(text()
                                        .append(text(">", DARK_GRAY, BOLD))
                                        .append(space())
                                        .append(text("Tick durations (min/med/95%ile/max ms) from last 10s, 1m:", GOLD))
                                        .build()
                                );
                                report.add(text()
                                        .content("    ")
                                        .append(formatTickDurations(tickStatistics.duration10Sec())).append(text("; "))
                                        .append(formatTickDurations(tickStatistics.duration1Min()))
                                        .build()
                                );
                                report.add(empty());
                            }
                        }

                        report.add(text()
                                .append(text(">", DARK_GRAY, BOLD))
                                .append(space())
                                .append(text("CPU usage from last 10s, 1m, 15m:", GOLD))
                                .build()
                        );
                        report.add(text()
                                .content("    ")
                                .append(formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(text(", "))
                                .append(formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(text(", "))
                                .append(formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
                                .append(text("  (system)", DARK_GRAY))
                                .build()
                        );
                        report.add(text()
                                .content("    ")
                                .append(formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(text(", "))
                                .append(formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(text(", "))
                                .append(formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
                                .append(text("  (process)", DARK_GRAY))
                                .build()
                        );
                        report.add(empty());

                        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
                        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
                        report.add(text()
                                .append(text(">", DARK_GRAY, BOLD))
                                .append(space())
                                .append(text("Memory usage:", GOLD))
                                .build()
                        );
                        report.add(text()
                                .content("    ")
                                .append(text(FormatUtil.formatBytes(heapUsage.getUsed()), WHITE))
                                .append(space())
                                .append(text("/", GRAY))
                                .append(space())
                                .append(text(FormatUtil.formatBytes(heapUsage.getMax()), WHITE))
                                .append(text("   "))
                                .append(text("(", GRAY))
                                .append(text(FormatUtil.percent(heapUsage.getUsed(), heapUsage.getMax()), GREEN))
                                .append(text(")", GRAY))
                                .build()
                        );
                        report.add(text().content("    ").append(generateMemoryUsageDiagram(heapUsage, 40)).build());
                        report.add(empty());

                        if (arguments.boolFlag("memory")) {
                            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
                            report.add(text()
                                    .append(text(">", DARK_GRAY, BOLD))
                                    .append(space())
                                    .append(text("Non-heap memory usage:", GOLD))
                                    .build()
                            );
                            report.add(text()
                                    .content("    ")
                                    .append(text(FormatUtil.formatBytes(nonHeapUsage.getUsed()), WHITE))
                                    .build()
                            );
                            report.add(empty());

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

                                report.add(text()
                                        .append(text(">", DARK_GRAY, BOLD))
                                        .append(space())
                                        .append(text(memoryPool.getName() + " pool usage:", GOLD))
                                        .build()
                                );
                                report.add(text()
                                        .content("    ")
                                        .append(text(FormatUtil.formatBytes(usage.getUsed()), WHITE))
                                        .append(space())
                                        .append(text("/", GRAY))
                                        .append(space())
                                        .append(text(FormatUtil.formatBytes(usage.getMax()), WHITE))
                                        .append(text("   "))
                                        .append(text("(", GRAY))
                                        .append(text(FormatUtil.percent(usage.getUsed(), usage.getMax()), GREEN))
                                        .append(text(")", GRAY))
                                        .build()
                                );
                                report.add(text().content("    ").append(generateMemoryPoolDiagram(usage, collectionUsage, 40)).build());

                                if (collectionUsage != null) {
                                    report.add(text()
                                            .content("     ")
                                            .append(text("-", RED))
                                            .append(space())
                                            .append(text("Usage at last GC:", GRAY))
                                            .append(space())
                                            .append(text(FormatUtil.formatBytes(collectionUsage.getUsed()), WHITE))
                                            .build()
                                    );
                                }
                                report.add(empty());
                            }
                        }

                        try {
                            FileStore fileStore = Files.getFileStore(Paths.get("."));
                            long totalSpace = fileStore.getTotalSpace();
                            long usedSpace = totalSpace - fileStore.getUsableSpace();
                            report.add(text()
                                    .append(text(">", DARK_GRAY, BOLD))
                                    .append(space())
                                    .append(text("Disk usage:", GOLD))
                                    .build()
                            );
                            report.add(text()
                                    .content("    ")
                                    .append(text(FormatUtil.formatBytes(usedSpace), WHITE))
                                    .append(space())
                                    .append(text("/", GRAY))
                                    .append(space())
                                    .append(text(FormatUtil.formatBytes(totalSpace), WHITE))
                                    .append(text("   "))
                                    .append(text("(", GRAY))
                                    .append(text(FormatUtil.percent(usedSpace, totalSpace), GREEN))
                                    .append(text(")", GRAY))
                                    .build()
                            );
                            report.add(text().content("    ").append(generateDiskUsageDiagram(usedSpace, totalSpace, 40)).build());
                            report.add(empty());
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

    public static TextComponent formatTps(double tps) {
        TextColor color;
        if (tps > 18.0) {
            color = GREEN;
        } else if (tps > 16.0) {
            color = YELLOW;
        } else {
            color = RED;
        }

        return text((tps > 20.0 ? "*" : "") + Math.min(Math.round(tps * 100.0) / 100.0, 20.0), color);
    }

    public static TextComponent formatTickDurations(RollingAverage average){
        return text()
                .append(formatTickDuration(average.getMin()))
                .append(text('/', GRAY))
                .append(formatTickDuration(average.getMedian()))
                .append(text('/', GRAY))
                .append(formatTickDuration(average.getPercentile(MSPT_95_PERCENTILE)))
                .append(text('/', GRAY))
                .append(formatTickDuration(average.getMax()))
                .build();
    }

    public static TextComponent formatTickDuration(double duration){
        TextColor color;
        if (duration >= 50d) {
            color = RED;
        } else if (duration >= 40d) {
            color = YELLOW;
        } else {
            color = GREEN;
        }

        return text(String.format("%.1f", duration), color);
    }

    public static TextComponent formatCpuUsage(double usage) {
        TextColor color;
        if (usage > 0.9) {
            color = RED;
        } else if (usage > 0.65) {
            color = YELLOW;
        } else {
            color = GREEN;
        }

        return text(FormatUtil.percent(usage, 1d), color);
    }

    private static TextComponent generateMemoryUsageDiagram(MemoryUsage usage, int length) {
        double used = usage.getUsed();
        double committed = usage.getCommitted();
        double max = usage.getMax();

        int usedChars = (int) ((used * length) / max);
        int committedChars = (int) ((committed * length) / max);

        TextComponent.Builder line = text().content(Strings.repeat("/", usedChars)).color(GRAY);
        if (committedChars > usedChars) {
            line.append(text(Strings.repeat(" ", (committedChars - usedChars) - 1)));
            line.append(text("|", YELLOW));
        }
        if (length > committedChars) {
            line.append(text(Strings.repeat(" ", (length - committedChars))));
        }

        return text()
                .append(text("[", DARK_GRAY))
                .append(line.build())
                .append(text("]", DARK_GRAY))
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

        TextComponent.Builder line = text().content(Strings.repeat("/", collectionUsedChars)).color(GRAY);

        if (usedChars > collectionUsedChars) {
            line.append(text("|", RED));
            line.append(text(Strings.repeat("/", (usedChars - collectionUsedChars) - 1), GRAY));
        }
        if (committedChars > usedChars) {
            line.append(text(Strings.repeat(" ", (committedChars - usedChars) - 1)));
            line.append(text("|", YELLOW));
        }
        if (length > committedChars) {
            line.append(text(Strings.repeat(" ", (length - committedChars))));
        }

        return text()
                .append(text("[", DARK_GRAY))
                .append(line.build())
                .append(text("]", DARK_GRAY))
                .build();
    }

    private static TextComponent generateDiskUsageDiagram(double used, double max, int length) {
        int usedChars = (int) ((used * length) / max);
        String line = Strings.repeat("/", usedChars) + Strings.repeat(" ", length - usedChars);
        return text()
                .append(text("[", DARK_GRAY))
                .append(text(line, GRAY))
                .append(text("]", DARK_GRAY))
                .build();
    }

}

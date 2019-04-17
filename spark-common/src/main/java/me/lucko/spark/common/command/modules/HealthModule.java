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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HealthModule<S> implements CommandModule<S> {

    @Override
    public void registerCommands(Consumer<Command<S>> consumer) {
        consumer.accept(Command.<S>builder()
                        .aliases("tps")
                        .executor((platform, sender, resp, arguments) -> {
                            TpsCalculator tpsCalculator = platform.getTpsCalculator();
                            if (tpsCalculator != null) {
                                resp.replyPrefixed("TPS from last 5s, 10s, 1m, 5m, 15m:");
                                resp.replyPrefixed(" " + tpsCalculator.toFormattedString());
                            } else {
                                resp.replyPrefixed("Not supported!");
                            }
                        })
                        .tabCompleter(Command.TabCompleter.empty())
                        .build()
        );

        consumer.accept(Command.<S>builder()
                .aliases("healthreport", "health", "ht")
                .argumentUsage("memory", null)
                .executor((platform, sender, resp, arguments) -> {
                    resp.replyPrefixed("&7Generating server health report...");
                    platform.getPlugin().runAsync(() -> {
                        List<String> report = new ArrayList<>(15);
                        report.add("");

                        TpsCalculator tpsCalculator = platform.getTpsCalculator();
                        if (tpsCalculator != null) {
                            report.add("&8&l>&6 TPS from last 5s, 10s, 1m, 5m, 15m:");
                            report.add("    " + tpsCalculator.toFormattedString());
                            report.add("");
                        }

                        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

                        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
                        report.add("&8&l>&6 Memory usage: ");
                        report.add("    &f" + formatBytes(heapUsage.getUsed()) + " &7/ &f" + formatBytes(heapUsage.getMax()) +
                                "   &7(&a" + percent(heapUsage.getUsed(), heapUsage.getMax()) + "&7)");
                        report.add("    " + generateMemoryUsageDiagram(heapUsage, 40));
                        report.add("");

                        if (arguments.boolFlag("memory")) {
                            MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();
                            report.add("&8&l>&6 Non-heap memory usage: ");
                            report.add("    &f" + formatBytes(nonHeapUsage.getUsed()));
                            report.add("");

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

                                report.add("&8&l>&6 " + memoryPool.getName() + " pool usage: ");
                                report.add("    &f" + formatBytes(usage.getUsed()) + " &7/ &f" + formatBytes(usage.getMax()) +
                                        "   &7(&a" + percent(usage.getUsed(), usage.getMax()) + "&7)");
                                report.add("    " + generateMemoryPoolDiagram(usage, collectionUsage,40));


                                if (collectionUsage != null) {
                                    report.add("     &c- &7Usage at last GC: &f" + formatBytes(collectionUsage.getUsed()));
                                }
                                report.add("");
                            }
                        }

                        double systemCpuLoad = CpuMonitor.getSystemCpuLoad();
                        double processCpuLoad = CpuMonitor.getProcessCpuLoad();

                        if (systemCpuLoad >= 0 || processCpuLoad >= 0) {
                            report.add("&8&l>&6 CPU usage: ");

                            if (systemCpuLoad >= 0) {
                                report.add("    &7System: &a" + percent(systemCpuLoad, 1.0d));
                                report.add("    " + generateCpuUsageDiagram(systemCpuLoad, 40));
                                report.add("");
                            }
                            if (processCpuLoad >= 0) {
                                report.add("    &7Process: &a" + percent(processCpuLoad, 1.0d));
                                report.add("    " + generateCpuUsageDiagram(processCpuLoad, 40));
                                report.add("");
                            }
                        }

                        try {
                            FileStore fileStore = Files.getFileStore(Paths.get("."));
                            long totalSpace = fileStore.getTotalSpace();
                            long usedSpace = totalSpace - fileStore.getUsableSpace();

                            report.add("&8&l>&6 Disk usage: ");
                            report.add("    &f" + formatBytes(usedSpace) + " &7/ &f" + formatBytes(totalSpace) +
                                    "   &7(&a" + percent(usedSpace, totalSpace) + "&7)");
                            report.add("    " + generateDiskUsageDiagram(usedSpace, totalSpace, 40));
                            report.add("");
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

    private static String percent(double value, double max) {
        double percent = (value * 100d) / max;
        return (int) percent + "%";
    }

    private static String generateMemoryUsageDiagram(MemoryUsage usage, int length) {
        double used = usage.getUsed();
        double committed = usage.getCommitted();
        double max = usage.getMax();

        int usedChars = (int) ((used * length) / max);
        int committedChars = (int) ((committed * length) / max);

        String line = "&7" + Strings.repeat("/", usedChars);
        if (committedChars > usedChars) {
            line += Strings.repeat(" ", (committedChars - usedChars) - 1) + "&e|";
        }
        if (length > committedChars) {
            line += Strings.repeat(" ", (length - committedChars));
        }

        return "&8[" + line + "&8]";
    }

    private static String generateMemoryPoolDiagram(MemoryUsage usage, MemoryUsage collectionUsage, int length) {
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

        String line = "&7" + Strings.repeat("/", collectionUsedChars);
        if (usedChars > collectionUsedChars) {
            line += "&c|&7" + Strings.repeat("/", (usedChars - collectionUsedChars) - 1);
        }
        if (committedChars > usedChars) {
            line += Strings.repeat(" ", (committedChars - usedChars) - 1) + "&e|";
        }
        if (length > committedChars) {
            line += Strings.repeat(" ", (length - committedChars));
        }

        return "&8[" + line + "&8]";
    }

    private static String generateCpuUsageDiagram(double usage, int length) {
        int usedChars = (int) ((usage * length));

        String line = "&7" + Strings.repeat("/", usedChars) + Strings.repeat(" ", length - usedChars);
        return "&8[" + line + "&8]";
    }

    private static String generateDiskUsageDiagram(double used, double max, int length) {
        int usedChars = (int) ((used * length) / max);

        String line = "&7" + Strings.repeat("/", usedChars) + Strings.repeat(" ", length - usedChars);
        return "&8[" + line + "&8]";
    }

    private static String formatBytes(long bytes) {
        if (bytes == 0) {
            return "0 bytes";
        }
        String[] sizes = new String[]{"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        int sizeIndex = (int) (Math.log(bytes) / Math.log(1024));
        return String.format("%.1f", bytes / Math.pow(1024, sizeIndex)) + " " + sizes[sizeIndex];
    }

}

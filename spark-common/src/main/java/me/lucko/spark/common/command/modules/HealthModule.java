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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.activitylog.Activity;
import me.lucko.spark.common.command.Arguments;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.monitor.cpu.CpuMonitor;
import me.lucko.spark.common.monitor.disk.DiskUsage;
import me.lucko.spark.common.monitor.net.Direction;
import me.lucko.spark.common.monitor.net.NetworkInterfaceAverages;
import me.lucko.spark.common.monitor.net.NetworkMonitor;
import me.lucko.spark.common.monitor.ping.PingStatistics;
import me.lucko.spark.common.monitor.ping.PingSummary;
import me.lucko.spark.common.monitor.tick.TickStatistics;
import me.lucko.spark.common.platform.SparkMetadata;
import me.lucko.spark.common.sampler.Sampler;
import me.lucko.spark.common.util.FormatUtil;
import me.lucko.spark.common.util.MediaTypes;
import me.lucko.spark.common.util.RollingAverage;
import me.lucko.spark.common.util.StatisticFormatter;
import me.lucko.spark.proto.SparkProtos;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.logging.Level;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class HealthModule implements CommandModule {

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("tps", "cpu")
                .executor(HealthModule::tps)
                .tabCompleter(Command.TabCompleter.empty())
                .build()
        );

        consumer.accept(Command.builder()
                .aliases("ping")
                .argumentUsage("player", "username")
                .executor(HealthModule::ping)
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--player"))
                .build()
        );

        consumer.accept(Command.builder()
                .aliases("healthreport", "health", "ht")
                .argumentUsage("upload", null)
                .argumentUsage("memory", null)
                .argumentUsage("network", null)
                .executor(HealthModule::healthReport)
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--upload", "--memory", "--network"))
                .build()
        );
    }

    private static void tps(SparkPlatform platform, CommandSender sender, CommandResponseHandler resp, Arguments arguments) {
        TickStatistics tickStatistics = platform.getTickStatistics();
        if (tickStatistics != null) {
            resp.replyPrefixed(text("TPS from last 5s, 10s, 1m, 5m, 15m:"));
            resp.replyPrefixed(text()
                    .content(" ")
                    .append(StatisticFormatter.formatTps(tickStatistics.tps5Sec(), tickStatistics.gameTargetTps())).append(text(", "))
                    .append(StatisticFormatter.formatTps(tickStatistics.tps10Sec(), tickStatistics.gameTargetTps())).append(text(", "))
                    .append(StatisticFormatter.formatTps(tickStatistics.tps1Min(), tickStatistics.gameTargetTps())).append(text(", "))
                    .append(StatisticFormatter.formatTps(tickStatistics.tps5Min(), tickStatistics.gameTargetTps())).append(text(", "))
                    .append(StatisticFormatter.formatTps(tickStatistics.tps15Min(), tickStatistics.gameTargetTps()))
                    .build()
            );
            resp.replyPrefixed(empty());

            if (tickStatistics.isDurationSupported()) {
                resp.replyPrefixed(text("Tick durations (min/med/95%ile/max ms) from last 10s, 1m:"));
                resp.replyPrefixed(text()
                        .content(" ")
                        .append(StatisticFormatter.formatTickDurations(tickStatistics.duration10Sec(), tickStatistics.gameMaxIdealDuration())).append(text(";  "))
                        .append(StatisticFormatter.formatTickDurations(tickStatistics.duration1Min(), tickStatistics.gameMaxIdealDuration()))
                        .build()
                );
                resp.replyPrefixed(empty());
            }
        }

        resp.replyPrefixed(text("CPU usage from last 10s, 1m, 15m:"));
        resp.replyPrefixed(text()
                .content(" ")
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(text(", "))
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(text(", "))
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
                .append(text("  (system)", DARK_GRAY))
                .build()
        );
        resp.replyPrefixed(text()
                .content(" ")
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(text(", "))
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(text(", "))
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
                .append(text("  (process)", DARK_GRAY))
                .build()
        );
    }

    private static void ping(SparkPlatform platform, CommandSender sender, CommandResponseHandler resp, Arguments arguments) {
        PingStatistics pingStatistics = platform.getPingStatistics();
        if (pingStatistics == null) {
            resp.replyPrefixed(text("Ping data is not available on this platform."));
            return;
        }

        // lookup for specific player
        Set<String> players = arguments.stringFlag("player");
        if (!players.isEmpty()) {
            for (String player : players) {
                PingStatistics.PlayerPing playerPing = pingStatistics.query(player);
                if (playerPing == null) {
                    resp.replyPrefixed(text("Ping data is not available for '" + player + "'."));
                } else {
                    resp.replyPrefixed(text()
                            .content("Player ")
                            .append(text(playerPing.name(), WHITE))
                            .append(text(" has "))
                            .append(StatisticFormatter.formatPingRtt(playerPing.ping()))
                            .append(text(" ms ping."))
                            .build()
                    );
                }
            }
            return;
        }

        PingSummary summary = pingStatistics.currentSummary();
        RollingAverage average = pingStatistics.getPingAverage();

        if (summary.total() == 0 && average.getSamples() == 0) {
            resp.replyPrefixed(text("There is not enough data to show ping averages yet. Please try again later."));
            return;
        }

        resp.replyPrefixed(text("Average Pings (min/med/95%ile/max ms) from now, last 15m:"));
        resp.replyPrefixed(text()
                .content(" ")
                .append(StatisticFormatter.formatPingRtts(summary.min(), summary.median(), summary.percentile95th(), summary.max())).append(text(";  "))
                .append(StatisticFormatter.formatPingRtts(average.min(), average.median(), average.percentile95th(), average.max()))
                .build()
        );
    }

    private static void healthReport(SparkPlatform platform, CommandSender sender, CommandResponseHandler resp, Arguments arguments) {
        resp.replyPrefixed(text("Generating server health report..."));

        if (arguments.boolFlag("upload")) {
            uploadHealthReport(platform, sender, resp, arguments);
            return;
        }

        List<Component> report = new LinkedList<>();
        report.add(empty());

        TickStatistics tickStatistics = platform.getTickStatistics();
        if (tickStatistics != null) {
            addTickStats(report, tickStatistics);
        }

        addCpuStats(report);

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        addBasicMemoryStats(report, memoryMXBean);

        if (arguments.boolFlag("memory")) {
            addDetailedMemoryStats(report, memoryMXBean);
        }

        addNetworkStats(report, arguments.boolFlag("network"));

        addDiskStats(report);

        resp.reply(report);
    }

    private static void uploadHealthReport(SparkPlatform platform, CommandSender sender, CommandResponseHandler resp, Arguments arguments) {
        SparkProtos.HealthMetadata.Builder metadata = SparkProtos.HealthMetadata.newBuilder();
        SparkMetadata.gather(platform, sender.toData(), platform.getStartupGcStatistics()).writeTo(metadata);

        SparkProtos.HealthData.Builder data = SparkProtos.HealthData.newBuilder()
                .setMetadata(metadata);

        Sampler activeSampler = platform.getSamplerContainer().getActiveSampler();
        if (activeSampler != null) {
            data.putAllTimeWindowStatistics(activeSampler.exportWindowStatistics());
        }

        try {
            String key = platform.getBytebinClient().postContent(data.build(), MediaTypes.SPARK_HEALTH_MEDIA_TYPE).key();
            String url = platform.getViewerUrl() + key;

            resp.broadcastPrefixed(text("Health report:", GOLD));
            resp.broadcast(text()
                    .content(url)
                    .color(GRAY)
                    .clickEvent(ClickEvent.openUrl(url))
                    .build()
            );

            platform.getActivityLog().addToLog(Activity.urlActivity(resp.senderData(), System.currentTimeMillis(), "Health report", url));
        } catch (Exception e) {
            resp.broadcastPrefixed(text("An error occurred whilst uploading the data.", RED));
            platform.getPlugin().log(Level.SEVERE, "An error occurred whilst uploading data", e);
        }
    }

    private static void addTickStats(List<Component> report, TickStatistics tickStatistics) {
        report.add(text()
                .append(text(">", DARK_GRAY, BOLD))
                .append(space())
                .append(text("TPS from last 5s, 10s, 1m, 5m, 15m:", GOLD))
                .build()
        );
        report.add(text()
                .content("    ")
                .append(StatisticFormatter.formatTps(tickStatistics.tps5Sec(), tickStatistics.gameTargetTps())).append(text(", "))
                .append(StatisticFormatter.formatTps(tickStatistics.tps10Sec(), tickStatistics.gameTargetTps())).append(text(", "))
                .append(StatisticFormatter.formatTps(tickStatistics.tps1Min(), tickStatistics.gameTargetTps())).append(text(", "))
                .append(StatisticFormatter.formatTps(tickStatistics.tps5Min(), tickStatistics.gameTargetTps())).append(text(", "))
                .append(StatisticFormatter.formatTps(tickStatistics.tps15Min(), tickStatistics.gameTargetTps()))
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
                    .append(StatisticFormatter.formatTickDurations(tickStatistics.duration10Sec(), tickStatistics.gameMaxIdealDuration())).append(text("; "))
                    .append(StatisticFormatter.formatTickDurations(tickStatistics.duration1Min(), tickStatistics.gameMaxIdealDuration()))
                    .build()
            );
            report.add(empty());
        }
    }

    private static void addCpuStats(List<Component> report) {
        report.add(text()
                .append(text(">", DARK_GRAY, BOLD))
                .append(space())
                .append(text("CPU usage from last 10s, 1m, 15m:", GOLD))
                .build()
        );
        report.add(text()
                .content("    ")
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad10SecAvg())).append(text(", "))
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad1MinAvg())).append(text(", "))
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.systemLoad15MinAvg()))
                .append(text("  (system)", DARK_GRAY))
                .build()
        );
        report.add(text()
                .content("    ")
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad10SecAvg())).append(text(", "))
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad1MinAvg())).append(text(", "))
                .append(StatisticFormatter.formatCpuUsage(CpuMonitor.processLoad15MinAvg()))
                .append(text("  (process)", DARK_GRAY))
                .build()
        );
        report.add(empty());
    }

    private static void addBasicMemoryStats(List<Component> report, MemoryMXBean memoryMXBean) {
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
        report.add(text().content("    ").append(StatisticFormatter.generateMemoryUsageDiagram(heapUsage, 60)).build());
        report.add(empty());
    }

    private static void addDetailedMemoryStats(List<Component> report, MemoryMXBean memoryMXBean) {
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
            report.add(text().content("    ").append(StatisticFormatter.generateMemoryPoolDiagram(usage, collectionUsage, 60)).build());

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

    private static void addNetworkStats(List<Component> report, boolean detailed) {
        List<Component> averagesReport = new LinkedList<>();

        for (Map.Entry<String, NetworkInterfaceAverages> ent : NetworkMonitor.systemAverages().entrySet()) {
            String interfaceName = ent.getKey();
            NetworkInterfaceAverages averages = ent.getValue();

            for (Direction direction : Direction.values()) {
                long bytesPerSec = (long) averages.bytesPerSecond(direction).mean();
                long packetsPerSec = (long) averages.packetsPerSecond(direction).mean();

                if (detailed || bytesPerSec > 0 || packetsPerSec > 0) {
                    averagesReport.add(text()
                            .color(GRAY)
                            .content("    ")
                            .append(FormatUtil.formatBytes(bytesPerSec, GREEN, "/s"))
                            .append(text(" / "))
                            .append(text(String.format(Locale.ENGLISH, "%,d", packetsPerSec), WHITE))
                            .append(text(" pps "))
                            .append(text().color(DARK_GRAY)
                                    .append(text('('))
                                    .append(text(interfaceName + " " + direction.abbrev(), WHITE))
                                    .append(text(')'))
                            )
                            .build()
                    );
                }
            }
        }

        if (!averagesReport.isEmpty()) {
            report.add(text()
                    .append(text(">", DARK_GRAY, BOLD))
                    .append(space())
                    .append(text("Network usage: (system, last 15m)", GOLD))
                    .build()
            );
            report.addAll(averagesReport);
            report.add(empty());
        }
    }

    private static void addDiskStats(List<Component> report) {
        long total = DiskUsage.getTotal();
        long used = DiskUsage.getUsed();
        
        if (total == 0 || used == 0) {
            return;
        }

        report.add(text()
                .append(text(">", DARK_GRAY, BOLD))
                .append(space())
                .append(text("Disk usage:", GOLD))
                .build()
        );
        report.add(text()
                .content("    ")
                .append(text(FormatUtil.formatBytes(used), WHITE))
                .append(space())
                .append(text("/", GRAY))
                .append(space())
                .append(text(FormatUtil.formatBytes(total), WHITE))
                .append(text("   "))
                .append(text("(", GRAY))
                .append(text(FormatUtil.percent(used, total), GREEN))
                .append(text(")", GRAY))
                .build()
        );
        report.add(text().content("    ").append(StatisticFormatter.generateDiskUsageDiagram(used, total, 60)).build());
        report.add(empty());
    }

}

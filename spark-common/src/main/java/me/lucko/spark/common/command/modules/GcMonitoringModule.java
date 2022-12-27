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

import com.sun.management.GarbageCollectionNotificationInfo;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.monitor.memory.GarbageCollectionMonitor;
import me.lucko.spark.common.monitor.memory.GarbageCollectorStatistics;
import me.lucko.spark.common.util.FormatUtil;

import net.kyori.adventure.text.Component;

import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.empty;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class GcMonitoringModule implements CommandModule {
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    /** The gc monitoring instance currently running, if any */
    private ReportingGcMonitor activeGcMonitor = null;

    @Override
    public void close() {
        if (this.activeGcMonitor != null) {
            this.activeGcMonitor.close();
            this.activeGcMonitor = null;
        }
    }

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("gc")
                .executor((platform, sender, resp, arguments) -> {
                    resp.replyPrefixed(text("Calculating GC statistics..."));

                    List<Component> report = new LinkedList<>();
                    report.add(empty());
                    report.add(text()
                            .append(text(">", DARK_GRAY, BOLD))
                            .append(space())
                            .append(text("Garbage Collector statistics", GOLD))
                            .build()
                    );

                    long serverUptime = System.currentTimeMillis() - platform.getServerNormalOperationStartTime();
                    Map<String, GarbageCollectorStatistics> collectorStats = GarbageCollectorStatistics.pollStatsSubtractInitial(platform.getStartupGcStatistics());

                    for (Map.Entry<String, GarbageCollectorStatistics> collector : collectorStats.entrySet()) {
                        String collectorName = collector.getKey();
                        double collectionTime = collector.getValue().getCollectionTime();
                        long collectionCount = collector.getValue().getCollectionCount();

                        report.add(empty());

                        if (collectionCount == 0) {
                            report.add(text()
                                    .content("    ")
                                    .append(text(collectorName + " collector:", GRAY))
                                    .build()
                            );
                            report.add(text()
                                    .content("      ")
                                    .append(text(0, WHITE))
                                    .append(text(" collections", GRAY))
                                    .build()
                            );
                            continue;
                        }

                        double averageCollectionTime = collectionTime / collectionCount;
                        double averageFrequency = (serverUptime - collectionTime) / collectionCount;

                        report.add(text()
                                .content("    ")
                                .append(text(collectorName + " collector:", GRAY))
                                .build()
                        );
                        report.add(text()
                                .content("      ")
                                .append(text(DF.format(averageCollectionTime), GOLD))
                                .append(text(" ms avg", GRAY))
                                .append(text(", ", DARK_GRAY))
                                .append(text(collectionCount, WHITE))
                                .append(text(" total collections", GRAY))
                                .build()
                        );
                        report.add(text()
                                .content("      ")
                                .append(text(FormatUtil.formatSeconds((long) averageFrequency / 1000), WHITE))
                                .append(text(" avg frequency", GRAY))
                                .build()
                        );
                    }

                    if (collectorStats.isEmpty()) {
                        resp.replyPrefixed(text("No garbage collectors are reporting data."));
                    } else {
                        resp.reply(report);
                    }
                })
                .build()
        );

        consumer.accept(Command.builder()
                .aliases("gcmonitor", "gcmonitoring")
                .executor((platform, sender, resp, arguments) -> {
                    if (this.activeGcMonitor == null) {
                        this.activeGcMonitor = new ReportingGcMonitor(platform, resp);
                        resp.broadcastPrefixed(text("GC monitor enabled."));
                    } else {
                        close();
                        resp.broadcastPrefixed(text("GC monitor disabled."));
                    }
                })
                .build()
        );
    }

    private static class ReportingGcMonitor extends GarbageCollectionMonitor implements GarbageCollectionMonitor.Listener {
        private final SparkPlatform platform;
        private final CommandResponseHandler resp;

        ReportingGcMonitor(SparkPlatform platform, CommandResponseHandler resp) {
            this.platform = platform;
            this.resp = resp;
            addListener(this);
        }

        @Override
        public void onGc(GarbageCollectionNotificationInfo data) {
            String gcType = GarbageCollectionMonitor.getGcType(data);
            String gcCause = data.getGcCause() != null ? " (cause = " + data.getGcCause() + ")" : "";

            Map<String, MemoryUsage> beforeUsages = data.getGcInfo().getMemoryUsageBeforeGc();
            Map<String, MemoryUsage> afterUsages = data.getGcInfo().getMemoryUsageAfterGc();

            this.platform.getPlugin().executeAsync(() -> {
                List<Component> report = new LinkedList<>();
                report.add(CommandResponseHandler.applyPrefix(
                        text()
                                .color(GRAY)
                                .append(text(gcType + " "))
                                .append(text("GC", RED))
                                .append(text(" lasting "))
                                .append(text(DF.format(data.getGcInfo().getDuration()), GOLD))
                                .append(text(" ms." + gcCause))
                                .build()
                ));

                for (Map.Entry<String, MemoryUsage> entry : afterUsages.entrySet()) {
                    String type = entry.getKey();
                    MemoryUsage after = entry.getValue();
                    MemoryUsage before = beforeUsages.get(type);

                    if (before == null) {
                        continue;
                    }

                    long diff = before.getUsed() - after.getUsed();
                    if (diff == 0) {
                        continue;
                    }

                    if (diff > 0) {
                        report.add(text()
                                .content("  ")
                                .append(text(FormatUtil.formatBytes(diff), GOLD))
                                .append(text(" freed from ", DARK_GRAY))
                                .append(text(type, GRAY))
                                .build()
                        );
                        report.add(text()
                                .content("    ")
                                .append(text(FormatUtil.formatBytes(before.getUsed()), GRAY))
                                .append(text(" → ", DARK_GRAY))
                                .append(text(FormatUtil.formatBytes(after.getUsed()), GRAY))
                                .append(space())
                                .append(text("(", DARK_GRAY))
                                .append(text(FormatUtil.percent(diff, before.getUsed()), WHITE))
                                .append(text(")", DARK_GRAY))
                                .build()
                        );
                    } else {
                        report.add(text()
                                .content("  ")
                                .append(text(FormatUtil.formatBytes(-diff), GOLD))
                                .append(text(" moved to ", DARK_GRAY))
                                .append(text(type, GRAY))
                                .build()
                        );
                        report.add(text()
                                .content("    ")
                                .append(text(FormatUtil.formatBytes(before.getUsed()), GRAY))
                                .append(text(" → ", DARK_GRAY))
                                .append(text(FormatUtil.formatBytes(after.getUsed()), GRAY))
                                .build()
                        );
                    }
                }

                this.resp.broadcast(report);
            });
        }

    }
}

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
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.monitor.tick.TickMonitor;
import me.lucko.spark.common.monitor.tick.TickMonitor.ReportPredicate;
import me.lucko.spark.common.sampler.tick.TickHook;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.*;

public class TickMonitoringModule implements CommandModule {

    /** The tick hook instance currently running, if any */
    private TickHook tickHook = null;
    private ReportingTickMonitor activeTickMonitor = null;

    @Override
    public void close() {
        if (this.activeTickMonitor != null) {
            this.tickHook.removeCallback(this.activeTickMonitor);
            this.activeTickMonitor.close();
            this.activeTickMonitor = null;
        }
    }

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("tickmonitor", "tickmonitoring")
                .argumentUsage("threshold", "percentage increase")
                .argumentUsage("threshold-tick", "tick duration")
                .argumentUsage("without-gc", null)
                .executor((platform, sender, resp, arguments) -> {
                    if (this.tickHook == null) {
                        this.tickHook = platform.getTickHook();
                    }
                    if (this.tickHook == null) {
                        resp.replyPrefixed(text("Not supported!", NamedTextColor.RED));
                        return;
                    }

                    if (this.activeTickMonitor == null) {
                        ReportPredicate reportPredicate;

                        int threshold;
                        if ((threshold = arguments.intFlag("threshold")) != -1) {
                            reportPredicate = new ReportPredicate.PercentageChangeGt(threshold);
                        } else if ((threshold = arguments.intFlag("threshold-tick")) != -1) {
                            reportPredicate = new ReportPredicate.DurationGt(threshold);
                        } else {
                            reportPredicate = new ReportPredicate.PercentageChangeGt(100);
                        }

                        this.activeTickMonitor = new ReportingTickMonitor(platform, resp, this.tickHook, reportPredicate, !arguments.boolFlag("without-gc"));
                        this.tickHook.addCallback(this.activeTickMonitor);
                    } else {
                        close();
                        resp.broadcastPrefixed(text("Tick monitor disabled."));
                    }
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--threshold", "--threshold-tick", "--without-gc"))
                .build()
        );
    }

    private static class ReportingTickMonitor extends TickMonitor {
        private final CommandResponseHandler resp;

        ReportingTickMonitor(SparkPlatform platform, CommandResponseHandler resp, TickHook tickHook, ReportPredicate reportPredicate, boolean monitorGc) {
            super(platform, tickHook, reportPredicate, monitorGc);
            this.resp = resp;
        }

        @Override
        protected void sendMessage(Component message) {
            this.resp.broadcastPrefixed(message);
        }
    }
}

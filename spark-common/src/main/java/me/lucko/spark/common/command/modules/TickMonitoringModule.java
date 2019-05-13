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

import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.monitor.tick.TickMonitor;
import me.lucko.spark.common.sampler.TickCounter;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import java.util.function.Consumer;

public class TickMonitoringModule implements CommandModule {

    /** The tick monitor instance currently running, if any */
    private TickCounter tickCounter = null;
    private ReportingTickMonitor activeTickMonitor = null;

    @Override
    public void close() {
        if (this.activeTickMonitor != null) {
            this.tickCounter.removeTickTask(this.activeTickMonitor);
            this.activeTickMonitor.close();
            this.activeTickMonitor = null;
        }
    }

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("tickmonitoring")
                .argumentUsage("threshold", "percentage increase")
                .argumentUsage("without-gc", null)
                .executor((platform, sender, resp, arguments) -> {
                    if (this.tickCounter == null) {
                        this.tickCounter = platform.getTickCounter();
                    }
                    if (this.tickCounter == null) {
                        resp.replyPrefixed(TextComponent.of("Not supported!", TextColor.RED));
                        return;
                    }

                    if (this.activeTickMonitor == null) {
                        int threshold = arguments.intFlag("threshold");
                        if (threshold == -1) {
                            threshold = 100;
                        }

                        this.activeTickMonitor = new ReportingTickMonitor(resp, this.tickCounter, threshold, !arguments.boolFlag("without-gc"));
                        this.tickCounter.addTickTask(this.activeTickMonitor);
                    } else {
                        close();
                        resp.broadcastPrefixed(TextComponent.of("Tick monitor disabled."));
                    }
                })
                .tabCompleter((platform, sender, arguments) -> TabCompleter.completeForOpts(arguments, "--threshold", "--without-gc"))
                .build()
        );
    }

    private class ReportingTickMonitor extends TickMonitor {
        private final CommandResponseHandler resp;

        ReportingTickMonitor(CommandResponseHandler resp, TickCounter tickCounter, int percentageChangeThreshold, boolean monitorGc) {
            super(tickCounter, percentageChangeThreshold, monitorGc);
            this.resp = resp;
        }

        @Override
        protected void sendMessage(Component message) {
            this.resp.broadcastPrefixed(message);
        }
    }
}

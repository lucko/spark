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
import me.lucko.spark.monitor.TickMonitor;
import me.lucko.spark.sampler.TickCounter;

import java.util.function.Consumer;

public class MonitoringModule<S> implements CommandModule<S> {

    /** The tick monitor instance currently running, if any */
    private ReportingTickMonitor activeTickMonitor = null;

    @Override
    public void registerCommands(Consumer<Command<S>> consumer) {
        consumer.accept(Command.<S>builder()
                .aliases("monitoring")
                .executor((platform, sender, arguments) -> {
                    if (this.activeTickMonitor == null) {

                        int threshold = arguments.intFlag("threshold");
                        if (threshold == -1) {
                            threshold = 100;
                        }

                        try {
                            TickCounter tickCounter = platform.newTickCounter();
                            this.activeTickMonitor = new ReportingTickMonitor(platform, tickCounter, threshold);
                        } catch (UnsupportedOperationException e) {
                            platform.sendPrefixedMessage(sender, "&cNot supported!");
                        }
                    } else {
                        this.activeTickMonitor.close();
                        this.activeTickMonitor = null;
                        platform.sendPrefixedMessage("&7Tick monitor disabled.");
                    }
                })
                .tabCompleter((platform, sender, arguments) -> {
                    return null;
                })
                .build()
        );
    }

    private class ReportingTickMonitor extends TickMonitor {
        private final SparkPlatform<S> platform;

        ReportingTickMonitor(SparkPlatform<S> platform, TickCounter tickCounter, int percentageChangeThreshold) {
            super(tickCounter, percentageChangeThreshold);
            this.platform = platform;
        }

        @Override
        protected void sendMessage(String message) {
            platform.sendPrefixedMessage(message);
        }
    }
}

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
import me.lucko.spark.common.monitor.tick.TpsCalculator;

import java.util.function.Consumer;

public class MonitoringModule<S> implements CommandModule<S> {

    @Override
    public void registerCommands(Consumer<Command<S>> consumer) {
        consumer.accept(Command.<S>builder()
                .aliases("tps")
                .executor((platform, sender, resp, arguments) -> {
                    TpsCalculator tpsCalculator = platform.getTpsCalculator();
                    if (tpsCalculator == null) {
                        resp.replyPrefixed("TPS data is not available.");
                        return;
                    }

                    String formattedTpsString = tpsCalculator.toFormattedString();
                    resp.replyPrefixed("TPS from last 5s, 10s, 1m, 5m, 15m");
                    resp.replyPrefixed(formattedTpsString);
                })
                .tabCompleter(Command.TabCompleter.empty())
                .build()
        );
    }

}

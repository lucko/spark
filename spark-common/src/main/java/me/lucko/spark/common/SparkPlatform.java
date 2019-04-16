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

package me.lucko.spark.common;

import com.google.common.collect.ImmutableList;
import me.lucko.spark.common.command.Arguments;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.command.modules.HealthModule;
import me.lucko.spark.common.command.modules.MemoryModule;
import me.lucko.spark.common.command.modules.SamplerModule;
import me.lucko.spark.common.command.modules.TickMonitoringModule;
import me.lucko.spark.common.command.tabcomplete.CompletionSupplier;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.monitor.tick.TpsCalculator;
import me.lucko.spark.common.sampler.TickCounter;
import me.lucko.spark.common.util.BytebinClient;
import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract spark implementation used by all platforms.
 *
 * @param <S> the sender (e.g. CommandSender) type used by the platform
 */
public class SparkPlatform<S> {

    /** The URL of the viewer frontend */
    public static final String VIEWER_URL = "https://sparkprofiler.github.io/#";
    /** The shared okhttp client */
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient();
    /** The bytebin instance used by the platform */
    public static final BytebinClient BYTEBIN_CLIENT = new BytebinClient(OK_HTTP_CLIENT, "https://bytebin.lucko.me/", "spark-plugin");

    private final List<Command<S>> commands;
    private final SparkPlugin<S> plugin;

    private final TickCounter tickCounter;
    private final TpsCalculator tpsCalculator;

    public SparkPlatform(SparkPlugin<S> plugin) {
        this.plugin = plugin;

        ImmutableList.Builder<Command<S>> commandsBuilder = ImmutableList.builder();
        new SamplerModule<S>().registerCommands(commandsBuilder::add);
        new HealthModule<S>().registerCommands(commandsBuilder::add);
        new TickMonitoringModule<S>().registerCommands(commandsBuilder::add);
        new MemoryModule<S>().registerCommands(commandsBuilder::add);
        this.commands = commandsBuilder.build();

        this.tickCounter = plugin.createTickCounter();
        this.tpsCalculator = this.tickCounter != null ? new TpsCalculator() : null;
    }

    public void enable() {
        if (this.tickCounter != null) {
            this.tickCounter.addTickTask(this.tpsCalculator);
            this.tickCounter.start();
        }
    }

    public void disable() {
        if (this.tickCounter != null) {
            this.tickCounter.close();
        }
    }

    public SparkPlugin<S> getPlugin() {
        return this.plugin;
    }

    public TickCounter getTickCounter() {
        return this.tickCounter;
    }

    public TpsCalculator getTpsCalculator() {
        return this.tpsCalculator;
    }

    public void executeCommand(S sender, String[] args) {
        CommandResponseHandler<S> resp = new CommandResponseHandler<>(this, sender);
        if (args.length == 0) {
            sendUsage(resp);
            return;
        }

        ArrayList<String> rawArgs = new ArrayList<>(Arrays.asList(args));
        String alias = rawArgs.remove(0).toLowerCase();

        for (Command<S> command : this.commands) {
            if (command.aliases().contains(alias)) {
                try {
                    command.executor().execute(this, sender, resp, new Arguments(rawArgs));
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    resp.replyPrefixed("&c" + e.getMessage());
                }
                return;
            }
        }

        sendUsage(resp);
    }

    public List<String> tabCompleteCommand(S sender, String[] args) {
        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        if (args.length <= 1) {
            List<String> mainCommands = this.commands.stream().map(c -> c.aliases().get(0)).collect(Collectors.toList());
            return TabCompleter.create()
                    .at(0, CompletionSupplier.startsWith(mainCommands))
                    .complete(arguments);
        }

        String alias = arguments.remove(0);
        for (Command<S> command : this.commands) {
            if (command.aliases().contains(alias)) {
                return command.tabCompleter().completions(this, sender, arguments);
            }
        }

        return Collections.emptyList();
    }

    private void sendUsage(CommandResponseHandler<S> sender) {
        sender.replyPrefixed("&fspark &7v" + getPlugin().getVersion());
        for (Command<S> command : this.commands) {
            sender.reply("&6&l> &7/" + getPlugin().getLabel() + " " + command.aliases().get(0));
            for (Command.ArgumentInfo arg : command.arguments()) {
                if (arg.requiresParameter()) {
                    sender.reply("       &8[&7--" + arg.argumentName() + "&8 <" + arg.parameterDescription() + ">]");
                } else {
                    sender.reply("       &8[&7--" + arg.argumentName() + "]");
                }
            }
        }
    }

}

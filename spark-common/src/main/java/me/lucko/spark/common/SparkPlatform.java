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
import me.lucko.spark.common.command.modules.HeapModule;
import me.lucko.spark.common.command.modules.MonitoringModule;
import me.lucko.spark.common.command.modules.SamplerModule;
import me.lucko.spark.common.command.tabcomplete.CompletionSupplier;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.sampler.ThreadDumper;
import me.lucko.spark.sampler.TickCounter;
import me.lucko.spark.util.BytebinClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Abstract command handling class used by all platforms.
 *
 * @param <S> the sender (e.g. CommandSender) type used by the platform
 */
public abstract class SparkPlatform<S> {

    /** The URL of the viewer frontend */
    public static final String VIEWER_URL = "https://sparkprofiler.github.io/#";
    /** The bytebin instance used by the platform */
    public static final BytebinClient BYTEBIN_CLIENT = new BytebinClient("https://bytebin.lucko.me/", "spark-plugin");

    /** The prefix used in all messages */
    private static final String PREFIX = "&8[&fspark&8] &7";
    
    private static <T> List<Command<T>> prepareCommands() {
        ImmutableList.Builder<Command<T>> builder = ImmutableList.builder();
        new SamplerModule<T>().registerCommands(builder::add);
        new MonitoringModule<T>().registerCommands(builder::add);
        new HeapModule<T>().registerCommands(builder::add);
        return builder.build();
    }

    private final List<Command<S>> commands = prepareCommands();
    
    // abstract methods implemented by each platform
    public abstract String getVersion();
    public abstract String getLabel();
    public abstract void sendMessage(S sender, String message);
    public abstract void sendMessage(String message);
    public abstract void sendLink(String url);
    public abstract void runAsync(Runnable r);
    public abstract ThreadDumper getDefaultThreadDumper();
    public abstract TickCounter newTickCounter();

    public void sendPrefixedMessage(S sender, String message) {
        sendMessage(sender, PREFIX + message);
    }

    public void sendPrefixedMessage(String message) {
        sendMessage(PREFIX + message);
    }

    public void executeCommand(S sender, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return;
        }

        ArrayList<String> rawArgs = new ArrayList<>(Arrays.asList(args));
        String alias = rawArgs.remove(0).toLowerCase();

        for (Command<S> command : this.commands) {
            if (command.aliases().contains(alias)) {
                try {
                    command.executor().execute(this, sender, new Arguments(rawArgs));
                } catch (IllegalArgumentException e) {
                    sendMessage(sender, "&c" + e.getMessage());
                }
                return;
            }
        }

        sendUsage(sender);
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

    private void sendUsage(S sender) {
        sendPrefixedMessage(sender, "&fspark &7v" + getVersion());
        for (Command<S> command : this.commands) {
            sendMessage(sender, "&b&l> &7/" + getLabel() + " " + command.aliases().get(0));
            for (Command.ArgumentInfo arg : command.arguments()) {
                if (arg.requiresParameter()) {
                    sendMessage(sender, "       &8[&7--" + arg.argumentName() + "&8 <" + arg.parameterDescription() + ">]");
                } else {
                    sendMessage(sender, "       &8[&7--" + arg.argumentName() + "]");
                }
            }
        }
    }

}

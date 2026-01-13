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

package me.lucko.spark.common.command;

import com.google.common.collect.ImmutableList;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.modules.ActivityLogModule;
import me.lucko.spark.common.command.modules.GcMonitoringModule;
import me.lucko.spark.common.command.modules.HealthModule;
import me.lucko.spark.common.command.modules.HeapAnalysisModule;
import me.lucko.spark.common.command.modules.SamplerModule;
import me.lucko.spark.common.command.modules.TickMonitoringModule;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.command.tabcomplete.CompletionSupplier;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.util.config.Configuration;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;
import static net.kyori.adventure.text.format.TextDecoration.BOLD;

public class CommandManager implements AutoCloseable {
    private final SparkPlatform platform;

    private final boolean disableResponseBroadcast;
    private final List<CommandModule> modules;
    private final List<Command> commands;
    private final ReentrantLock executeLock = new ReentrantLock(true);

    public CommandManager(SparkPlatform platform, Configuration configuration) {
        this.platform = platform;

        this.disableResponseBroadcast = configuration.getBoolean("disableResponseBroadcast", false);

        this.modules = new ArrayList<>();
        this.modules.add(new SamplerModule());
        this.modules.add(new HealthModule());
        if (platform.getTickHook() != null) {
            this.modules.add(new TickMonitoringModule());
        }
        this.modules.add(new GcMonitoringModule());
        this.modules.add(new HeapAnalysisModule());
        this.modules.add(new ActivityLogModule());

        ImmutableList.Builder<Command> commandsBuilder = ImmutableList.builder();
        for (CommandModule module : this.modules) {
            module.registerCommands(commandsBuilder::add);
        }
        this.commands = commandsBuilder.build();
    }

    @Override
    public void close() {
        for (CommandModule module : this.modules) {
            module.close();
        }
    }

    public boolean shouldBroadcastResponse() {
        return !this.disableResponseBroadcast;
    }

    public List<Command> getCommands() {
        return this.commands;
    }

    private List<Command> getAvailableCommands(CommandSender sender) {
        if (sender.hasPermission("spark")) {
            return this.commands;
        }
        return this.commands.stream()
                .filter(c -> sender.hasPermission("spark." + c.primaryAlias()))
                .collect(Collectors.toList());
    }

    public Set<String> getAllSparkPermissions() {
        return Stream.concat(
                Stream.of("spark"),
                this.commands.stream()
                        .map(Command::primaryAlias)
                        .map(alias -> "spark." + alias)
        ).collect(Collectors.toSet());
    }

    public boolean hasPermissionForAnyCommand(CommandSender sender) {
        return !getAvailableCommands(sender).isEmpty();
    }

    public CompletableFuture<Void> executeCommand(CommandSender sender, String[] args) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicReference<Thread> executorThread = new AtomicReference<>();
        AtomicReference<Thread> timeoutThread = new AtomicReference<>();
        AtomicBoolean completed = new AtomicBoolean(false);

        SparkPlugin plugin = this.platform.getPlugin();

        // execute the command
        plugin.executeAsync(() -> {
            executorThread.set(Thread.currentThread());
            this.executeLock.lock();
            try {
                executeCommand0(sender, args);
                future.complete(null);
            } catch (Throwable e) {
                plugin.log(Level.SEVERE, "Exception occurred whilst executing a spark command", e);
                future.completeExceptionally(e);
            } finally {
                this.executeLock.unlock();
                executorThread.set(null);
                completed.set(true);

                Thread timeout = timeoutThread.get();
                if (timeout != null) {
                    timeout.interrupt();
                }
            }
        });

        // schedule a task to detect timeouts
        plugin.executeAsync(() -> {
            timeoutThread.set(Thread.currentThread());
            int warningIntervalSeconds = 5;

            try {
                if (completed.get()) {
                    return;
                }

                for (int i = 1; i <= 3; i++) {
                    try {
                        Thread.sleep(warningIntervalSeconds * 1000);
                    } catch (InterruptedException e) {
                        // ignore
                    }

                    if (completed.get()) {
                        return;
                    }

                    Thread executor = executorThread.get();
                    if (executor == null) {
                        plugin.log(Level.WARNING, "A command execution has not completed after " +
                                (i * warningIntervalSeconds) + " seconds but there is no executor present. Perhaps the executor shutdown?");
                        plugin.log(Level.WARNING, "If the command subsequently completes without any errors, this warning should be ignored. :)");

                    } else {
                        String stackTrace = Arrays.stream(executor.getStackTrace())
                                .map(el -> "  " + el)
                                .collect(Collectors.joining("\n"));

                        plugin.log(Level.WARNING, "A command execution has not completed after " +
                                (i * warningIntervalSeconds) + " seconds, it *might* be stuck. Trace: \n" + stackTrace);
                        plugin.log(Level.WARNING, "If the command subsequently completes without any errors, this warning should be ignored. :)");
                    }
                }
            } finally {
                timeoutThread.set(null);
            }
        });

        return future;
    }

    private void executeCommand0(CommandSender sender, String[] args) {
        CommandResponseHandler resp = new CommandResponseHandler(this.platform, sender);
        List<Command> commands = getAvailableCommands(sender);

        if (commands.isEmpty()) {
            resp.replyPrefixed(text("You do not have permission to use this command.", RED));
            return;
        }

        if (args.length == 0) {
            resp.replyPrefixed(text()
                    .append(text("spark", WHITE))
                    .append(space())
                    .append(text("v" + this.platform.getPlugin().getVersion(), GRAY))
                    .build()
            );

            String helpCmd = "/" + this.platform.getPlugin().getCommandName() + " help";
            resp.replyPrefixed(text()
                    .color(GRAY)
                    .append(text("Run "))
                    .append(text()
                            .content(helpCmd)
                            .color(WHITE)
                            .clickEvent(ClickEvent.runCommand(helpCmd))
                            .build()
                    )
                    .append(text(" to view usage information."))
                    .build()
            );
            return;
        }

        ArrayList<String> rawArgs = new ArrayList<>(Arrays.asList(args));
        String alias = rawArgs.remove(0).toLowerCase();

        for (Command command : commands) {
            if (command.aliases().contains(alias)) {
                resp.setCommandPrimaryAlias(command.primaryAlias());
                try {
                    command.executor().execute(this.platform, sender, resp, new Arguments(rawArgs, command.allowSubCommand()));
                } catch (Arguments.ParseException e) {
                    resp.replyPrefixed(text(e.getMessage(), RED));
                }
                return;
            }
        }

        sendUsage(commands, resp);
    }

    public List<String> tabCompleteCommand(CommandSender sender, String[] args) {
        List<Command> commands = getAvailableCommands(sender);
        if (commands.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> arguments = new ArrayList<>(Arrays.asList(args));

        if (args.length <= 1) {
            List<String> mainCommands = commands.stream()
                    .map(Command::primaryAlias)
                    .collect(Collectors.toList());

            return TabCompleter.create()
                    .at(0, CompletionSupplier.startsWith(mainCommands))
                    .complete(arguments);
        }

        String alias = arguments.remove(0);
        for (Command command : commands) {
            if (command.aliases().contains(alias)) {
                return command.tabCompleter().completions(this.platform, sender, arguments);
            }
        }

        return Collections.emptyList();
    }

    private void sendUsage(List<Command> commands, CommandResponseHandler sender) {
        sender.replyPrefixed(text()
                .append(text("spark", WHITE))
                .append(space())
                .append(text("v" + this.platform.getPlugin().getVersion(), GRAY))
                .build()
        );
        for (Command command : commands) {
            String usage = "/" + this.platform.getPlugin().getCommandName() + " " + command.primaryAlias();

            if (command.allowSubCommand()) {
                Map<String, List<Command.ArgumentInfo>> argumentsBySubCommand = command.arguments().stream()
                        .collect(Collectors.groupingBy(Command.ArgumentInfo::subCommandName, LinkedHashMap::new, Collectors.toList()));

                argumentsBySubCommand.forEach((subCommand, arguments) -> {
                    String subCommandUsage = usage + " " + subCommand;

                    sender.reply(text()
                            .append(text(">", GOLD, BOLD))
                            .append(space())
                            .append(text().content(subCommandUsage).color(GRAY).clickEvent(ClickEvent.suggestCommand(subCommandUsage)).build())
                            .build()
                    );

                    for (Command.ArgumentInfo arg : arguments) {
                        if (arg.argumentName().isEmpty()) {
                            continue;
                        }
                        sender.reply(arg.toComponent("      "));
                    }
                });
            } else {
                sender.reply(text()
                        .append(text(">", GOLD, BOLD))
                        .append(space())
                        .append(text().content(usage).color(GRAY).clickEvent(ClickEvent.suggestCommand(usage)).build())
                        .build()
                );

                for (Command.ArgumentInfo arg : command.arguments()) {
                    sender.reply(arg.toComponent("    "));
                }
            }
        }

        sender.reply(Component.empty());
        sender.replyPrefixed(text()
                .append(text("For full usage information, please go to: "))
                .append(text()
                        .content("https://spark.lucko.me/docs/Command-Usage")
                        .color(WHITE)
                        .clickEvent(ClickEvent.openUrl("https://spark.lucko.me/docs/Command-Usage"))
                        .build()
                )
                .build()
        );
    }
}

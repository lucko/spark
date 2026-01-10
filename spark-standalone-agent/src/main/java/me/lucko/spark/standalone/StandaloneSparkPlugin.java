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

package me.lucko.spark.standalone;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.util.SparkThreadFactory;
import me.lucko.spark.common.util.classfinder.ClassFinder;
import me.lucko.spark.common.util.classfinder.FallbackClassFinder;
import me.lucko.spark.common.util.classfinder.InstrumentationClassFinder;
import me.lucko.spark.standalone.remote.SshRemoteInterface;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.instrument.Instrumentation;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.stream.Stream;

public class StandaloneSparkPlugin implements SparkPlugin {
    private final Instrumentation instrumentation;
    private final Set<StandaloneCommandSender> senders;
    private final ScheduledExecutorService scheduler;
    private final SparkPlatform platform;
    private final SshRemoteInterface remoteInterface;

    public StandaloneSparkPlugin(Instrumentation instrumentation, Map<String, String> arguments) {
        this.instrumentation = instrumentation;
        this.senders = ConcurrentHashMap.newKeySet();
        this.senders.add(StandaloneCommandSender.SYSTEM_OUT);
        this.scheduler = Executors.newScheduledThreadPool(4, new SparkThreadFactory());
        this.platform = new SparkPlatform(this);
        this.platform.enable();
        this.remoteInterface = new SshRemoteInterface(this, Integer.parseInt(arguments.getOrDefault("port", "0")));

        if (arguments.containsKey("start")) {
            execute(new String[]{"profiler", "start"}, StandaloneCommandSender.SYSTEM_OUT).join();

            if (arguments.containsKey("open")) {
                execute(new String[]{"profiler", "open"}, StandaloneCommandSender.SYSTEM_OUT).join();
            }
        }
    }

    public void disable() {
        this.platform.disable();
        this.scheduler.shutdown();
        this.remoteInterface.close();
    }

    public CompletableFuture<Void> execute(String[] args, StandaloneCommandSender sender) {
        return this.platform.executeCommand(sender, args);
    }

    public List<String> suggest(String[] args, StandaloneCommandSender sender) {
        return this.platform.tabCompleteCommand(sender, args);
    }

    public void addSender(StandaloneCommandSender sender) {
        this.senders.add(sender);
    }

    public void removeSender(StandaloneCommandSender sender) {
        this.senders.remove(sender);
    }

    public CommandResponseHandler createResponseHandler(StandaloneCommandSender sender) {
        return new CommandResponseHandler(this.platform, sender);
    }

    @Override
    public String getVersion() {
        return "@version@";
    }

    @Override
    public Path getPluginDirectory() {
        return Paths.get("spark");
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<StandaloneCommandSender> getCommandSenders() {
        return this.senders.stream();
    }

    @Override
    public void executeAsync(Runnable task) {
        this.scheduler.execute(task);
    }

    @Override
    public void log(Level level, String msg) {
        log(level, msg, null);
    }

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        CommandResponseHandler resp = createResponseHandler(StandaloneCommandSender.SYSTEM_OUT);
        if (level.intValue() >= 900 || throwable != null) { // severe/warning
            resp.replyPrefixed(Component.text(msg, NamedTextColor.RED));
            if (throwable != null) {
                StringWriter stringWriter = new StringWriter();
                throwable.printStackTrace(new PrintWriter(stringWriter));
                resp.replyPrefixed(Component.text(stringWriter.toString(), NamedTextColor.YELLOW));
            }
        } else {
            resp.replyPrefixed(Component.text(msg));
        }
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new StandalonePlatformInfo(getVersion());
    }

    @Override
    public ClassFinder createClassFinder() {
        return ClassFinder.combining(
                new InstrumentationClassFinder(this.instrumentation),
                FallbackClassFinder.INSTANCE
        );
    }

    public SshRemoteInterface getRemoteInterface() {
        return this.remoteInterface;
    }
}

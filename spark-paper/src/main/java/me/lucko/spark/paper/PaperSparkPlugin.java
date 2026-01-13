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

package me.lucko.spark.paper;

import me.lucko.spark.api.Spark;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.common.util.classfinder.ClassFinder;
import me.lucko.spark.paper.api.PaperClassLookup;
import me.lucko.spark.paper.api.PaperScheduler;
import me.lucko.spark.paper.api.PaperSparkModule;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PaperSparkPlugin implements PaperSparkModule, SparkPlugin {
    private final Server server;
    private final Logger logger;
    private final PaperScheduler scheduler;
    private final PaperClassLookup classLookup;

    private final PaperTickHook tickHook;
    private final PaperTickReporter tickReporter;
    private final ThreadDumper gameThreadDumper;
    private final SparkPlatform platform;

    public PaperSparkPlugin(Server server, Logger logger, PaperScheduler scheduler, PaperClassLookup classLookup) {
        this.server = server;
        this.logger = logger;
        this.scheduler = scheduler;
        this.classLookup = classLookup;
        this.tickHook = new PaperTickHook();
        this.tickReporter = new PaperTickReporter();
        this.gameThreadDumper = new ThreadDumper.Specific(Thread.currentThread());
        this.platform = new SparkPlatform(this);
    }

    @Override
    public void enable() {
        this.platform.enable();
    }

    @Override
    public void disable() {
        this.platform.disable();
    }

    @Override
    public void executeCommand(CommandSender sender, String[] args) {
        this.platform.executeCommand(new PaperCommandSender(sender), args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return this.platform.tabCompleteCommand(new PaperCommandSender(sender), args);
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return this.platform.hasPermissionForAnyCommand(new PaperCommandSender(sender));
    }

    @Override
    public Collection<String> getPermissions() {
        return this.platform.getCommandManager().getAllSparkPermissions();
    }

    @Override
    public void onServerTickStart() {
        this.tickHook.onTick();
    }

    @Override
    public void onServerTickEnd(double duration) {
        this.tickReporter.onTick(duration);
    }

    @Override
    public String getVersion() {
        return "@version@";
    }

    @Override
    public Path getPluginDirectory() {
        return this.server.getPluginsFolder().toPath().resolve("spark");
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<PaperCommandSender> getCommandSenders() {
        return Stream.concat(
                this.server.getOnlinePlayers().stream(),
                Stream.of(this.server.getConsoleSender())
        ).map(PaperCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        this.scheduler.executeAsync(task);
    }

    @Override
    public void executeSync(Runnable task) {
        this.scheduler.executeSync(task);
    }

    @Override
    public void log(Level level, String msg) {
        this.logger.log(level, msg);
    }

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        this.logger.log(level, msg, throwable);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public TickHook createTickHook() {
        return this.tickHook;
    }

    @Override
    public TickReporter createTickReporter() {
        return this.tickReporter;
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new PaperClassSourceLookup();
    }

    @Override
    public ClassFinder createClassFinder() {
        return className -> {
            try {
                return this.classLookup.lookup(className);
            } catch (Exception e) {
                return null;
            }
        };
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                Arrays.asList(this.server.getPluginManager().getPlugins()),
                Plugin::getName,
                plugin -> plugin.getPluginMeta().getVersion(),
                plugin -> String.join(", ", plugin.getPluginMeta().getAuthors()),
                plugin -> plugin.getPluginMeta().getDescription()
        );
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new PaperPlayerPingProvider(this.server);
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new PaperServerConfigProvider();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new PaperWorldInfoProvider(this.server);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return PaperPlatformInfo.INSTANCE;
    }

    @Override
    public void registerApi(Spark api) {
        // this.server.getServicesManager().register(Spark.class, api, null, ServicePriority.Normal);
    }
}

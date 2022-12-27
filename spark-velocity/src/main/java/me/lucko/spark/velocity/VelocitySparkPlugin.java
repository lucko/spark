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

package me.lucko.spark.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

@Plugin(
        id = "spark",
        name = "spark",
        version = "@version@",
        description = "@desc@",
        authors = {"Luck"}
)
public class VelocitySparkPlugin implements SparkPlugin, SimpleCommand {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path configDirectory;

    private SparkPlatform platform;

    @Inject
    public VelocitySparkPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path configDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.configDirectory = configDirectory;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onEnable(ProxyInitializeEvent e) {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
        this.proxy.getCommandManager().register("sparkv", this, "sparkvelocity");
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisable(ProxyShutdownEvent e) {
        this.platform.disable();
    }

    @Override
    public void execute(Invocation inv) {
        this.platform.executeCommand(new VelocityCommandSender(inv.source()), inv.arguments());
    }

    @Override
    public List<String> suggest(Invocation inv) {
        return this.platform.tabCompleteCommand(new VelocityCommandSender(inv.source()), inv.arguments());
    }

    @Override
    public String getVersion() {
        return VelocitySparkPlugin.class.getAnnotation(Plugin.class).version();
    }

    @Override
    public Path getPluginDirectory() {
        return this.configDirectory;
    }

    @Override
    public String getCommandName() {
        return "sparkv";
    }

    @Override
    public Stream<VelocityCommandSender> getCommandSenders() {
        return Stream.concat(
                this.proxy.getAllPlayers().stream(),
                Stream.of(this.proxy.getConsoleCommandSource())
        ).map(VelocityCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        this.proxy.getScheduler().buildTask(this, task).schedule();
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.INFO) {
            this.logger.info(msg);
        } else if (level == Level.WARNING) {
            this.logger.warn(msg);
        } else if (level == Level.SEVERE) {
            this.logger.error(msg);
        } else {
            throw new IllegalArgumentException(level.getName());
        }
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new VelocityClassSourceLookup(this.proxy.getPluginManager());
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                this.proxy.getPluginManager().getPlugins(),
                plugin -> plugin.getDescription().getId(),
                plugin -> plugin.getDescription().getVersion().orElse("unspecified"),
                plugin -> String.join(", ", plugin.getDescription().getAuthors())
        );
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new VelocityPlayerPingProvider(this.proxy);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new VelocityPlatformInfo(this.proxy);
    }
}

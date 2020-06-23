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
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.platform.PlatformInfo;
import org.checkerframework.checker.optional.qual.MaybePresent;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Plugin(
        id = "spark",
        name = "spark",
        version = "@version@",
        description = "@desc@",
        authors = {"Luck", "sk89q"}
)
public class VelocitySparkPlugin implements SparkPlugin, Command {

    private final ProxyServer proxy;
    private final Path configDirectory;

    private SparkPlatform platform;

    @Inject
    public VelocitySparkPlugin(ProxyServer proxy, @DataDirectory Path configDirectory) {
        this.proxy = proxy;
        this.configDirectory = configDirectory;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onEnable(ProxyInitializeEvent e) {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
        this.proxy.getCommandManager().register(this, "sparkv", "sparkvelocity");
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisable(ProxyShutdownEvent e) {
        this.platform.disable();
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        this.platform.executeCommand(new VelocityCommandSender(sender), args);
    }

    @Override
    public @MaybePresent List<String> suggest(CommandSource sender, String[] currentArgs) {
        return this.platform.tabCompleteCommand(new VelocityCommandSender(sender), currentArgs);
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
    public Stream<VelocityCommandSender> getSendersWithPermission(String permission) {
        return Stream.concat(
                this.proxy.getAllPlayers().stream().filter(player -> player.hasPermission(permission)),
                Stream.of(this.proxy.getConsoleCommandSource())
        ).map(VelocityCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        this.proxy.getScheduler().buildTask(this, task).schedule();
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new VelocityPlatformInfo(this.proxy);
    }
}

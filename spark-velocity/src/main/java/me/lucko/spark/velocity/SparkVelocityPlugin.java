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
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.TickCounter;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.optional.qual.MaybePresent;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

@Plugin(
        id = "spark",
        name = "spark",
        version = "@version@",
        description = "@desc@",
        authors = {"Luck", "sk89q"}
)
public class SparkVelocityPlugin implements SparkPlugin<CommandSource>, Command {

    private final SparkPlatform<CommandSource> platform = new SparkPlatform<>(this);

    private final ProxyServer proxy;
    private final Path configDirectory;

    @Inject
    public SparkVelocityPlugin(ProxyServer proxy, @DataDirectory Path configDirectory) {
        this.proxy = proxy;
        this.configDirectory = configDirectory;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onEnable(ProxyInitializeEvent e) {
        this.platform.enable();
        this.proxy.getCommandManager().register(this, "sparkv", "sparkvelocity");
    }

    @Subscribe(order = PostOrder.LAST)
    public void onDisable(ProxyShutdownEvent e) {
        this.platform.disable();
    }

    @Override
    public void execute(@MaybePresent CommandSource sender, @NonNull @MaybePresent String[] args) {
        if (!sender.hasPermission("spark")) {
            TextComponent msg = TextComponent.builder("You do not have permission to use this command.").color(TextColor.RED).build();
            sender.sendMessage(msg);
            return;
        }

        this.platform.executeCommand(sender, args);
    }

    @Override
    public String getVersion() {
        return SparkVelocityPlugin.class.getAnnotation(Plugin.class).version();
    }

    @Override
    public Path getPluginFolder() {
        return this.configDirectory;
    }

    @Override
    public String getLabel() {
        return "sparkv";
    }

    @Override
    public Set<CommandSource> getSendersWithPermission(String permission) {
        Set<CommandSource> senders = new HashSet<>(this.proxy.getAllPlayers());
        senders.removeIf(sender -> !sender.hasPermission(permission));
        senders.add(this.proxy.getConsoleCommandSource());
        return senders;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendMessage(CommandSource sender, String message) {
        sender.sendMessage(ComponentSerializers.LEGACY.deserialize(message, '&'));
    }

    @Override
    public void sendLink(CommandSource sender, String url) {
        TextComponent msg = TextComponent.builder(url)
                .color(TextColor.GRAY)
                .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .build();
        sender.sendMessage(msg);
    }

    @Override
    public void runAsync(Runnable r) {
        this.proxy.getScheduler().buildTask(this, r).schedule();
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return ThreadDumper.ALL;
    }

    @Override
    public TickCounter createTickCounter() {
        return null;
    }
}

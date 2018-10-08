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
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import me.lucko.spark.common.CommandHandler;
import me.lucko.spark.profiler.ThreadDumper;
import me.lucko.spark.profiler.TickCounter;

import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;

@Plugin(
        id = "spark",
        name = "spark",
        version = "@version@",
        description = "Spark is a CPU profiling plugin based on sk89q's WarmRoast profiler",
        authors = {"Luck", "sk89q"}
)
public class SparkVelocityPlugin {

    private final ProxyServer proxy;

    private final CommandHandler<CommandSource> commandHandler = new CommandHandler<CommandSource>() {
        @SuppressWarnings("deprecation")
        private TextComponent colorize(String message) {
            return ComponentSerializers.LEGACY.deserialize(message, '&');
        }

        private void broadcast(Component msg) {
            SparkVelocityPlugin.this.proxy.getConsoleCommandSource().sendMessage(msg);
            for (Player player : SparkVelocityPlugin.this.proxy.getAllPlayers()) {
                if (player.hasPermission("spark.profiler")) {
                    player.sendMessage(msg);
                }
            }
        }

        @Override
        protected String getVersion() {
            return SparkVelocityPlugin.class.getAnnotation(Plugin.class).version();
        }

        @Override
        protected String getLabel() {
            return "sparkvelocity";
        }

        @Override
        protected void sendMessage(CommandSource sender, String message) {
            sender.sendMessage(colorize(message));
        }

        @Override
        protected void sendMessage(String message) {
            broadcast(colorize(message));
        }

        @Override
        protected void sendLink(String url) {
            TextComponent msg = TextComponent.builder(url)
                    .color(TextColor.GRAY)
                    .clickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                    .build();
            broadcast(msg);
        }

        @Override
        protected void runAsync(Runnable r) {
            SparkVelocityPlugin.this.proxy.getScheduler().buildTask(SparkVelocityPlugin.this, r).schedule();
        }

        @Override
        protected ThreadDumper getDefaultThreadDumper() {
            return ThreadDumper.ALL;
        }

        @Override
        protected TickCounter newTickCounter() {
            throw new UnsupportedOperationException();
        }
    };

    @Inject
    public SparkVelocityPlugin(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onEnable(ProxyInitializeEvent e) {
        this.proxy.getCommandManager().register((sender, args) -> {
            if (!sender.hasPermission("spark.profiler")) {
                TextComponent msg = TextComponent.builder("You do not have permission to use this command.").color(TextColor.RED).build();
                sender.sendMessage(msg);
                return;
            }

            SparkVelocityPlugin.this.commandHandler.handleCommand(sender, args);
        }, "sparkvelocity", "vprofiler");
    }
}

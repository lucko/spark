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

package me.lucko.spark.bungeecord;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.TickCounter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SparkBungeeCordPlugin extends Plugin implements SparkPlugin<CommandSender> {

    private final SparkPlatform<CommandSender> platform = new SparkPlatform<>(this);

    @Override
    public void onEnable() {
        this.platform.enable();
        getProxy().getPluginManager().registerCommand(this, new SparkCommand(this));
    }

    @Override
    public void onDisable() {
        this.platform.disable();
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public Path getPluginFolder() {
        return getDataFolder().toPath();
    }

    @Override
    public String getLabel() {
        return "sparkb";
    }

    @Override
    public Set<CommandSender> getSenders() {
        Set<CommandSender> senders = new HashSet<>(getProxy().getPlayers());
        senders.add(getProxy().getConsole());
        return senders;
    }

    @Override
    public void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', message)));
    }

    @Override
    public void sendLink(CommandSender sender, String url) {
        TextComponent component = new TextComponent(url);
        component.setColor(ChatColor.GRAY);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        sender.sendMessage(component);
    }

    @Override
    public void runAsync(Runnable r) {
        getProxy().getScheduler().runAsync(SparkBungeeCordPlugin.this, r);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return ThreadDumper.ALL;
    }

    @Override
    public TickCounter createTickCounter() {
        return null;
    }

    private static final class SparkCommand extends Command implements TabExecutor {
        private final SparkBungeeCordPlugin plugin;

        SparkCommand(SparkBungeeCordPlugin plugin) {
            super("sparkb", null, "sparkbungee");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!sender.hasPermission("spark")) {
                TextComponent msg = new TextComponent("You do not have permission to use this command.");
                msg.setColor(ChatColor.RED);
                sender.sendMessage(msg);
                return;
            }

            this.plugin.platform.executeCommand(sender, args);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (!sender.hasPermission("spark")) {
                return Collections.emptyList();
            }
            return this.plugin.platform.tabCompleteCommand(sender, args);
        }
    }
}

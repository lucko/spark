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

import me.lucko.spark.common.CommandSender;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.TickCounter;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SparkBungeeCordPlugin extends Plugin implements SparkPlugin {

    private final SparkPlatform platform = new SparkPlatform(this);

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
    public Set<CommandSender> getSendersWithPermission(String permission) {
        List<net.md_5.bungee.api.CommandSender> senders = new LinkedList<>(getProxy().getPlayers());
        senders.removeIf(sender -> !sender.hasPermission(permission));
        senders.add(getProxy().getConsole());
        return senders.stream().map(BungeeCordCommandSender::new).collect(Collectors.toSet());
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
        public void execute(net.md_5.bungee.api.CommandSender sender, String[] args) {
            this.plugin.platform.executeCommand(new BungeeCordCommandSender(sender), args);
        }

        @Override
        public Iterable<String> onTabComplete(net.md_5.bungee.api.CommandSender sender, String[] args) {
            return this.plugin.platform.tabCompleteCommand(new BungeeCordCommandSender(sender), args);
        }
    }
}

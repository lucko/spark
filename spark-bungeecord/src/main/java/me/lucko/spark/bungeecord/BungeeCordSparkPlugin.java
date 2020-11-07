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
import me.lucko.spark.common.platform.PlatformInfo;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.nio.file.Path;
import java.util.stream.Stream;

public class BungeeCordSparkPlugin extends Plugin implements SparkPlugin {
    private BungeeAudiences audienceFactory;
    private SparkPlatform platform;

    @Override
    public void onEnable() {
        this.audienceFactory = BungeeAudiences.create(this);
        this.platform = new SparkPlatform(this);
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
    public Path getPluginDirectory() {
        return getDataFolder().toPath();
    }

    @Override
    public String getCommandName() {
        return "sparkb";
    }

    @Override
    public Stream<BungeeCordCommandSender> getSendersWithPermission(String permission) {
        return Stream.concat(
                getProxy().getPlayers().stream().filter(player -> player.hasPermission(permission)),
                Stream.of(getProxy().getConsole())
        ).map(sender -> new BungeeCordCommandSender(sender, this.audienceFactory));
    }

    @Override
    public void executeAsync(Runnable task) {
        getProxy().getScheduler().runAsync(BungeeCordSparkPlugin.this, task);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new BungeeCordPlatformInfo(getProxy());
    }

    private static final class SparkCommand extends Command implements TabExecutor {
        private final BungeeCordSparkPlugin plugin;

        SparkCommand(BungeeCordSparkPlugin plugin) {
            super("sparkb", null, "sparkbungee");
            this.plugin = plugin;
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            this.plugin.platform.executeCommand(new BungeeCordCommandSender(sender, this.plugin.audienceFactory), args);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return this.plugin.platform.tabCompleteCommand(new BungeeCordCommandSender(sender, this.plugin.audienceFactory), args);
        }
    }
}

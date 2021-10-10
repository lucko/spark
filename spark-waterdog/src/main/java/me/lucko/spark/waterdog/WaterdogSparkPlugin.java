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

package me.lucko.spark.waterdog;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.util.ClassSourceLookup;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.command.Command;
import dev.waterdog.waterdogpe.command.CommandSender;
import dev.waterdog.waterdogpe.plugin.Plugin;

import java.nio.file.Path;
import java.util.stream.Stream;

public class WaterdogSparkPlugin extends Plugin implements SparkPlugin {
    private SparkPlatform platform;

    public ProxyServer getProxy() {
        return ProxyServer.getInstance();
    }

    @Override
    public void onEnable() {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
        getProxy().getCommandMap().registerCommand(new SparkCommand(this));
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
        return "sparkw";
    }

    @Override
    public Stream<WaterdogCommandSender> getCommandSenders() {
        return Stream.concat(
                getProxy().getPlayers().values().stream(),
                Stream.of(getProxy().getConsoleSender())
        ).map(WaterdogCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        getProxy().getScheduler().scheduleAsync(task);
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new WaterdogClassSourceLookup(getProxy());
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new WaterdogPlatformInfo();
    }

    private static final class SparkCommand extends Command {
        private final WaterdogSparkPlugin plugin;

        SparkCommand(WaterdogSparkPlugin plugin) {
            super("sparkw");
            this.plugin = plugin;
        }

        @Override
        public boolean onExecute(CommandSender sender, String alias, String[] args) {
            this.plugin.platform.executeCommand(new WaterdogCommandSender(sender), args);
            return true;
        }
    }
}

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

package me.lucko.spark.forge.plugin;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.forge.Forge1122CommandSender;
import me.lucko.spark.forge.Forge1122SparkMod;

import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

import javax.annotation.Nullable;

public abstract class Forge1122SparkPlugin implements SparkPlugin, ICommand {

    private final Forge1122SparkMod mod;
    private final Logger logger;
    protected final ScheduledExecutorService scheduler;
    protected final SparkPlatform platform;
    protected final ThreadDumper.GameThread threadDumper = new ThreadDumper.GameThread();

    protected Forge1122SparkPlugin(Forge1122SparkMod mod) {
        this.mod = mod;
        this.logger = LogManager.getLogger("spark");
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("spark-forge-async-worker");
            thread.setDaemon(true);
            return thread;
        });
        this.platform = new SparkPlatform(this);
    }

    public void enable() {
        this.platform.enable();
    }

    public void disable() {
        this.platform.disable();
        this.scheduler.shutdown();
    }

    public abstract boolean hasPermission(ICommandSender sender, String permission);

    @Override
    public String getVersion() {
        return this.mod.getVersion();
    }

    @Override
    public Path getPluginDirectory() {
        return this.mod.getConfigDirectory();
    }

    @Override
    public void executeAsync(Runnable task) {
        this.scheduler.execute(task);
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
    public ThreadDumper getDefaultThreadDumper() {
        return this.threadDumper.get();
    }

    // implement ICommand

    @Override
    public String getName() {
        return getCommandName();
    }

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "/" + getCommandName();
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList(getCommandName());
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        this.threadDumper.ensureSetup();
        this.platform.executeCommand(new Forge1122CommandSender(sender, this), args);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos blockPos) {
        return this.platform.tabCompleteCommand(new Forge1122CommandSender(sender, this), args);
    }

    @Override
    public boolean checkPermission(MinecraftServer minecraftServer, ICommandSender sender) {
        return this.platform.hasPermissionForAnyCommand(new Forge1122CommandSender(sender, this));
    }

    @Override
    public boolean isUsernameIndex(String[] strings, int i) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return getCommandName().compareTo(o.getName());
    }

}

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

package me.lucko.spark.forge;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.sampler.ThreadDumper;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SuppressWarnings("NullableProblems")
public abstract class ForgeSparkPlugin implements SparkPlugin, ICommand {

    private final ForgeSparkMod mod;
    private final ScheduledExecutorService scheduler;
    private final SparkPlatform platform;

    protected ForgeSparkPlugin(ForgeSparkMod mod) {
        this.mod = mod;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("spark-forge-async-worker").build()
        );
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    abstract boolean hasPermission(ICommandSender sender, String permission);

    @Override
    public String getVersion() {
        return ForgeSparkMod.class.getAnnotation(Mod.class).version();
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
    public ThreadDumper getDefaultThreadDumper() {
        return new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
    }

    // implement ICommand

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "/" + getCommandName();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        this.platform.executeCommand(new ForgeCommandSender(sender, this), args);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos blockPos) {
        return this.platform.tabCompleteCommand(new ForgeCommandSender(sender, this), args);
    }

    @Override
    public boolean checkPermission(MinecraftServer minecraftServer, ICommandSender sender) {
        return hasPermission(sender, "spark");
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

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
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.serializer.ComponentSerializers;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SuppressWarnings("NullableProblems")
public abstract class ForgeSparkPlugin implements SparkPlugin<ICommandSender>, ICommand {

    private final SparkForgeMod mod;
    private final ScheduledExecutorService scheduler;
    private final SparkPlatform<ICommandSender> platform;

    protected ForgeSparkPlugin(SparkForgeMod mod) {
        this.mod = mod;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("spark-forge-async-worker").build()
        );
        this.platform = new SparkPlatform<>(this);
        this.platform.enable();
    }

    @Override
    public String getVersion() {
        return SparkForgeMod.class.getAnnotation(Mod.class).version();
    }

    @Override
    public Path getPluginFolder() {
        return this.mod.getConfigDirectory();
    }

    @Override
    public void sendMessage(ICommandSender sender, Component message) {
        ITextComponent component = ITextComponent.Serializer.jsonToComponent(ComponentSerializers.JSON.serialize(message));
        sender.sendMessage(component);
    }

    @Override
    public void runAsync(Runnable r) {
        this.scheduler.execute(r);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return new ThreadDumper.Specific(new long[]{Thread.currentThread().getId()});
    }

    // implement ICommand

    @Override
    public String getUsage(ICommandSender iCommandSender) {
        return "/" + getLabel();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (!checkPermission(server, sender)) {
            TextComponentString msg = new TextComponentString("You do not have permission to use this command.");
            Style style = msg.getStyle();
            style.setColor(TextFormatting.GRAY);
            msg.setStyle(style);

            sender.sendMessage(msg);
            return;
        }

        this.platform.executeCommand(sender, args);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos blockPos) {
        if (!checkPermission(server, sender)) {
            return Collections.emptyList();
        }
        return this.platform.tabCompleteCommand(sender, args);
    }

    @Override
    public boolean isUsernameIndex(String[] strings, int i) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return getLabel().compareTo(o.getName());
    }
}

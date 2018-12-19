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
import me.lucko.spark.sampler.ThreadDumper;

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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

@SuppressWarnings("NullableProblems")
public abstract class ForgeSparkPlatform extends SparkPlatform<ICommandSender> implements ICommand {

    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            new ThreadFactoryBuilder().setNameFormat("spark-forge-async-worker").build()
    );

    @Override
    public String getVersion() {
        return SparkForgeMod.class.getAnnotation(Mod.class).version();
    }

    @SuppressWarnings("deprecation")
    protected ITextComponent colorize(String message) {
        TextComponent component = ComponentSerializers.LEGACY.deserialize(message, '&');
        return ITextComponent.Serializer.jsonToComponent(ComponentSerializers.JSON.serialize(component));
    }

    protected abstract void broadcast(ITextComponent msg);

    @Override
    public void sendMessage(ICommandSender sender, String message) {
        sender.sendMessage(colorize(message));
    }

    @Override
    public void sendMessage(String message) {
        ITextComponent msg = colorize(message);
        broadcast(msg);
    }

    @Override
    public void sendLink(String url) {
        TextComponentString msg = new TextComponentString(url);
        Style style = msg.getStyle();
        style.setColor(TextFormatting.GRAY);
        style.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        msg.setStyle(style);

        broadcast(msg);
    }

    @Override
    public void runAsync(Runnable r) {
        this.worker.execute(r);
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

        executeCommand(sender, args);
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos blockPos) {
        if (!checkPermission(server, sender)) {
            return Collections.emptyList();
        }
        return tabCompleteCommand(sender, args);
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

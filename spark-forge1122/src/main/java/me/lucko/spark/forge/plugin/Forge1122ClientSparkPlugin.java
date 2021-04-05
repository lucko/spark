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

import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.forge.Forge1122CommandSender;
import me.lucko.spark.forge.Forge1122PlatformInfo;
import me.lucko.spark.forge.Forge1122SparkMod;
import me.lucko.spark.forge.Forge1122TickHook;
import me.lucko.spark.forge.Forge1122TickReporter;

import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.stream.Stream;

public class Forge1122ClientSparkPlugin extends Forge1122SparkPlugin {

    public static void register(Forge1122SparkMod mod) {
        Forge1122ClientSparkPlugin plugin = new Forge1122ClientSparkPlugin(mod, Minecraft.getMinecraft());
        plugin.enable();

        // register listeners
        MinecraftForge.EVENT_BUS.register(plugin);

        // register commands
        ClientCommandHandler.instance.registerCommand(plugin);
    }

    private final Minecraft minecraft;

    public Forge1122ClientSparkPlugin(Forge1122SparkMod mod, Minecraft minecraft) {
        super(mod);
        this.minecraft = minecraft;
    }

    @Override
    public boolean hasPermission(ICommandSender sender, String permission) {
        return true;
    }

    @Override
    public Stream<Forge1122CommandSender> getCommandSenders() {
        return Stream.of(new Forge1122CommandSender(this.minecraft.player, this));
    }

    @Override
    public TickHook createTickHook() {
        return new Forge1122TickHook(TickEvent.Type.CLIENT);
    }

    @Override
    public TickReporter createTickReporter() {
        return new Forge1122TickReporter(TickEvent.Type.CLIENT);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new Forge1122PlatformInfo(PlatformInfo.Type.CLIENT);
    }

    @Override
    public String getCommandName() {
        return "sparkc";
    }

}

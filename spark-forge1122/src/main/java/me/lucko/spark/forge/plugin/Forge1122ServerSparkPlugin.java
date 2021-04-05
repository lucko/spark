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

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.stream.Stream;

public class Forge1122ServerSparkPlugin extends Forge1122SparkPlugin {

    public static Forge1122ServerSparkPlugin register(Forge1122SparkMod mod, FMLServerStartingEvent event) {
        Forge1122ServerSparkPlugin plugin = new Forge1122ServerSparkPlugin(mod, event.getServer());
        plugin.enable();

        // register commands & permissions
        event.registerServerCommand(plugin);
        PermissionAPI.registerNode("spark", DefaultPermissionLevel.OP, "Access to the spark command");

        return plugin;
    }

    private final MinecraftServer server;

    public Forge1122ServerSparkPlugin(Forge1122SparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
    }

    @Override
    public boolean hasPermission(ICommandSender sender, String permission) {
        if (sender instanceof EntityPlayer) {
            return PermissionAPI.hasPermission((EntityPlayer) sender, permission);
        } else {
            return true;
        }
    }

    @Override
    public Stream<Forge1122CommandSender> getCommandSenders() {
        return Stream.concat(
            this.server.getPlayerList().getPlayers().stream(),
            Stream.of(this.server)
        ).map(sender -> new Forge1122CommandSender(sender, this));
    }

    @Override
    public TickHook createTickHook() {
        return new Forge1122TickHook(TickEvent.Type.SERVER);
    }

    @Override
    public TickReporter createTickReporter() {
        return new Forge1122TickReporter(TickEvent.Type.SERVER);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new Forge1122PlatformInfo(PlatformInfo.Type.SERVER);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}

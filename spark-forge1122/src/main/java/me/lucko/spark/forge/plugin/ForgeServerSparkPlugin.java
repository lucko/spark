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
import me.lucko.spark.common.sampler.tick.TickHook;
import me.lucko.spark.common.sampler.tick.TickReporter;
import me.lucko.spark.forge.ForgeCommandSender;
import me.lucko.spark.forge.ForgePlatformInfo;
import me.lucko.spark.forge.ForgeSparkMod;
import me.lucko.spark.forge.ForgeTickHook;
import me.lucko.spark.forge.ForgeTickReporter;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.stream.Stream;

public class ForgeServerSparkPlugin extends ForgeSparkPlugin {

    public static ForgeServerSparkPlugin register(ForgeSparkMod mod, FMLServerStartingEvent event) {
        ForgeServerSparkPlugin plugin = new ForgeServerSparkPlugin(mod, event.getServer());
        plugin.enable();

        // register commands & permissions
        event.registerServerCommand(plugin);
        PermissionAPI.registerNode("spark", DefaultPermissionLevel.OP, "Access to the spark command");

        return plugin;
    }

    private final MinecraftServer server;

    public ForgeServerSparkPlugin(ForgeSparkMod mod, MinecraftServer server) {
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
    public Stream<ForgeCommandSender> getCommandSenders() {
        return Stream.concat(
            this.server.getPlayerList().getPlayers().stream(),
            Stream.of(this.server)
        ).map(sender -> new ForgeCommandSender(sender, this));
    }

    @Override
    public TickHook createTickHook() {
        return new ForgeTickHook(TickEvent.Type.SERVER);
    }

    @Override
    public TickReporter createTickReporter() {
        return new ForgeTickReporter(TickEvent.Type.SERVER);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new ForgePlatformInfo(PlatformInfo.Type.SERVER);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}

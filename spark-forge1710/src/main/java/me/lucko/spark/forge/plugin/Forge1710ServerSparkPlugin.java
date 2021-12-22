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

import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.forge.*;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.stream.Stream;

public class Forge1710ServerSparkPlugin extends Forge1710SparkPlugin {

    public static Forge1710ServerSparkPlugin register(Forge1710SparkMod mod, FMLServerStartingEvent event) {
        Forge1710ServerSparkPlugin plugin = new Forge1710ServerSparkPlugin(mod, event.getServer());
        plugin.enable();

        // register commands & permissions
        event.registerServerCommand(plugin);

        return plugin;
    }

    private final MinecraftServer server;

    public Forge1710ServerSparkPlugin(Forge1710SparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
    }

    @Override
    public boolean hasPermission(ICommandSender sender, String permission) {
        if (sender instanceof EntityPlayer) {
            return isOp((EntityPlayer) sender);
        } else {
            return true;
        }
    }

    @Override
    public Stream<Forge1710CommandSender> getCommandSenders() {
        return Stream.concat(
                ((List<EntityPlayer>)this.server.getConfigurationManager().playerEntityList).stream(),
            Stream.of(this.server)
        ).map(sender -> new Forge1710CommandSender(sender, this));
    }

    @Override
    public TickHook createTickHook() {
        return new Forge1710TickHook(TickEvent.Type.SERVER);
    }

    @Override
    public TickReporter createTickReporter() {
        return new Forge1710TickReporter(TickEvent.Type.SERVER);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new Forge1710PlatformInfo(PlatformInfo.Type.SERVER);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}

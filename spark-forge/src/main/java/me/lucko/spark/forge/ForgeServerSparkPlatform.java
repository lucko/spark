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

import me.lucko.spark.sampler.TickCounter;

import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;

public class ForgeServerSparkPlatform extends ForgeSparkPlatform {

    @Override
    protected void broadcast(ITextComponent msg) {
        FMLCommonHandler.instance().getMinecraftServerInstance().sendMessage(msg);

        List<EntityPlayerMP> players = FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().getPlayers();
        for (EntityPlayerMP player : players) {
            if (player.canUseCommand(4, "spark.profiler")) {
                player.sendMessage(msg);
            }
        }
    }

    @Override
    public TickCounter newTickCounter() {
        return new ForgeTickCounter(TickEvent.Type.SERVER);
    }

    @Override
    public String getLabel() {
        return "spark";
    }

    @Override
    public String getName() {
        return "spark";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("profiler");
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(4, "spark.profiler");
    }
}

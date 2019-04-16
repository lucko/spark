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

import me.lucko.spark.common.sampler.TickCounter;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ForgeServerSparkPlugin extends ForgeSparkPlugin {
    public ForgeServerSparkPlugin(SparkForgeMod mod) {
        super(mod);
    }

    @Override
    public Set<ICommandSender> getSenders() {
        MinecraftServer mcServer = FMLCommonHandler.instance().getMinecraftServerInstance();
        Set<ICommandSender> senders = new HashSet<>(mcServer.getPlayerList().getPlayers());
        senders.add(mcServer);
        return senders;
    }

    @Override
    public TickCounter createTickCounter() {
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
        return Collections.emptyList();
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(4, "spark");
    }
}

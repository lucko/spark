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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.spark.common.sampler.TickCounter;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.Arrays;
import java.util.stream.Stream;

public class ForgeServerSparkPlugin extends ForgeSparkPlugin implements Command<CommandSource> {

    public static void register(ForgeSparkMod mod, FMLServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        CommandDispatcher<CommandSource> dispatcher = event.getCommandDispatcher();

        ForgeServerSparkPlugin plugin = new ForgeServerSparkPlugin(mod, server);
        registerCommands(dispatcher, plugin, "spark");
        PermissionAPI.registerNode("spark", DefaultPermissionLevel.OP, "Access to the spark command");
    }

    private final MinecraftServer server;

    public ForgeServerSparkPlugin(ForgeSparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String[] split = context.getInput().split(" ");
        if (split.length == 0 || !split[0].equals("/spark")) {
            return 0;
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);

        this.platform.executeCommand(new ForgeCommandSender(context.getSource().asPlayer(), this), args);
        return 1;
    }

    @Override
    boolean hasPermission(ICommandSource sender, String permission) {
        if (sender instanceof PlayerEntity) {
            return PermissionAPI.hasPermission((PlayerEntity) sender, permission);
        } else {
            return true;
        }
    }

    @Override
    public Stream<ForgeCommandSender> getSendersWithPermission(String permission) {
        return Stream.concat(
                this.server.getPlayerList().getPlayers().stream().filter(player -> PermissionAPI.hasPermission(player, permission)),
                Stream.of(this.server)
        ).map(sender -> new ForgeCommandSender(sender, this));
    }

    @Override
    public TickCounter createTickCounter() {
        return new ForgeTickCounter(TickEvent.Type.SERVER);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}

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

package me.lucko.spark.fabric;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.lucko.spark.common.sampler.TickCounter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.util.Arrays;
import java.util.stream.Stream;

public class FabricServerSparkPlugin extends FabricSparkPlugin implements Command<ServerCommandSource> {

    private final MinecraftServer server;

    public FabricServerSparkPlugin(FabricSparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
    }

    public static void register(FabricSparkMod mod, MinecraftServer server) {
        CommandDispatcher<ServerCommandSource> dispatcher = server.getCommandManager().getDispatcher();

        FabricServerSparkPlugin plugin = new FabricServerSparkPlugin(mod, server);
        registerCommands(dispatcher, plugin, "spark");
        //        PermissionAPI.registerNode("spark", DefaultPermissionLevel.OP, "Access to the spark command");
    }

    @Override
    public int run(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String[] split = context.getInput().split(" ");
        if (split.length == 0 || !split[0].equals("/spark")) {
            context.getSource().sendError(new LiteralText("Wrong split started with " + (split.length == 0 ? "nothing" : split[0])));
            return 0;
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);

        this.platform.executeCommand(new FabricCommandSender(context.getSource()::hasPermissionLevel, context.getSource().getPlayer()), args);
        return 1;
    }

    @Override
    public Stream<FabricCommandSender> getSendersWithPermission(String permission) {
        return Stream.concat(
                this.server.getPlayerManager().getPlayerList().stream()
                        .filter(player -> this.server.getPermissionLevel(player.getGameProfile()) == 4),
                Stream.of(this.server)
        ).map(sender -> new FabricCommandSender(i -> true, sender));
    }

    @Override
    public TickCounter createTickCounter() {
        return new FabricTickCounter(FabricSparkMod.getInstance()::addServerCounter, FabricSparkMod.getInstance()::removeServerCounter);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}

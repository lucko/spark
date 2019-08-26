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

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.spark.common.sampler.TickCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.command.CommandSource;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class FabricClientSparkPlugin extends FabricSparkPlugin {

    private final MinecraftClient minecraft;
    private CommandDispatcher<CommandSource> dispatcher;

    public FabricClientSparkPlugin(FabricSparkMod mod, MinecraftClient minecraft) {
        super(mod);
        this.minecraft = minecraft;
    }

    public static void register(FabricSparkMod mod, MinecraftClient client) {
        FabricClientSparkPlugin plugin = new FabricClientSparkPlugin(mod, client);

        plugin.scheduler.scheduleWithFixedDelay(plugin::checkCommandRegistered, 10, 10, TimeUnit.SECONDS);
    }

    private void checkCommandRegistered() {
        ClientPlayerEntity player = this.minecraft.player;
        if (player == null) {
            return;
        }

        ClientPlayNetworkHandler connection = player.networkHandler;
        if (connection == null) {
            return;
        }

        try {
            CommandDispatcher<CommandSource> dispatcher = connection.getCommandDispatcher();
            if (dispatcher != this.dispatcher) {
                this.dispatcher = dispatcher;
                registerCommands(this.dispatcher, c -> 0, "sparkc", "sparkclient");
                this.mod.setChatSendCallback(this::onClientChat);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FabricCommandSender createClientSender() {
        return new FabricCommandSender(this.minecraft.player.networkHandler.getCommandSource()::hasPermissionLevel, this.minecraft.player);
    }

    public boolean onClientChat(String chat) {
        String[] split = chat.split(" ");
        if (split.length == 0 || (!split[0].equals("/sparkc") && !split[0].equals("/sparkclient"))) {
            return false;
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);
        this.platform.executeCommand(createClientSender(), args);
        this.minecraft.inGameHud.getChatHud().addToMessageHistory(chat);
        return true;
    }

    @Override
    public Stream<FabricCommandSender> getSendersWithPermission(String permission) {
        return Stream.of(createClientSender());
    }

    @Override
    public TickCounter createTickCounter() {
        return new FabricTickCounter(FabricSparkMod.getInstance()::addClientCounter, FabricSparkMod.getInstance()::removeClientCounter);
    }

    @Override
    public String getCommandName() {
        return "sparkc";
    }

}

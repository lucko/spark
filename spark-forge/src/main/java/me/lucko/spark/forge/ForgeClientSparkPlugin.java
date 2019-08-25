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

import com.mojang.brigadier.CommandDispatcher;
import me.lucko.spark.common.sampler.TickCounter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.command.ICommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class ForgeClientSparkPlugin extends ForgeSparkPlugin {

    private static final Field COMMAND_DISPATCHER_FIELD;
    static {
        COMMAND_DISPATCHER_FIELD = Arrays.stream(ClientPlayNetHandler.class.getDeclaredFields())
                .filter(f -> f.getType() == CommandDispatcher.class)
                .findFirst().orElseThrow(() -> new RuntimeException("No field with CommandDispatcher type"));
        COMMAND_DISPATCHER_FIELD.setAccessible(true);
    }

    public static void register(ForgeSparkMod mod, FMLClientSetupEvent event) {
        Minecraft minecraft = event.getMinecraftSupplier().get();

        ForgeClientSparkPlugin plugin = new ForgeClientSparkPlugin(mod, minecraft);
        MinecraftForge.EVENT_BUS.register(plugin);

        plugin.scheduler.scheduleWithFixedDelay(plugin::checkCommandRegistered, 10, 10, TimeUnit.SECONDS);
    }

    private final Minecraft minecraft;
    private CommandDispatcher<ISuggestionProvider> dispatcher;

    public ForgeClientSparkPlugin(ForgeSparkMod mod, Minecraft minecraft) {
        super(mod);
        this.minecraft = minecraft;
    }

    private void checkCommandRegistered() {
        ClientPlayerEntity player = this.minecraft.player;
        if (player == null) {
            return;
        }

        ClientPlayNetHandler connection = player.connection;
        if (connection == null) {
            return;
        }

        try {
            CommandDispatcher<ISuggestionProvider> dispatcher = (CommandDispatcher) COMMAND_DISPATCHER_FIELD.get(connection);
            if (dispatcher != this.dispatcher) {
                this.dispatcher = dispatcher;
                registerCommands(this.dispatcher, context -> 1, "sparkc", "sparkclient");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onClientChat(ClientChatEvent event) {
        String chat = event.getMessage();
        String[] split = chat.split(" ");
        if (split.length == 0 || (!split[0].equals("/sparkc") && !split[0].equals("/sparkclient"))) {
            return;
        }

        String[] args = Arrays.copyOfRange(split, 1, split.length);
        this.platform.executeCommand(new ForgeCommandSender(this.minecraft.player, this), args);
        this.minecraft.ingameGUI.getChatGUI().addToSentMessages(chat);
        event.setCanceled(true);
    }

    @Override
    boolean hasPermission(ICommandSource sender, String permission) {
        return true;
    }

    @Override
    public Stream<ForgeCommandSender> getSendersWithPermission(String permission) {
        return Stream.of(new ForgeCommandSender(this.minecraft.player, this));
    }

    @Override
    public TickCounter createTickCounter() {
        return new ForgeTickCounter(TickEvent.Type.CLIENT);
    }

    @Override
    public String getCommandName() {
        return "sparkc";
    }

}

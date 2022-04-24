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

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.forge.ForgeCommandSender;
import me.lucko.spark.forge.ForgePlatformInfo;
import me.lucko.spark.forge.ForgePlayerPingProvider;
import me.lucko.spark.forge.ForgeSparkMod;
import me.lucko.spark.forge.ForgeTickHook;
import me.lucko.spark.forge.ForgeTickReporter;

import net.minecraft.command.CommandSource;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppingEvent;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class ForgeServerSparkPlugin extends ForgeSparkPlugin<MinecraftServer> implements Command<CommandSource>, SuggestionProvider<CommandSource> {

    public static void register(ForgeSparkMod mod, FMLServerAboutToStartEvent event) {
        ForgeServerSparkPlugin plugin = new ForgeServerSparkPlugin(mod, event.getServer());
        plugin.enable();
    }

    //private Map<String, PermissionNode<Boolean>> registeredPermissions = Collections.emptyMap();

    public ForgeServerSparkPlugin(ForgeSparkMod mod, MinecraftServer server) {
        super(mod, server);
    }

    @Override
    public void enable() {
        super.enable();

        // register commands
        registerCommands(this.game.getCommands().getDispatcher());

        // register default permission
        PermissionAPI.registerNode("spark", DefaultPermissionLevel.OP, "");

        // register listeners
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void disable() {
        super.disable();

        // unregister listeners
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onDisable(FMLServerStoppingEvent event) {
        if (event.getServer() == this.game) {
            disable();
        }
    }

    /*
    @SubscribeEvent
    public void onPermissionGather(PermissionGatherEvent.Nodes e) {
        PermissionResolver<Boolean> defaultValue = (player, playerUUID, context) -> player != null && player.hasPermissions(4);

        // collect all possible permissions
        List<String> permissions = this.platform.getCommands().stream()
                .map(me.lucko.spark.common.command.Command::primaryAlias)
                .collect(Collectors.toList());

        // special case for the "spark" permission: map it to "spark.all"
        permissions.add("all");

        // register permissions with forge & keep a copy for lookup
        ImmutableMap.Builder<String, PermissionNode<Boolean>> builder = ImmutableMap.builder();
        for (String permission : permissions) {
            PermissionNode<Boolean> node = new PermissionNode<>("spark", permission, PermissionTypes.BOOLEAN, defaultValue);
            e.addNodes(node);
            builder.put("spark." + permission, node);
        }
        this.registeredPermissions = builder.build();
    }
    */

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent e) {
        registerCommands(e.getDispatcher());
    }

    private void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
        registerCommands(dispatcher, this, this, "spark");
    }

    @Override
    public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
        String[] args = processArgs(context, false);
        if (args == null) {
            return 0;
        }

        this.threadDumper.ensureSetup();
        this.platform.executeCommand(new ForgeCommandSender(context.getSource().source, this), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String[] args = processArgs(context, true);
        if (args == null) {
            return Suggestions.empty();
        }

        return generateSuggestions(new ForgeCommandSender(context.getSource().getPlayerOrException(), this), args, builder);
    }

    private static String [] processArgs(CommandContext<CommandSource> context, boolean tabComplete) {
        String[] split = context.getInput().split(" ", tabComplete ? -1 : 0);
        if (split.length == 0 || !split[0].equals("/spark") && !split[0].equals("spark")) {
            return null;
        }

        return Arrays.copyOfRange(split, 1, split.length);
    }

    @Override
    public boolean hasPermission(ICommandSource sender, String permission) {
        if (sender instanceof PlayerEntity) {
            return PermissionAPI.hasPermission((PlayerEntity) sender, permission);
        } else {
            return true;
        }
    }

    @Override
    public Stream<ForgeCommandSender> getCommandSenders() {
        return Stream.concat(
            this.game.getPlayerList().getPlayers().stream(),
            Stream.of(this.game)
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
    public PlayerPingProvider createPlayerPingProvider() {
        return new ForgePlayerPingProvider(this.game);
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

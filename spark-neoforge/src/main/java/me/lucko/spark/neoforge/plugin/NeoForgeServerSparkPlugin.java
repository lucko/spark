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

package me.lucko.spark.neoforge.plugin;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.MetadataProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.neoforge.NeoForgeCommandSender;
import me.lucko.spark.neoforge.NeoForgeExtraMetadataProvider;
import me.lucko.spark.neoforge.NeoForgePlatformInfo;
import me.lucko.spark.neoforge.NeoForgePlayerPingProvider;
import me.lucko.spark.neoforge.NeoForgeServerConfigProvider;
import me.lucko.spark.neoforge.NeoForgeSparkMod;
import me.lucko.spark.neoforge.NeoForgeTickHook;
import me.lucko.spark.neoforge.NeoForgeTickReporter;
import me.lucko.spark.neoforge.NeoForgeWorldInfoProvider;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NeoForgeServerSparkPlugin extends NeoForgeSparkPlugin implements Command<CommandSourceStack>, SuggestionProvider<CommandSourceStack> {

    public static void register(NeoForgeSparkMod mod, ServerAboutToStartEvent event) {
        NeoForgeServerSparkPlugin plugin = new NeoForgeServerSparkPlugin(mod, event.getServer());
        plugin.enable();
    }

    private static final PermissionNode.PermissionResolver<Boolean> DEFAULT_PERMISSION_VALUE = (player, playerUUID, context) -> {
        if (player == null) {
            return false;
        }

        MinecraftServer server = player.getServer();
        if (server != null && server.isSingleplayerOwner(player.getGameProfile())) {
            return true;
        }

        return player.hasPermissions(4);
    };

    private final MinecraftServer server;
    private final ThreadDumper gameThreadDumper;
    private Map<String, PermissionNode<Boolean>> registeredPermissions = Collections.emptyMap();

    public NeoForgeServerSparkPlugin(NeoForgeSparkMod mod, MinecraftServer server) {
        super(mod);
        this.server = server;
        this.gameThreadDumper = new ThreadDumper.Specific(server.getRunningThread());
    }

    @Override
    public void enable() {
        super.enable();

        // register commands
        registerCommands(this.server.getCommands().getDispatcher());

        // register listeners
        NeoForge.EVENT_BUS.register(this);
    }

    @Override
    public void disable() {
        super.disable();

        // unregister listeners
        NeoForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onDisable(ServerStoppingEvent event) {
        if (event.getServer() == this.server) {
            disable();
        }
    }

    @SubscribeEvent
    public void onPermissionGather(PermissionGatherEvent.Nodes e) {
        // collect all possible permissions
        List<String> permissions = this.platform.getCommands().stream()
                .map(me.lucko.spark.common.command.Command::primaryAlias)
                .collect(Collectors.toList());

        // special case for the "spark" permission: map it to "spark.all"
        permissions.add("all");

        // register permissions with forge & keep a copy for lookup
        ImmutableMap.Builder<String, PermissionNode<Boolean>> builder = ImmutableMap.builder();

        Map<String, PermissionNode<?>> alreadyRegistered = e.getNodes().stream().collect(Collectors.toMap(PermissionNode::getNodeName, Function.identity()));

        for (String permission : permissions) {
            String permissionString = "spark." + permission;

            // there's a weird bug where it seems that this listener can be called twice, causing an
            // IllegalArgumentException to be thrown the second time e.addNodes is called.
            PermissionNode<?> existing = alreadyRegistered.get(permissionString);
            if (existing != null) {
                //noinspection unchecked
                builder.put(permissionString, (PermissionNode<Boolean>) existing);
                continue;
            }

            PermissionNode<Boolean> node = new PermissionNode<>("spark", permission, PermissionTypes.BOOLEAN, DEFAULT_PERMISSION_VALUE);
            e.addNodes(node);
            builder.put(permissionString, node);
        }
        this.registeredPermissions = builder.build();
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent e) {
        registerCommands(e.getDispatcher());
    }

    private void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerCommands(dispatcher, this, this, "spark");
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String[] args = processArgs(context, false, "/spark", "spark");
        if (args == null) {
            return 0;
        }

        CommandSource source = context.getSource().getEntity() != null ? context.getSource().getEntity() : context.getSource().getServer();
        this.platform.executeCommand(new NeoForgeCommandSender(source, this), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String[] args = processArgs(context, true, "/spark", "spark");
        if (args == null) {
            return Suggestions.empty();
        }

        return generateSuggestions(new NeoForgeCommandSender(context.getSource().getPlayerOrException(), this), args, builder);
    }

    @Override
    public boolean hasPermission(CommandSource sender, String permission) {
        if (sender instanceof ServerPlayer) {
            if (permission.equals("spark")) {
                permission = "spark.all";
            }

            PermissionNode<Boolean> permissionNode = this.registeredPermissions.get(permission);
            if (permissionNode == null) {
                throw new IllegalStateException("spark permission not registered: " + permission);
            }
            return PermissionAPI.getPermission((ServerPlayer) sender, permissionNode);
        } else {
            return true;
        }
    }

    @Override
    public Stream<NeoForgeCommandSender> getCommandSenders() {
        return Stream.concat(
            this.server.getPlayerList().getPlayers().stream(),
            Stream.of(this.server)
        ).map(sender -> new NeoForgeCommandSender(sender, this));
    }

    @Override
    public void executeSync(Runnable task) {
        this.server.executeIfPossible(task);
    }

    @Override
    public ThreadDumper getDefaultThreadDumper() {
        return this.gameThreadDumper;
    }

    @Override
    public TickHook createTickHook() {
        return new NeoForgeTickHook.Server();
    }

    @Override
    public TickReporter createTickReporter() {
        return new NeoForgeTickReporter.Server();
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new NeoForgePlayerPingProvider(this.server);
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new NeoForgeServerConfigProvider();
    }

    @Override
    public MetadataProvider createExtraMetadataProvider() {
        return new NeoForgeExtraMetadataProvider(this.server.getPackRepository());
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new NeoForgeWorldInfoProvider.Server(this.server);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new NeoForgePlatformInfo(PlatformInfo.Type.SERVER);
    }

    @Override
    public String getCommandName() {
        return "spark";
    }
}

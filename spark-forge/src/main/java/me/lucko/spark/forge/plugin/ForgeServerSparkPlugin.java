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

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.forge.ForgePlatformInfo;
import me.lucko.spark.forge.ForgePlayerPingProvider;
import me.lucko.spark.forge.ForgeServerCommandSender;
import me.lucko.spark.forge.ForgeServerConfigProvider;
import me.lucko.spark.forge.ForgeSparkMod;
import me.lucko.spark.forge.ForgeTickHook;
import me.lucko.spark.forge.ForgeTickReporter;
import me.lucko.spark.forge.ForgeWorldInfoProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.server.permission.PermissionAPI;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.nodes.PermissionNode;
import net.minecraftforge.server.permission.nodes.PermissionNode.PermissionResolver;
import net.minecraftforge.server.permission.nodes.PermissionTypes;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ForgeServerSparkPlugin extends ForgeSparkPlugin implements Command<CommandSourceStack>, SuggestionProvider<CommandSourceStack> {

    public static void register(ForgeSparkMod mod, ServerAboutToStartEvent event) {
        ForgeServerSparkPlugin plugin = new ForgeServerSparkPlugin(mod, event.getServer());
        plugin.enable();
    }

    private static final PermissionResolver<Boolean> DEFAULT_PERMISSION_VALUE = (player, playerUUID, context) -> {
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
    private Collection<EventListener> listeners = Collections.emptyList();

    public ForgeServerSparkPlugin(ForgeSparkMod mod, MinecraftServer server) {
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
        this.listeners = BusGroup.DEFAULT.register(MethodHandles.lookup(), this);
    }

    @Override
    public void disable() {
        super.disable();

        // unregister listeners
        if (!this.listeners.isEmpty()) {
            BusGroup.DEFAULT.unregister(this.listeners);
        }
        this.listeners = Collections.emptyList();
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

        this.platform.executeCommand(new ForgeServerCommandSender(context.getSource(), this), args);
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String[] args = processArgs(context, true, "/spark", "spark");
        if (args == null) {
            return Suggestions.empty();
        }

        return generateSuggestions(new ForgeServerCommandSender(context.getSource(), this), args, builder);
    }

    public boolean hasPermission(CommandSourceStack stack, String permission) {
        ServerPlayer player = stack.getPlayer();
        if (player != null) {
            if (permission.equals("spark")) {
                permission = "spark.all";
            }

            PermissionNode<Boolean> permissionNode = this.registeredPermissions.get(permission);
            if (permissionNode == null) {
                throw new IllegalStateException("spark permission not registered: " + permission);
            }
            return PermissionAPI.getPermission(player, permissionNode);
        } else {
            return true;
        }
    }

    @Override
    public Stream<ForgeServerCommandSender> getCommandSenders() {
        return Stream.concat(
            this.server.getPlayerList().getPlayers().stream().map(ServerPlayer::createCommandSourceStack),
            Stream.of(this.server.createCommandSourceStack())
        ).map(stack -> new ForgeServerCommandSender(stack, this));
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
        return new ForgeTickHook(TickEvent.Type.SERVER);
    }

    @Override
    public TickReporter createTickReporter() {
        return new ForgeTickReporter(TickEvent.Type.SERVER);
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new ForgePlayerPingProvider(this.server);
    }

    @Override
    public ServerConfigProvider createServerConfigProvider() {
        return new ForgeServerConfigProvider();
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new ForgeWorldInfoProvider.Server(this.server);
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

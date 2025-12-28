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

import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.forge.ForgeClassSourceLookup;
import me.lucko.spark.forge.ForgePlatformInfo;
import me.lucko.spark.forge.ForgeSparkMod;
import me.lucko.spark.forge.ForgeTickHook;
import me.lucko.spark.forge.ForgeTickReporter;
import me.lucko.spark.forge.ForgeWorldInfoProvider;
import me.lucko.spark.minecraft.plugin.MinecraftClientSparkPlugin;
import me.lucko.spark.minecraft.sender.MinecraftClientCommandSender;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.EventListener;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.forgespi.language.IModInfo;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

public class ForgeClientSparkPlugin extends MinecraftClientSparkPlugin<ForgeSparkMod, CommandSourceStack> {

    public static void init(ForgeSparkMod mod, FMLClientSetupEvent event) {
        ForgeClientSparkPlugin plugin = new ForgeClientSparkPlugin(mod, Minecraft.getInstance());
        plugin.enable();
    }

    private Collection<EventListener> listeners = Collections.emptyList();

    public ForgeClientSparkPlugin(ForgeSparkMod mod, Minecraft minecraft) {
        super(mod, minecraft, () -> minecraft.gameThread);
    }

    @Override
    public void enable() {
        super.enable();

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
    public void onRegisterClientCommands(RegisterClientCommandsEvent e) {
        registerCommands(e.getDispatcher(), this, this, "sparkc", "sparkclient");
    }

    // not used - workaround for the idiotic Forge limitation that requires listeners using the @SubscribeEvent annotation to have at least two listener methods
    @SubscribeEvent
    public void onRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {

    }

    @Override
    protected CommandSender createCommandSender(CommandSourceStack source) {
        return new MinecraftClientCommandSender(source);
    }

    @Override
    public Stream<MinecraftClientCommandSender> getCommandSenders() {
        return Stream.of(new MinecraftClientCommandSender(ClientCommandHandler.getSource()));
    }

    @Override
    public TickHook createTickHook() {
        return new ForgeTickHook.Client();
    }

    @Override
    public TickReporter createTickReporter() {
        return new ForgeTickReporter.Client();
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new ForgeClassSourceLookup();
    }

    @Override
    public Collection<SourceMetadata> getKnownSources() {
        return SourceMetadata.gather(
                ModList.get().getMods(),
                IModInfo::getModId,
                mod -> mod.getVersion().toString(),
                mod -> null, // ?
                IModInfo::getDescription
        );
    }

    @Override
    public WorldInfoProvider createWorldInfoProvider() {
        return new ForgeWorldInfoProvider.Client(this.minecraft);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new ForgePlatformInfo(PlatformInfo.Type.CLIENT);
    }
}

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

import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.tick.TickReporter;
import me.lucko.spark.minecraft.plugin.MinecraftClientSparkPlugin;
import me.lucko.spark.minecraft.sender.MinecraftClientCommandSender;
import me.lucko.spark.neoforge.NeoForgeClassSourceLookup;
import me.lucko.spark.neoforge.NeoForgePlatformInfo;
import me.lucko.spark.neoforge.NeoForgeSparkMod;
import me.lucko.spark.neoforge.NeoForgeTickHook;
import me.lucko.spark.neoforge.NeoForgeTickReporter;
import me.lucko.spark.neoforge.NeoForgeWorldInfoProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.ClientCommandHandler;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.Collection;
import java.util.stream.Stream;

public class NeoForgeClientSparkPlugin extends MinecraftClientSparkPlugin<NeoForgeSparkMod, CommandSourceStack> {

    public static void init(NeoForgeSparkMod mod, FMLClientSetupEvent event) {
        NeoForgeClientSparkPlugin plugin = new NeoForgeClientSparkPlugin(mod, Minecraft.getInstance());
        plugin.enable();
    }

    public NeoForgeClientSparkPlugin(NeoForgeSparkMod mod, Minecraft minecraft) {
        super(mod, minecraft, () -> minecraft.gameThread);
    }

    @Override
    public void enable() {
        super.enable();

        // register listeners
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterClientCommandsEvent e) {
        registerCommands(e.getDispatcher(), this, this, "sparkc", "sparkclient");
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
        return new NeoForgeTickHook.Client();
    }

    @Override
    public TickReporter createTickReporter() {
        return new NeoForgeTickReporter.Client();
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new NeoForgeClassSourceLookup();
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
        return new NeoForgeWorldInfoProvider.Client(this.minecraft);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new NeoForgePlatformInfo(PlatformInfo.Type.CLIENT);
    }
}

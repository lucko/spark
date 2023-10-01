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

package me.lucko.spark.geyser;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.monitor.ping.PlayerPingProvider;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;

import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.api.command.Command;
import org.geysermc.geyser.api.command.CommandSource;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCommandsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserShutdownEvent;
import org.geysermc.geyser.api.extension.Extension;
import org.geysermc.geyser.api.util.PlatformType;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

public class GeyserSparkExtension implements SparkPlugin, Extension {
    private SparkPlatform platform;

    @Subscribe
    public void onPreInitialize(GeyserPreInitializeEvent event) {
        if (this.geyserApi().platformType() != PlatformType.STANDALONE) {
            this.logger().severe("spark is only supported on standalone Geyser instances! If you wish to use it on other platforms please download the spark version for that platform.");
            this.disable();
        }
    }

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    @Subscribe
    public void onShutdown(GeyserShutdownEvent event) {
        if (this.platform != null) {
            this.platform.disable();
        }
    }

    @Subscribe
    public void onCommandDefine(GeyserDefineCommandsEvent event) {
        for (me.lucko.spark.common.command.Command command : this.platform.getCommands()) {
            // TODO Allow registering executor at the base
            event.register(Command.builder(this)
                .source(CommandSource.class)
                .name(command.primaryAlias())
                .aliases(command.aliases())
                .executor((source, ranCommand, args) -> {
                    List<String> listArgs = new ArrayList<>(Arrays.asList(args));
                    listArgs.add(0, command.primaryAlias());
                    this.platform.executeCommand(new GeyserCommandSender(source), listArgs.toArray(new String[0]));
                })
                .build());
        }
    }

    @Override
    public String getVersion() {
        return this.description().version();
    }

    @Override
    public Path getPluginDirectory() {
        return this.dataFolder();
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<GeyserCommandSender> getCommandSenders() {
        return Stream.concat(
                this.geyserApi().onlineConnections().stream(),
                Stream.of(((GeyserImpl) this.geyserApi()).getLogger())
        ).map(GeyserCommandSender::new);
    }

    @Override
    public void executeAsync(Runnable task) {
        GeyserImpl.getInstance().getScheduledThread().execute(task);
    }

    @Override
    public void log(Level level, String msg) {
        if (level == Level.INFO) {
            this.logger().info(msg);
        } else if (level == Level.WARNING) {
            this.logger().warning(msg);
        } else if (level == Level.SEVERE) {
            this.logger().error(msg);
        } else {
            throw new IllegalArgumentException(level.getName());
        }
    }

    @Override
    public ClassSourceLookup createClassSourceLookup() {
        return new GeyserClassSourceLookup();
    }

    @Override
    public PlayerPingProvider createPlayerPingProvider() {
        return new GeyserPlayerPingProvider(this.geyserApi());
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new GeyserPlatformInfo(this.geyserApi());
    }
}

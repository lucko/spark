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

package me.lucko.spark.test.plugin;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.util.classfinder.ClassFinder;
import me.lucko.spark.common.util.classfinder.FallbackClassFinder;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class TestSparkPlugin implements SparkPlugin, AutoCloseable {

    private static final Logger LOGGER = Logger.getLogger("spark-test");
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(16);

    private final Path directory;
    private final Map<String, String> props;

    private final SparkPlatform platform;

    public TestSparkPlugin(Path directory, Map<String, String> config) {
        this.directory = directory;
        this.props = new HashMap<>(config);
        this.props.putIfAbsent("backgroundProfiler", "false");

        this.props.forEach((k, v) -> System.setProperty("spark." + k, v));
        this.platform = new SparkPlatform(this);
        this.platform.enable();
    }

    public TestSparkPlugin(Path directory) {
        this(directory, Collections.emptyMap());
    }

    public SparkPlatform platform() {
        return this.platform;
    }

    @Override
    public void close() {
        this.platform.disable();
        this.props.keySet().forEach((k) -> System.clearProperty("spark." + k));
    }

    @Override
    public String getVersion() {
        return "1.0-test";
    }

    @Override
    public Path getPluginDirectory() {
        return this.directory;
    }

    @Override
    public String getCommandName() {
        return "spark";
    }

    @Override
    public Stream<? extends CommandSender> getCommandSenders() {
        return Stream.of(TestCommandSender.INSTANCE);
    }

    @Override
    public void executeAsync(Runnable task) {
        EXECUTOR_SERVICE.execute(task);
    }

    @Override
    public void log(Level level, String msg) {
        LOGGER.log(level, msg);
    }

    @Override
    public void log(Level level, String msg, Throwable throwable) {
        LOGGER.log(level, msg, throwable);
    }

    @Override
    public PlatformInfo getPlatformInfo() {
        return new PlatformInfo() {
            @Override
            public Type getType() {
                return Type.SERVER;
            }

            @Override
            public String getName() {
                return "Test";
            }

            @Override
            public String getBrand() {
                return "Test";
            }

            @Override
            public String getVersion() {
                return "v1.0-test";
            }

            @Override
            public String getMinecraftVersion() {
                return null;
            }
        };
    }

    @Override
    public ClassFinder createClassFinder() {
        return FallbackClassFinder.INSTANCE;
    }
}

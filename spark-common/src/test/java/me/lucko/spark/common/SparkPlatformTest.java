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

package me.lucko.spark.common;

import me.lucko.spark.test.plugin.TestCommandSender;
import me.lucko.spark.test.plugin.TestSparkPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

public class SparkPlatformTest {

    @Test
    public void testEnableDisable(@TempDir Path directory) {
        System.setProperty("spark.backgroundProfiler", "false");

        SparkPlatform platform = new SparkPlatform(new TestSparkPlugin(directory));
        platform.enable();

        platform.executeCommand(TestCommandSender.INSTANCE, new String[]{"help"}).join();
        platform.executeCommand(TestCommandSender.INSTANCE, new String[]{"profiler", "info"}).join();
        platform.executeCommand(TestCommandSender.INSTANCE, new String[]{"health"}).join();

        platform.disable();
    }

}

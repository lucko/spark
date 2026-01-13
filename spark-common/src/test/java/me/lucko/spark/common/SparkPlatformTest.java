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

import com.google.common.collect.ImmutableSet;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.test.plugin.TestSparkPlugin;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SparkPlatformTest {

    @Test
    public void testEnableDisable(@TempDir Path directory) {
        try (TestSparkPlugin plugin = new TestSparkPlugin(directory)) {
            assertTrue(plugin.platform().hasEnabled());
        }
    }

    @Test
    public void testPermissions(@TempDir Path directory) {
        try (TestSparkPlugin plugin = new TestSparkPlugin(directory)) {
            SparkPlatform platform = plugin.platform();

            Set<String> permissions = platform.getCommandManager().getAllSparkPermissions();
            assertEquals(
                    ImmutableSet.of(
                            "spark",
                            "spark.profiler",
                            "spark.tps",
                            "spark.ping",
                            "spark.healthreport",
                            "spark.gc",
                            "spark.gcmonitor",
                            "spark.heapsummary",
                            "spark.heapdump",
                            "spark.activity"
                    ),
                    permissions
            );

            TestCommandSender testSender = new TestCommandSender();
            assertFalse(platform.hasPermissionForAnyCommand(testSender));

            testSender.permissions.add("spark.tps");
            assertTrue(platform.hasPermissionForAnyCommand(testSender));

            testSender.permissions.clear();
            testSender.permissions.add("spark");
            assertTrue(platform.hasPermissionForAnyCommand(testSender));
        }
    }

    private static final class TestCommandSender implements CommandSender {
        private final Set<String> permissions = new HashSet<>();

        @Override
        public String getName() {
            return "Test";
        }

        @Override
        public UUID getUniqueId() {
            return new UUID(0, 0);
        }

        @Override
        public void sendMessage(Component message) {

        }

        @Override
        public boolean hasPermission(String permission) {
            return this.permissions.contains(permission);
        }
    }

}

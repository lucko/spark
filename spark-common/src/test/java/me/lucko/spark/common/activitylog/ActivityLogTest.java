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

package me.lucko.spark.common.activitylog;

import me.lucko.spark.common.command.sender.CommandSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActivityLogTest {

    private static final CommandSender.Data USER = new CommandSender.Data("Test", UUID.fromString("5937921d-7051-45e1-bac7-3bbfdc12444f"));

    @Test
    public void testSaveLoad(@TempDir Path tempDir) {
        ActivityLog log = new ActivityLog(tempDir.resolve("activity-log.json"));
        log.addToLog(Activity.fileActivity(USER, 1721937782184L, "Profiler", "path/to/profile.sparkprofile"));
        log.addToLog(Activity.urlActivity(USER, 1721937782184L, "Profiler", "https://spark.lucko.me/abcd"));
        log.save();

        ActivityLog log2 = new ActivityLog(tempDir.resolve("activity-log.json"));
        log2.load();

        // check the log contents
        assertEquals(
                log.getLog().stream().map(Activity::serialize).collect(Collectors.toList()),
                log2.getLog().stream().map(Activity::serialize).collect(Collectors.toList())
        );
    }

}

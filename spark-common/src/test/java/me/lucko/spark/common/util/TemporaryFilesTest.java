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

package me.lucko.spark.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TemporaryFilesTest {

    @Test
    public void testDelete(@TempDir Path tempDir) throws IOException {
        Path dir = tempDir.resolve("test");
        TemporaryFiles temporaryFiles = new TemporaryFiles(dir);

        assertTrue(Files.exists(dir) && Files.isDirectory(dir));
        assertTrue(Files.exists(dir.resolve("about.txt")));
        assertEquals("# What is this directory?", Files.readAllLines(dir.resolve("about.txt")).get(0));

        Path temporaryFile = temporaryFiles.create("test", ".txt");
        Files.write(temporaryFile, "Hello, world!".getBytes());

        assertTrue(Files.exists(temporaryFile));
        temporaryFiles.deleteTemporaryFiles();
        assertFalse(Files.exists(temporaryFile));
    }

    @Test
    public void testCleanupOnInit(@TempDir Path tempDir) throws IOException {
        Path dir = tempDir.resolve("test");

        Path nestedDirectory = dir.resolve("hello").resolve("world");
        Files.createDirectories(nestedDirectory);

        Path testFile = nestedDirectory.resolve("file.txt");
        Files.write(testFile, "Hello, world!".getBytes());
        assertTrue(Files.exists(testFile));

        TemporaryFiles temporaryFiles = new TemporaryFiles(dir);

        assertFalse(Files.exists(testFile));
    }

    @Test
    public void testSecondInit(@TempDir Path tempDir) throws IOException {
        Path dir = tempDir.resolve("test");

        TemporaryFiles temporaryFiles = new TemporaryFiles(dir);
        TemporaryFiles temporaryFiles2 = new TemporaryFiles(dir);
    }

}

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

import com.google.common.collect.ImmutableList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility for handling temporary files.
 */
public final class TemporaryFiles {

    public static final FileAttribute<?>[] OWNER_ONLY_FILE_PERMISSIONS;

    static {
        boolean isPosix = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
        if (isPosix) {
            OWNER_ONLY_FILE_PERMISSIONS = new FileAttribute[]{PosixFilePermissions.asFileAttribute(EnumSet.of(
                    PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE
            ))};
        } else {
            OWNER_ONLY_FILE_PERMISSIONS = new FileAttribute[0];
        }
    }

    private final Path tmpDirectory;
    private final Set<Path> files = Collections.synchronizedSet(new HashSet<>());

    public TemporaryFiles(Path tmpDirectory) {
        this.tmpDirectory = tmpDirectory;
    }

    public Path create(String prefix, String suffix) throws IOException {
        Path file;
        if (ensureDirectoryIsReady()) {
            String name = prefix + Long.toHexString(System.nanoTime()) + suffix;
            file = Files.createFile(this.tmpDirectory.resolve(name), OWNER_ONLY_FILE_PERMISSIONS);
        } else {
            file = Files.createTempFile(prefix, suffix);
        }
        return register(file);
    }

    public Path register(Path path) {
        path.toFile().deleteOnExit();
        this.files.add(path);
        return path;
    }

    public void deleteTemporaryFiles() {
        synchronized (this.files) {
            for (Iterator<Path> iterator = this.files.iterator(); iterator.hasNext(); ) {
                Path path = iterator.next();
                try {
                    Files.deleteIfExists(path);
                } catch (IOException e) {
                    // ignore
                }
                iterator.remove();
            }
        }
    }

    private boolean ensureDirectoryIsReady() {
        if (Boolean.parseBoolean(System.getProperty("spark.useOsTmpDir", "false"))) {
            return false;
        }

        if (Files.isDirectory(this.tmpDirectory)) {
            return true;
        }

        try {
            Files.createDirectories(this.tmpDirectory);

            Files.write(this.tmpDirectory.resolve("about.txt"), ImmutableList.of(
                    "# What is this directory?",
                    "",
                    "* In order to perform certain functions, spark sometimes needs to write temporary data to the disk. ",
                    "* Previously, a temporary directory provided by the operating system was used for this purpose. ",
                    "* However, this proved to be unreliable in some circumstances, so spark now stores temporary data here instead!",
                    "",
                    "spark will automatically cleanup the contents of this directory. " ,
                    "(but if for some reason it doesn't, if the server is stopped, you can freely delete any files ending in .tmp)",
                    "",
                    "tl;dr: spark uses this folder to store some temporary data."
            ), StandardCharsets.UTF_8);

            return true;
        } catch (IOException e) {
            return false;
        }
    }

}

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Utility for handling temporary files.
 */
public final class TemporaryFiles {
    private TemporaryFiles() {}

    private static final Set<Path> DELETE_SET = Collections.synchronizedSet(new HashSet<>());

    public static Path create(String prefix, String suffix) throws IOException {
        return register(Files.createTempFile(prefix, suffix));
    }

    public static Path register(Path path) {
        path.toFile().deleteOnExit();
        DELETE_SET.add(path);
        return path;
    }

    public static void deleteTemporaryFiles() {
        synchronized (DELETE_SET) {
            for (Iterator<Path> iterator = DELETE_SET.iterator(); iterator.hasNext(); ) {
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

}

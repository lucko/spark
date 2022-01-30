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

package me.lucko.spark.common.monitor.disk;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Exposes the system disk usage.
 */
public enum DiskUsage {
    ;

    private static final FileStore FILE_STORE;

    static {
        FileStore fileStore = null;
        try {
            fileStore = Files.getFileStore(Paths.get("."));
        } catch (IOException e) {
            // ignore
        }
        FILE_STORE = fileStore;
    }

    public static long getUsed() {
        if (FILE_STORE == null) {
            return 0;
        }

        try {
            long total = FILE_STORE.getTotalSpace();
            return total - FILE_STORE.getUsableSpace();
        } catch (IOException e) {
            return 0;
        }
    }

    public static long getTotal() {
        if (FILE_STORE == null) {
            return 0;
        }

        try {
            return FILE_STORE.getTotalSpace();
        } catch (IOException e) {
            return 0;
        }
    }

}

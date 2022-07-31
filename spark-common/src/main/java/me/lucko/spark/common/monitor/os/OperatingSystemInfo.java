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

package me.lucko.spark.common.monitor.os;

import me.lucko.spark.common.util.LinuxProc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public enum OperatingSystemInfo {
    ;

    private static String name = null;
    private static String version = null;

    static {
        final String osNameJavaProp = System.getProperty("os.name");

        if (osNameJavaProp.startsWith("Windows")) {
            final String[] args = { "wmic", "os", "get", "caption,version", "/FORMAT:list" };
            try (final BufferedReader buf = new BufferedReader(new InputStreamReader(new ProcessBuilder(args).redirectErrorStream(true).start().getInputStream()))) {
                String line;
                while ((line = buf.readLine()) != null) {
                    if (line.startsWith("Caption")) {
                        name = line.substring(18).trim();
                    } else if (line.startsWith("Version")) {
                        version = line.substring(8).trim();
                    }
                }
            } catch (final IOException | IndexOutOfBoundsException e) {
                // ignore
            }
        } else {
            for (final String line : LinuxProc.OSINFO.read()) {
                if (line.startsWith("PRETTY_NAME")) {
                    try {
                        name = line.substring(12).trim();
                    } catch (final IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }
        }

        if (name == null)
            name = osNameJavaProp;

        if (version == null)
            version = System.getProperty("os.version");
    }

    public static String getName() {
        return name;
    }

    public static String getVersion() {
        return version;
    }
}

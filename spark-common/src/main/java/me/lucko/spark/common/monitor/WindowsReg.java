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

package me.lucko.spark.common.monitor;

import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for reading from Windows Registry on Windows systems.
 * A replacement for deprecated WMIC.
 */
public enum WindowsReg {

    /**
     * Gets the CPU name from the registry
     */
    CPU_GET_NAME("reg", "query", "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString"),

    /**
     * Gets the operating system name (ProductName) and version (CurrentBuild).
     * Modern JVMs handle OS name/version natively via System.getProperty,
     * but this is retained if native registry readout is strictly needed.
     */
    OS_GET_INFO("reg", "query", "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion");

    private static final boolean SUPPORTED = System.getProperty("os.name").startsWith("Windows");

    private final String[] cmdArgs;

    WindowsReg(String... cmdArgs) {
        this.cmdArgs = cmdArgs;
    }

    public @NonNull List<String> read() {
        if (SUPPORTED) {
            ProcessBuilder process = new ProcessBuilder(this.cmdArgs).redirectErrorStream(true);
            try (BufferedReader buf = new BufferedReader(new InputStreamReader(process.start().getInputStream()))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = buf.readLine()) != null) {
                    if (!line.trim().isEmpty()) {
                        lines.add(line);
                    }
                }
                return lines;
            } catch (Exception ignored) { }
        }
        return Collections.emptyList();
    }
}

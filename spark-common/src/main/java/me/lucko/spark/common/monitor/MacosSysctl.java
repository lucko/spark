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

import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Utility for reading from sysctl on macOS systems.
 */
public enum MacosSysctl {

    SYSCTL("sysctl", "-a"),;

    private static final boolean SUPPORTED = System.getProperty("os.name").toLowerCase(Locale.ROOT).replace(" ", "").equals("macosx");

    private final String[] cmdArgs;

    MacosSysctl(String... cmdArgs) {
        this.cmdArgs = cmdArgs;
    }

    public @NonNull List<String> read() {
        if (SUPPORTED) {
            ProcessBuilder process = new ProcessBuilder(this.cmdArgs).redirectErrorStream(true);
            try (BufferedReader buf = new BufferedReader(new InputStreamReader(process.start().getInputStream()))) {
                List<String> lines = new ArrayList<>();

                String line;
                while ((line = buf.readLine()) != null) {
                    lines.add(line);
                }

                return lines;
            } catch (Exception e) {
                // ignore
            }
        }

        return Collections.emptyList();
    }
}


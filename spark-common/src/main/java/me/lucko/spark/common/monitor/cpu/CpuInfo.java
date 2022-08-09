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

package me.lucko.spark.common.monitor.cpu;

import me.lucko.spark.common.monitor.LinuxProc;
import me.lucko.spark.common.monitor.WindowsWmic;

import java.util.regex.Pattern;

/**
 * Small utility to query the CPU model on Linux and Windows systems.
 */
public enum CpuInfo {
    ;

    private static final Pattern SPACE_COLON_SPACE_PATTERN = Pattern.compile("\\s+:\\s");

    /**
     * Queries the CPU model.
     *
     * @return the cpu model
     */
    public static String queryCpuModel() {
        for (String line : LinuxProc.CPUINFO.read()) {
            String[] splitLine = SPACE_COLON_SPACE_PATTERN.split(line);
            if (splitLine[0].equals("model name") || splitLine[0].equals("Processor")) {
                return splitLine[1];
            }
        }

        for (String line : WindowsWmic.CPU_GET_NAME.read()) {
            if (line.startsWith("Name")) {
                return line.substring(5).trim();
            }
        }

        return "";
    }

}

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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.Locale;

public enum FormatUtil {
    ;

    private static final String[] SIZE_UNITS = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};

    public static String percent(double value, double max) {
        double percent = (value * 100d) / max;
        return (int) percent + "%";
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) {
            return "0 bytes";
        }
        int sizeIndex = (int) (Math.log(bytes) / Math.log(1024));
        return String.format(Locale.ENGLISH, "%.1f", bytes / Math.pow(1024, sizeIndex)) + " " + SIZE_UNITS[sizeIndex];
    }

    public static Component formatBytes(long bytes, TextColor color, String suffix) {
        String value;
        String unit;

        if (bytes <= 0) {
            value = "0";
            unit = "KB" + suffix;
        } else {
            int sizeIndex = (int) (Math.log(bytes) / Math.log(1024));
            value = String.format(Locale.ENGLISH, "%.1f", bytes / Math.pow(1024, sizeIndex));
            unit = SIZE_UNITS[sizeIndex] + suffix;
        }

        return Component.text()
                .append(Component.text(value, color))
                .append(Component.space())
                .append(Component.text(unit))
                .build();
    }

    public static String formatSeconds(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }

        long second = seconds;
        long minute = second / 60;
        second = second % 60;

        StringBuilder sb = new StringBuilder();
        if (minute != 0) {
            sb.append(minute).append("m ");
        }
        if (second != 0) {
            sb.append(second).append("s ");
        }

        return sb.toString().trim();
    }
}

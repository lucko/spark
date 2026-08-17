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

import com.google.common.base.Strings;
import me.lucko.spark.api.statistic.misc.DoubleAverageInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;

import java.lang.management.MemoryUsage;
import java.util.Locale;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GREEN;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.YELLOW;

public enum StatisticFormatter {
    ;

    private static final String BAR_TRUE_CHARACTER = "┃";
    private static final String BAR_FALSE_CHARACTER = "╻";

    public static TextComponent formatTps(double tps, int gameTargetTps) {
        TextColor color;

        if (tps > (gameTargetTps * 0.9d)) {
            color = GREEN;
        } else if (tps > (gameTargetTps * 0.8d)) {
            color = YELLOW;
        } else {
            color = RED;
        }

        return text((tps > gameTargetTps ? "*" : "") + Math.min(Math.round(tps * 100.0) / 100.0, gameTargetTps), color);
    }

    public static TextComponent formatTickDurations(DoubleAverageInfo average, int gameMaxIdealDuration) {
        return text()
                .append(formatTickDuration(average.min(), gameMaxIdealDuration))
                .append(text('/', GRAY))
                .append(formatTickDuration(average.median(), gameMaxIdealDuration))
                .append(text('/', GRAY))
                .append(formatTickDuration(average.percentile95th(), gameMaxIdealDuration))
                .append(text('/', GRAY))
                .append(formatTickDuration(average.max(), gameMaxIdealDuration))
                .build();
    }

    public static TextComponent formatTickDuration(double duration, int gameMaxIdealDuration) {
        TextColor color;
        if (duration >= gameMaxIdealDuration) {
            color = RED;
        } else if (duration >= (gameMaxIdealDuration * 0.8d)) {
            color = YELLOW;
        } else {
            color = GREEN;
        }

        return text(String.format(Locale.ENGLISH, "%.1f", duration), color);
    }

    public static TextComponent formatCpuUsage(double usage) {
        TextColor color;
        if (usage > 0.9) {
            color = RED;
        } else if (usage > 0.65) {
            color = YELLOW;
        } else {
            color = GREEN;
        }

        return text(FormatUtil.percent(usage, 1d), color);
    }

    public static TextComponent formatPingRtts(double min, double median, double percentile95th, double max) {
        return text()
                .append(formatPingRtt(min))
                .append(text('/', GRAY))
                .append(formatPingRtt(median))
                .append(text('/', GRAY))
                .append(formatPingRtt(percentile95th))
                .append(text('/', GRAY))
                .append(formatPingRtt(max))
                .build();
    }

    public static TextComponent formatPingRtt(double ping) {
        TextColor color;
        if (ping >= 200) {
            color = RED;
        } else if (ping >= 100) {
            color = YELLOW;
        } else {
            color = GREEN;
        }

        return text((int) Math.ceil(ping), color);
    }

    public static TextComponent generateMemoryUsageDiagram(MemoryUsage usage, int length) {
        double used = usage.getUsed();
        double committed = usage.getCommitted();
        double max = usage.getMax();

        int usedChars = (int) ((used * length) / max);
        int committedChars = (int) ((committed * length) / max);

        TextComponent.Builder line = text().content(Strings.repeat(BAR_TRUE_CHARACTER, usedChars)).color(YELLOW);
        if (committedChars > usedChars) {
            line.append(text(Strings.repeat(BAR_FALSE_CHARACTER, (committedChars - usedChars) - 1), GRAY));
            line.append(Component.text(BAR_FALSE_CHARACTER, RED));
        }
        if (length > committedChars) {
            line.append(text(Strings.repeat(BAR_FALSE_CHARACTER, (length - committedChars)), GRAY));
        }

        return text()
                .append(text("[", DARK_GRAY))
                .append(line.build())
                .append(text("]", DARK_GRAY))
                .build();
    }

    public static TextComponent generateMemoryPoolDiagram(MemoryUsage usage, MemoryUsage collectionUsage, int length) {
        double used = usage.getUsed();
        double collectionUsed = used;
        if (collectionUsage != null) {
            collectionUsed = collectionUsage.getUsed();
        }
        double committed = usage.getCommitted();
        double max = usage.getMax();

        int usedChars = (int) ((used * length) / max);
        int collectionUsedChars = (int) ((collectionUsed * length) / max);
        int committedChars = (int) ((committed * length) / max);

        TextComponent.Builder line = text().content(Strings.repeat(BAR_TRUE_CHARACTER, collectionUsedChars)).color(YELLOW);

        if (usedChars > collectionUsedChars) {
            line.append(Component.text(BAR_TRUE_CHARACTER, RED));
            line.append(text(Strings.repeat(BAR_TRUE_CHARACTER, (usedChars - collectionUsedChars) - 1), YELLOW));
        }
        if (committedChars > usedChars) {
            line.append(text(Strings.repeat(BAR_FALSE_CHARACTER, (committedChars - usedChars) - 1), GRAY));
            line.append(Component.text(BAR_FALSE_CHARACTER, YELLOW));
        }
        if (length > committedChars) {
            line.append(text(Strings.repeat(BAR_FALSE_CHARACTER, (length - committedChars)), GRAY));
        }

        return text()
                .append(text("[", DARK_GRAY))
                .append(line.build())
                .append(text("]", DARK_GRAY))
                .build();
    }

    public static TextComponent generateDiskUsageDiagram(double used, double max, int length) {
        int usedChars = (int) ((used * length) / max);
        int freeChars = length - usedChars;
        return text()
                .append(text("[", DARK_GRAY))
                .append(text(Strings.repeat(BAR_TRUE_CHARACTER, usedChars), YELLOW))
                .append(text(Strings.repeat(BAR_FALSE_CHARACTER, freeChars), GRAY))
                .append(text("]", DARK_GRAY))
                .build();
    }
}

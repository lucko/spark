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

package me.lucko.spark.bukkit.placeholder;

import me.lucko.spark.bukkit.BukkitSparkPlugin;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.util.SparkPlaceholder;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import be.maximvdw.placeholderapi.PlaceholderReplaceEvent;
import be.maximvdw.placeholderapi.PlaceholderReplacer;

public class SparkMVdWPlaceholders implements PlaceholderReplacer {
    private final SparkPlatform platform;

    public SparkMVdWPlaceholders(BukkitSparkPlugin plugin, SparkPlatform platform) {
        this.platform = platform;
        PlaceholderAPI.registerPlaceholder(plugin, "spark_*", this);
    }

    @Override
    public String onPlaceholderReplace(PlaceholderReplaceEvent event) {
        String placeholder = event.getPlaceholder();
        if (!placeholder.startsWith("spark_")) {
            return null;
        }

        String identifier = placeholder.substring("spark_".length());
        return SparkPlaceholder.resolveFormattingCode(this.platform, identifier);
    }
}

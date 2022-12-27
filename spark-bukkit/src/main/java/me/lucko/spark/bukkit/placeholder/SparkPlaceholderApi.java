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

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.lucko.spark.bukkit.BukkitSparkPlugin;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.util.SparkPlaceholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class SparkPlaceholderApi extends PlaceholderExpansion {
    private final BukkitSparkPlugin plugin;
    private final SparkPlatform platform;

    public SparkPlaceholderApi(BukkitSparkPlugin plugin, SparkPlatform platform) {
        this.plugin = plugin;
        this.platform = platform;
        register();
    }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        return onRequest(null, params);
    }

    @Override
    public String onRequest(OfflinePlayer p, String params) {
        return SparkPlaceholder.resolveFormattingCode(this.platform, params);
    }

    @Override
    public String getIdentifier() {
        return "spark";
    }

    @Override
    public String getAuthor() {
        return String.join(", ", this.plugin.getDescription().getAuthors());
    }

    @Override
    public String getVersion() {
        return this.plugin.getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }
}

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

package me.lucko.spark.nukkit;

import cn.nukkit.Server;
import me.lucko.spark.common.platform.PlatformInfo;

public class NukkitPlatformInfo implements PlatformInfo {
    private final Server server;

    public NukkitPlatformInfo(Server server) {
        this.server = server;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "Nukkit";
    }

    @Override
    public String getBrand() {
        return this.server.getName();
    }

    @Override
    public String getVersion() {
        return this.server.getNukkitVersion();
    }

    @Override
    public String getMinecraftVersion() {
        return this.server.getVersion();
    }
}

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

package me.lucko.spark.velocity;

import com.velocitypowered.api.proxy.ProxyServer;
import me.lucko.spark.common.platform.PlatformInfo;

public class Velocity4PlatformInfo implements PlatformInfo {
    private final ProxyServer proxy;

    public Velocity4PlatformInfo(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public Type getType() {
        return Type.PROXY;
    }

    @Override
    public String getName() {
        return "Velocity";
    }

    @Override
    public String getBrand() {
        return this.proxy.version().name();
    }

    @Override
    public String getVersion() {
        return this.proxy.version().version();
    }

    @Override
    public String getMinecraftVersion() {
        return null;
    }
}

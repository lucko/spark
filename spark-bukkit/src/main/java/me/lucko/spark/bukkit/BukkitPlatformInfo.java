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

package me.lucko.spark.bukkit;

import me.lucko.spark.common.platform.AbstractPlatformInfo;
import org.bukkit.Server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class BukkitPlatformInfo extends AbstractPlatformInfo {
    private final Server server;

    public BukkitPlatformInfo(Server server) {
        this.server = server;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "Bukkit";
    }

    @Override
    public String getVersion() {
        return this.server.getVersion();
    }

    @Override
    public String getMinecraftVersion() {
        try {
            return this.server.getMinecraftVersion();
        } catch (NoSuchMethodError e) {
            // ignore
        }

        Class<? extends Server> serverClass = this.server.getClass();
        try {
            Field minecraftServerField = serverClass.getDeclaredField("console");
            minecraftServerField.setAccessible(true);

            Object minecraftServer = minecraftServerField.get(this.server);
            Class<?> minecraftServerClass = minecraftServer.getClass();

            Method getVersionMethod = minecraftServerClass.getDeclaredMethod("getVersion");
            getVersionMethod.setAccessible(true);

            return (String) getVersionMethod.invoke(minecraftServer);
        } catch (Exception e) {
            // ignore
        }

        return serverClass.getPackage().getName().split("\\.")[3];
    }
}

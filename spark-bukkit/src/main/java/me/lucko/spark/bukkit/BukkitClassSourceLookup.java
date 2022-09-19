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

import me.lucko.spark.common.sampler.source.ClassSourceLookup;

import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;

public class BukkitClassSourceLookup extends ClassSourceLookup.ByClassLoader {
    private static final Class<?> PLUGIN_CLASS_LOADER;
    private static final Field PLUGIN_FIELD;

    static {
        try {
            PLUGIN_CLASS_LOADER = Class.forName("org.bukkit.plugin.java.PluginClassLoader");
            PLUGIN_FIELD = PLUGIN_CLASS_LOADER.getDeclaredField("plugin");
            PLUGIN_FIELD.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public String identify(ClassLoader loader) throws ReflectiveOperationException {
        if (PLUGIN_CLASS_LOADER.isInstance(loader)) {
            JavaPlugin plugin = (JavaPlugin) PLUGIN_FIELD.get(loader);
            return plugin.getName();
        }
        return null;
    }
}


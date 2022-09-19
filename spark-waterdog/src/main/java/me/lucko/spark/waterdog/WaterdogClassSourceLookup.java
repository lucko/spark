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

package me.lucko.spark.waterdog;

import me.lucko.spark.common.sampler.source.ClassSourceLookup;

import dev.waterdog.waterdogpe.ProxyServer;
import dev.waterdog.waterdogpe.plugin.Plugin;
import dev.waterdog.waterdogpe.plugin.PluginClassLoader;

import java.util.Map;
import java.util.WeakHashMap;

public class WaterdogClassSourceLookup extends ClassSourceLookup.ByClassLoader {
    private final ProxyServer proxy;
    private final Map<ClassLoader, String> cache;

    public WaterdogClassSourceLookup(ProxyServer proxy) {
        this.proxy = proxy;
        this.cache = new WeakHashMap<>();
    }

    @Override
    public String identify(ClassLoader loader) throws ReflectiveOperationException {
        if (loader instanceof PluginClassLoader) {
            String name = this.cache.get(loader);
            if (name != null) {
                return name;
            }

            for (Plugin plugin : this.proxy.getPluginManager().getPlugins()) {
                if (plugin.getClass().getClassLoader() == loader) {
                    name = plugin.getName();
                    break;
                }
            }

            this.cache.put(loader, name);
            return name;
        }
        return null;
    }
}


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

package me.lucko.spark.common.util.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum RuntimeConfiguration implements Configuration {
    SYSTEM_PROPERTIES {
        @Override
        public String getString(String path, String def) {
            return System.getProperty("spark." + path, def);
        }
    },

    ENVIRONMENT_VARIABLES {
        @Override
        public String getString(String path, String def) {
            String name = "SPARK_" + path.replace(".", "_").replace("-", "_").toUpperCase();
            String value = System.getenv(name);
            return value != null ? value : def;
        }
    };

    @Override
    public boolean getBoolean(String path, boolean def) {
        return Boolean.parseBoolean(getString(path, Boolean.toString(def)));
    }

    @Override
    public int getInteger(String path, int def) {
        try {
            return Integer.parseInt(getString(path, Integer.toString(def)));
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public List<String> getStringList(String path) {
        String value = getString(path, "");
        if (value.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(value.split(","));
    }

    @Override
    public boolean contains(String path) {
        return getString(path, null) != null;
    }

    @Override
    public void load() {
        // no-op
    }

    @Override
    public void save() {
        // no-op
    }

    @Override
    public void setString(String path, String value) {
        // no-op
    }

    @Override
    public void setBoolean(String path, boolean value) {
        // no-op
    }

    @Override
    public void setInteger(String path, int value) {
        // no-op
    }

    @Override
    public void setStringList(String path, List<String> value) {
        // no-op
    }

    @Override
    public void remove(String path) {
        // no-op
    }
}

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

package me.lucko.spark.fabric.smap;

import org.spongepowered.asm.mixin.transformer.Config;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public enum MixinUtils {
    ;

    private static final Map<String, Config> MIXIN_CONFIGS;

    static {
        Map<String, Config> configs;
        try {
            Field allConfigsField = Config.class.getDeclaredField("allConfigs");
            allConfigsField.setAccessible(true);

            //noinspection unchecked
            configs = (Map<String, Config>) allConfigsField.get(null);
        } catch (Exception e) {
            e.printStackTrace();
            configs = new HashMap<>();
        }
        MIXIN_CONFIGS = configs;
    }

    public static Map<String, Config> getMixinConfigs() {
        return MIXIN_CONFIGS;
    }
}

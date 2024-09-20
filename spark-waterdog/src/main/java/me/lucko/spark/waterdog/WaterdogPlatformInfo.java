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

import dev.waterdog.waterdogpe.WaterdogPE;
import me.lucko.spark.common.platform.PlatformInfo;

public class WaterdogPlatformInfo implements PlatformInfo {

    @Override
    public Type getType() {
        return Type.PROXY;
    }

    @Override
    public String getName() {
        return "Waterdog";
    }

    @Override
    public String getBrand() {
        return "Waterdog";
    }

    @Override
    public String getVersion() {
        return WaterdogPE.version().baseVersion();
    }

    @Override
    public String getMinecraftVersion() {
        return null;
    }
}

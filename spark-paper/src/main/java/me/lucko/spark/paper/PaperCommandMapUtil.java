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

package me.lucko.spark.paper;

import java.util.Map;
import me.lucko.spark.bukkit.common.AbstractCommandMapUtil;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

public final class PaperCommandMapUtil extends AbstractCommandMapUtil {

    private final Server server;

    public PaperCommandMapUtil(final Server server) {
        this.server = server;
    }

    @Override
    public CommandMap getCommandMap() {
        return this.server.getCommandMap();
    }

    @Override
    public Map<String, Command> getKnownCommandMap(CommandMap commandMap) {
        return commandMap.getKnownCommands();
    }
}

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

package me.lucko.spark.neoforge;

import me.lucko.spark.minecraft.permission.PermissionRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.function.Function;
import java.util.stream.Collectors;

public class NeoForgePermissionRegistry extends PermissionRegistry<PermissionNode<?>, PermissionNode<Boolean>> {
    private static final Permission PERMISSION_LEVEL_OWNERS = new Permission.HasCommandLevel(PermissionLevel.OWNERS);
    private static final PermissionNode.PermissionResolver<Boolean> DEFAULT_PERMISSION_VALUE = (player, playerUniqueId, context) -> {
        if (player == null) {
            return false;
        }

        MinecraftServer server = player.level().getServer();
        if (server != null && server.isSingleplayerOwner(player.nameAndId())) {
            return true;
        }

        return player.permissions().hasPermission(PERMISSION_LEVEL_OWNERS);
    };

    public NeoForgePermissionRegistry(PermissionGatherEvent.Nodes event) {
        super(
                event.getNodes().stream().collect(Collectors.toMap(PermissionNode::getNodeName, Function.identity())),
                event::addNodes
        );
    }

    @Override
    protected PermissionNode<Boolean> createNode(String permission) {
        return new PermissionNode<>("spark", permission, PermissionTypes.BOOLEAN, DEFAULT_PERMISSION_VALUE);
    }
}

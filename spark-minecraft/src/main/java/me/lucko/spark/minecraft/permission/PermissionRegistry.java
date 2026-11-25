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

package me.lucko.spark.minecraft.permission;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A registry for spark permissions in NeoForge and Forge.
 *
 * <p>Both platforms require permissions to be registered at startup before they can be used.</p>
 *
 * @param <NT> node type - always {@code PermissionNode<?>}
 * @param <BNT> boolean node type - always {@code PermissionNode<Boolean>}
 */
public abstract class PermissionRegistry<NT, BNT> {

    // TODO: add unit test to ensure this is kept in sync with the commands
    private static final List<String> PERMISSIONS = List.of(
            "all",
            "profiler",
            "tps",
            "ping",
            "health",
            "gc",
            "gcmonitor",
            "heapsummary",
            "heapdump",
            "activity"
    );

    private final Map<String, BNT> registeredPermissions;

    public PermissionRegistry(Map<String, NT> alreadyRegistered, Consumer<BNT> registerNode) {
        ImmutableMap.Builder<String, BNT> builder = ImmutableMap.builder();

        for (String permission : PERMISSIONS) {
            String permissionString = "spark." + permission;

            // there's a weird bug where it seems that the event to initialize can be called twice, causing an
            // IllegalArgumentException to be thrown the second time the register node callback is called.
            NT existing = alreadyRegistered.get(permissionString);
            if (existing != null) {
                //noinspection unchecked
                builder.put(permissionString, (BNT) existing);
                continue;
            }

            BNT node = createNode(permission);
            registerNode.accept(node);
            builder.put(permissionString, node);
        }
        this.registeredPermissions = builder.build();
    }

    protected abstract BNT createNode(String permission);

    public BNT getPermissionNode(String permission) {
        if (permission.equals("spark")) {
            permission = "spark.all";
        }

        BNT permissionNode = this.registeredPermissions.get(permission);
        if (permissionNode == null) {
            throw new IllegalStateException("spark permission not registered: " + permission);
        }
        return permissionNode;
    }

}

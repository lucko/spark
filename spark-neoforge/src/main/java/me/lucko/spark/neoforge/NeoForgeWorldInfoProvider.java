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

import me.lucko.spark.minecraft.MinecraftWorldInfoProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.TransientEntitySectionManager;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public class NeoForgeWorldInfoProvider {

    public static final class Server extends MinecraftWorldInfoProvider.Server {
        public Server(MinecraftServer server) {
            super(server);
        }

        @Override
        protected int countEntities(ServerLevel level) {
            if (ModList.get().isLoaded("moonrise")) {
                return MoonriseMethods.getEntityCount(level.getEntities());
            } else {
                PersistentEntitySectionManager<Entity> entityManager = level.entityManager;
                EntityLookup<Entity> entityIndex = entityManager.visibleEntityStorage;
                return entityIndex.count();
            }
        }
    }

    public static final class Client extends MinecraftWorldInfoProvider.Client {
        public Client(Minecraft client) {
            super(client);
        }

        @Override
        protected int countEntities(ClientLevel level) {
            if (ModList.get().isLoaded("moonrise")) {
                return MoonriseMethods.getEntityCount(level.getEntities());
            } else {
                TransientEntitySectionManager<Entity> entityManager = level.entityStorage;
                EntityLookup<Entity> entityIndex = entityManager.entityStorage;
                return entityIndex.count();
            }
        }

        @Override
        protected Iterable<Entity> getAllEntities(ClientLevel level) {
            return level.getEntities().getAll();
        }
    }

    private static final class MoonriseMethods {
        private static Method getEntityCount;

        private static Method getEntityCountMethod(LevelEntityGetter<Entity> getter) {
            if (getEntityCount == null) {
                try {
                    getEntityCount = getter.getClass().getMethod("getEntityCount");
                } catch (final ReflectiveOperationException e) {
                    throw new RuntimeException("Cannot find Moonrise getEntityCount method", e);
                }
            }
            return getEntityCount;
        }

        private static int getEntityCount(LevelEntityGetter<Entity> getter) {
            try {
                return (int) getEntityCountMethod(getter).invoke(getter);
            } catch (final ReflectiveOperationException e) {
                throw new RuntimeException("Failed to invoke Moonrise getEntityCount method", e);
            }
        }
    }

}

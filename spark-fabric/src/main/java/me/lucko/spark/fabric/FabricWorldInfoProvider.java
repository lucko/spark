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

package me.lucko.spark.fabric;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.fabric.mixin.ClientEntityManagerAccessor;
import me.lucko.spark.fabric.mixin.ClientWorldAccessor;
import me.lucko.spark.fabric.mixin.ServerEntityManagerAccessor;
import me.lucko.spark.fabric.mixin.ServerWorldAccessor;
import me.lucko.spark.fabric.mixin.WorldAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.entity.ClientEntityManager;
import net.minecraft.world.entity.EntityIndex;
import net.minecraft.world.entity.EntityLookup;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public abstract class FabricWorldInfoProvider implements WorldInfoProvider {

    protected abstract ResourcePackManager getResourcePackManager();

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return getResourcePackManager().getEnabledProfiles().stream()
                .map(pack -> new DataPackInfo(
                        pack.getId(),
                        pack.getDescription().getString(),
                        resourcePackSource(pack.getSource())
                ))
                .collect(Collectors.toList());
    }

    private static String resourcePackSource(ResourcePackSource source) {
        if (source == ResourcePackSource.NONE) {
            return "none";
        } else if (source == ResourcePackSource.BUILTIN) {
            return "builtin";
        } else if (source == ResourcePackSource.WORLD) {
            return "world";
        } else if (source == ResourcePackSource.SERVER) {
            return "server";
        } else {
            return "unknown";
        }
    }

    public static final class Server extends FabricWorldInfoProvider {
        private final MinecraftServer server;

        public Server(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public CountsResult pollCounts() {
            int players = this.server.getCurrentPlayerCount();
            int entities = 0;
            int chunks = 0;

            for (ServerWorld world : this.server.getWorlds()) {

                if (FabricLoader.getInstance().isModLoaded("moonrise")) {
                    entities += MoonriseMethods.getEntityCount(((WorldAccessor) world).spark$getEntityLookup());
                } else {
                    ServerEntityManager<Entity> entityManager = ((ServerWorldAccessor) world).getEntityManager();
                    EntityIndex<?> entityIndex = ((ServerEntityManagerAccessor) entityManager).getIndex();
                    entities += entityIndex.size();
                }

                chunks += world.getChunkManager().getLoadedChunkCount();
            }

            return new CountsResult(players, entities, -1, chunks);
        }

        @Override
        public ChunksResult<FabricChunkInfo> pollChunks() {
            ChunksResult<FabricChunkInfo> data = new ChunksResult<>();

            for (ServerWorld world : this.server.getWorlds()) {
                Long2ObjectOpenHashMap<FabricChunkInfo> worldInfos = new Long2ObjectOpenHashMap<>();

                for (Entity entity : ((WorldAccessor) world).spark$getEntityLookup().iterate()) {
                    FabricChunkInfo info = worldInfos.computeIfAbsent(
                        entity.getChunkPos().toLong(), FabricChunkInfo::new);
                    info.entityCounts.increment(entity.getType());
                }

                data.put(world.getRegistryKey().getValue().getPath(), List.copyOf(worldInfos.values()));
            }

            return data;
        }

        @Override
        public GameRulesResult pollGameRules() {
            GameRulesResult data = new GameRulesResult();
            Iterable<ServerWorld> worlds = this.server.getWorlds();

            for (ServerWorld world : worlds) {
                String worldName = world.getRegistryKey().getValue().getPath();

                world.getGameRules().accept(new GameRules.Visitor() {
                    @Override
                    public <T extends GameRules.Rule<T>> void visit(GameRules.Key<T> key, GameRules.Type<T> type) {
                        String defaultValue = type.createRule().serialize();
                        data.putDefault(key.getName(), defaultValue);

                        String value = world.getGameRules().get(key).serialize();
                        data.put(key.getName(), worldName, value);
                    }
                });
            }
            return data;
        }

        @Override
        protected ResourcePackManager getResourcePackManager() {
            return this.server.getDataPackManager();
        }
    }

    public static final class Client extends FabricWorldInfoProvider {
        private final MinecraftClient client;

        public Client(MinecraftClient client) {
            this.client = client;
        }

        @Override
        public CountsResult pollCounts() {
            ClientWorld world = this.client.world;
            if (world == null) {
                return null;
            }

            int entities;

            if (FabricLoader.getInstance().isModLoaded("moonrise")) {
                entities = MoonriseMethods.getEntityCount(((WorldAccessor) world).spark$getEntityLookup());
            } else {
                ClientEntityManager<Entity> entityManager = ((ClientWorldAccessor) world).getEntityManager();
                EntityIndex<?> entityIndex = ((ClientEntityManagerAccessor) entityManager).getIndex();
                entities = entityIndex.size();
            }

            int chunks = world.getChunkManager().getLoadedChunkCount();

            return new CountsResult(-1, entities, -1, chunks);
        }

        @Override
        public ChunksResult<FabricChunkInfo> pollChunks() {
            ClientWorld world = this.client.world;
            if (world == null) {
                return null;
            }

            ChunksResult<FabricChunkInfo> data = new ChunksResult<>();

            Long2ObjectOpenHashMap<FabricChunkInfo> worldInfos = new Long2ObjectOpenHashMap<>();

            for (Entity entity : ((WorldAccessor) world).spark$getEntityLookup().iterate()) {
                FabricChunkInfo info = worldInfos.computeIfAbsent(entity.getChunkPos().toLong(), FabricChunkInfo::new);
                info.entityCounts.increment(entity.getType());
            }

            data.put(world.getRegistryKey().getValue().getPath(), List.copyOf(worldInfos.values()));

            return data;
        }

        @Override
        public GameRulesResult pollGameRules() {
            // Not available on client since 24w39a
            return null;
        }

        @Override
        protected ResourcePackManager getResourcePackManager() {
            return this.client.getResourcePackManager();
        }
    }

    static final class FabricChunkInfo extends AbstractChunkInfo<EntityType<?>> {
        private final CountMap<EntityType<?>> entityCounts;

        FabricChunkInfo(long chunkPos) {
            super(ChunkPos.getPackedX(chunkPos), ChunkPos.getPackedZ(chunkPos));

            this.entityCounts = new CountMap.Simple<>(new HashMap<>());
        }

        @Override
        public CountMap<EntityType<?>> getEntityCounts() {
            return this.entityCounts;
        }

        @Override
        public String entityTypeName(EntityType<?> type) {
            return EntityType.getId(type).toString();
        }
    }

    private static final class MoonriseMethods {
        private static Method getEntityCount;

        private static Method getEntityCountMethod(EntityLookup<Entity> getter) {
            if (getEntityCount == null) {
                try {
                    getEntityCount = getter.getClass().getMethod("getEntityCount");
                } catch (final ReflectiveOperationException e) {
                    throw new RuntimeException("Cannot find Moonrise getEntityCount method", e);
                }
            }
            return getEntityCount;
        }

        private static int getEntityCount(EntityLookup<Entity> getter) {
            try {
                return (int) getEntityCountMethod(getter).invoke(getter);
            } catch (final ReflectiveOperationException e) {
                throw new RuntimeException("Failed to invoke Moonrise getEntityCount method", e);
            }
        }
    }

}


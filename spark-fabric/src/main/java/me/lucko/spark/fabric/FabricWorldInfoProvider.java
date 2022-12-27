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

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;

import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import me.lucko.spark.fabric.mixin.ClientEntityManagerAccessor;
import me.lucko.spark.fabric.mixin.ClientWorldAccessor;
import me.lucko.spark.fabric.mixin.ServerEntityManagerAccessor;
import me.lucko.spark.fabric.mixin.ServerWorldAccessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientEntityManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerEntityManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.entity.EntityIndex;
import net.minecraft.world.entity.EntityTrackingSection;
import net.minecraft.world.entity.SectionedEntityCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public abstract class FabricWorldInfoProvider implements WorldInfoProvider {

    protected List<FabricChunkInfo> getChunksFromCache(SectionedEntityCache<Entity> cache) {
        LongSet loadedChunks = cache.getChunkPositions();
        List<FabricChunkInfo> list = new ArrayList<>(loadedChunks.size());

        for (LongIterator iterator = loadedChunks.iterator(); iterator.hasNext(); ) {
            long chunkPos = iterator.nextLong();
            Stream<EntityTrackingSection<Entity>> sections = cache.getTrackingSections(chunkPos);

            list.add(new FabricChunkInfo(chunkPos, sections));
        }

        return list;
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
                ServerEntityManager<Entity> entityManager = ((ServerWorldAccessor) world).getEntityManager();
                EntityIndex<?> entityIndex = ((ServerEntityManagerAccessor) entityManager).getIndex();

                entities += entityIndex.size();
                chunks += world.getChunkManager().getLoadedChunkCount();
            }

            return new CountsResult(players, entities, -1, chunks);
        }

        @Override
        public ChunksResult<FabricChunkInfo> pollChunks() {
            ChunksResult<FabricChunkInfo> data = new ChunksResult<>();

            for (ServerWorld world : this.server.getWorlds()) {
                ServerEntityManager<Entity> entityManager = ((ServerWorldAccessor) world).getEntityManager();
                SectionedEntityCache<Entity> cache = ((ServerEntityManagerAccessor) entityManager).getCache();

                List<FabricChunkInfo> list = getChunksFromCache(cache);
                data.put(world.getRegistryKey().getValue().getPath(), list);
            }

            return data;
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

            ClientEntityManager<Entity> entityManager = ((ClientWorldAccessor) world).getEntityManager();
            EntityIndex<?> entityIndex = ((ClientEntityManagerAccessor) entityManager).getIndex();

            int entities = entityIndex.size();
            int chunks = world.getChunkManager().getLoadedChunkCount();

            return new CountsResult(-1, entities, -1, chunks);
        }

        @Override
        public ChunksResult<FabricChunkInfo> pollChunks() {
            ChunksResult<FabricChunkInfo> data = new ChunksResult<>();

            ClientWorld world = this.client.world;
            if (world == null) {
                return null;
            }

            ClientEntityManager<Entity> entityManager = ((ClientWorldAccessor) world).getEntityManager();
            SectionedEntityCache<Entity> cache = ((ClientEntityManagerAccessor) entityManager).getCache();

            List<FabricChunkInfo> list = getChunksFromCache(cache);
            data.put(world.getRegistryKey().getValue().getPath(), list);

            return data;
        }
    }

    static final class FabricChunkInfo extends AbstractChunkInfo<EntityType<?>> {
        private final CountMap<EntityType<?>> entityCounts;

        FabricChunkInfo(long chunkPos, Stream<EntityTrackingSection<Entity>> entities) {
            super(ChunkPos.getPackedX(chunkPos), ChunkPos.getPackedZ(chunkPos));

            this.entityCounts = new CountMap.Simple<>(new HashMap<>());
            entities.forEach(section -> {
                if (section.getStatus().shouldTrack()) {
                    section.stream().forEach(entity ->
                            this.entityCounts.increment(entity.getType())
                    );
                }
            });
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

}


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

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityLookup;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.entity.TransientEntitySectionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public abstract class NeoForgeWorldInfoProvider implements WorldInfoProvider {

    protected List<ForgeChunkInfo> getChunksFromCache(EntitySectionStorage<Entity> cache) {
        LongSet loadedChunks = cache.getAllChunksWithExistingSections();
        List<ForgeChunkInfo> list = new ArrayList<>(loadedChunks.size());

        for (LongIterator iterator = loadedChunks.iterator(); iterator.hasNext(); ) {
            long chunkPos = iterator.nextLong();
            Stream<EntitySection<Entity>> sections = cache.getExistingSectionsInChunk(chunkPos);

            list.add(new ForgeChunkInfo(chunkPos, sections));
        }

        return list;
    }

    public static final class Server extends NeoForgeWorldInfoProvider {
        private final MinecraftServer server;

        public Server(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public CountsResult pollCounts() {
            int players = this.server.getPlayerCount();
            int entities = 0;
            int chunks = 0;

            for (ServerLevel level : this.server.getAllLevels()) {
                PersistentEntitySectionManager<Entity> entityManager = level.entityManager;
                EntityLookup<Entity> entityIndex = entityManager.visibleEntityStorage;

                entities += entityIndex.count();
                chunks += level.getChunkSource().getLoadedChunksCount();
            }

            return new CountsResult(players, entities, -1, chunks);
        }

        @Override
        public ChunksResult<ForgeChunkInfo> pollChunks() {
            ChunksResult<ForgeChunkInfo> data = new ChunksResult<>();

            for (ServerLevel level : this.server.getAllLevels()) {
                PersistentEntitySectionManager<Entity> entityManager = level.entityManager;
                EntitySectionStorage<Entity> cache = entityManager.sectionStorage;

                List<ForgeChunkInfo> list = getChunksFromCache(cache);
                data.put(level.dimension().location().getPath(), list);
            }

            return data;
        }
    }

    public static final class Client extends NeoForgeWorldInfoProvider {
        private final Minecraft client;

        public Client(Minecraft client) {
            this.client = client;
        }

        @Override
        public CountsResult pollCounts() {
            ClientLevel level = this.client.level;
            if (level == null) {
                return null;
            }

            TransientEntitySectionManager<Entity> entityManager = level.entityStorage;
            EntityLookup<Entity> entityIndex = entityManager.entityStorage;

            int entities = entityIndex.count();
            int chunks = level.getChunkSource().getLoadedChunksCount();

            return new CountsResult(-1, entities, -1, chunks);
        }

        @Override
        public ChunksResult<ForgeChunkInfo> pollChunks() {
            ChunksResult<ForgeChunkInfo> data = new ChunksResult<>();

            ClientLevel level = this.client.level;
            if (level == null) {
                return null;
            }

            TransientEntitySectionManager<Entity> entityManager = level.entityStorage;
            EntitySectionStorage<Entity> cache = entityManager.sectionStorage;

            List<ForgeChunkInfo> list = getChunksFromCache(cache);
            data.put(level.dimension().location().getPath(), list);

            return data;
        }
    }

    public static final class ForgeChunkInfo extends AbstractChunkInfo<EntityType<?>> {
        private final CountMap<EntityType<?>> entityCounts;

        ForgeChunkInfo(long chunkPos, Stream<EntitySection<Entity>> entities) {
            super(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));

            this.entityCounts = new CountMap.Simple<>(new HashMap<>());
            entities.forEach(section -> {
                if (section.getStatus().isAccessible()) {
                    section.getEntities().forEach(entity ->
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
            return EntityType.getKey(type).toString();
        }
    }


}

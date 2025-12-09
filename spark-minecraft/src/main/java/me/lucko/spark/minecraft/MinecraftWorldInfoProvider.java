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

package me.lucko.spark.minecraft;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.gamerules.GameRule;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public abstract class MinecraftWorldInfoProvider implements WorldInfoProvider {

    protected abstract PackRepository getPackRepository();

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return getPackRepository().getSelectedPacks().stream()
                .map(pack -> new DataPackInfo(
                        pack.getId(),
                        pack.getDescription().getString(),
                        resourcePackSource(pack.getPackSource())
                ))
                .collect(Collectors.toList());
    }

    private static String resourcePackSource(PackSource source) {
        if (source == PackSource.DEFAULT) {
            return "none";
        } else if (source == PackSource.BUILT_IN) {
            return "builtin";
        } else if (source == PackSource.WORLD) {
            return "world";
        } else if (source == PackSource.SERVER) {
            return "server";
        } else {
            return "unknown";
        }
    }

    public static abstract class Server extends MinecraftWorldInfoProvider {
        private final MinecraftServer server;

        public Server(MinecraftServer server) {
            this.server = server;
        }

        protected abstract int countEntities(ServerLevel level);

        @Override
        public CountsResult pollCounts() {
            int players = this.server.getPlayerCount();
            int entities = 0;
            int chunks = 0;

            for (ServerLevel level : this.server.getAllLevels()) {
                entities += countEntities(level);
                chunks += level.getChunkSource().getLoadedChunksCount();
            }

            return new CountsResult(players, entities, -1, chunks);
        }

        @Override
        public ChunksResult<MinecraftChunkInfo> pollChunks() {
            ChunksResult<MinecraftChunkInfo> data = new ChunksResult<>();

            for (ServerLevel level : this.server.getAllLevels()) {
                Long2ObjectOpenHashMap<MinecraftChunkInfo> worldInfos = new Long2ObjectOpenHashMap<>();

                for (Entity entity : level.getAllEntities()) {
                    MinecraftChunkInfo info = worldInfos.computeIfAbsent(entity.chunkPosition().toLong(), MinecraftChunkInfo::new);
                    info.entityCounts.increment(entity.getType());
                }

                data.put(level.dimension().identifier().getPath(), List.copyOf(worldInfos.values()));
            }

            return data;
        }

        @Override
        public GameRulesResult pollGameRules() {
            GameRulesResult data = new GameRulesResult();
            Iterable<ServerLevel> worlds = this.server.getAllLevels();

            for (ServerLevel level : worlds) {
                String levelName = level.dimension().identifier().getPath();

                level.getGameRules().availableRules().forEach(rule -> {
                    String defaultValue = gameRuleDefaultValue(rule);
                    data.putDefault(rule.id(), defaultValue);

                    String value = level.getGameRules().getAsString(rule);
                    data.put(rule.id(), levelName, value);
                });
            }
            return data;
        }

        private static <T> String gameRuleDefaultValue(GameRule<T> rule) {
            return rule.serialize(rule.defaultValue());
        }

        @Override
        protected PackRepository getPackRepository() {
            return this.server.getPackRepository();
        }
    }

    public static abstract class Client extends MinecraftWorldInfoProvider {
        private final Minecraft client;

        public Client(Minecraft client) {
            this.client = client;
        }

        protected abstract int countEntities(ClientLevel level);

        @Override
        public CountsResult pollCounts() {
            ClientLevel level = this.client.level;
            if (level == null) {
                return null;
            }

            int entities = countEntities(level);
            int chunks = level.getChunkSource().getLoadedChunksCount();

            return new CountsResult(-1, entities, -1, chunks);
        }

        protected abstract Iterable<Entity> getAllEntities(ClientLevel level);

        @Override
        public ChunksResult<MinecraftChunkInfo> pollChunks() {
            ClientLevel level = this.client.level;
            if (level == null) {
                return null;
            }

            ChunksResult<MinecraftChunkInfo> data = new ChunksResult<>();
            Long2ObjectOpenHashMap<MinecraftChunkInfo> worldInfos = new Long2ObjectOpenHashMap<>();

            for (Entity entity : getAllEntities(level)) {
                MinecraftChunkInfo info = worldInfos.computeIfAbsent(entity.chunkPosition().toLong(), MinecraftChunkInfo::new);
                info.entityCounts.increment(entity.getType());
            }

            data.put(level.dimension().identifier().getPath(), List.copyOf(worldInfos.values()));
            return data;
        }

        @Override
        public GameRulesResult pollGameRules() {
            // Not available on client since 24w39a
            return null;
        }

        @Override
        protected PackRepository getPackRepository() {
            return this.client.getResourcePackRepository();
        }
    }

    static final class MinecraftChunkInfo extends AbstractChunkInfo<EntityType<?>> {
        private final CountMap<EntityType<?>> entityCounts;

        MinecraftChunkInfo(long chunkPos) {
            super(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
            this.entityCounts = new CountMap.Simple<>(new HashMap<>());
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


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

package me.lucko.spark.sponge;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.api.Server;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.EntityType;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.world.chunk.WorldChunk;
import org.spongepowered.api.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class SpongeWorldInfoProvider implements WorldInfoProvider {
    private final Server server;

    public SpongeWorldInfoProvider(Server server) {
        this.server = server;
    }

    @Override
    public CountsResult pollCounts() {
        int players = this.server.onlinePlayers().size();
        int entities = 0;
        int tileEntities = 0;
        int chunks = 0;

        for (ServerWorld world : this.server.worldManager().worlds()) {
            entities += world.entities().size();
            tileEntities += world.blockEntities().size();
            chunks += Iterables.size(world.loadedChunks());
        }

        return new CountsResult(players, entities, tileEntities, chunks);
    }

    @Override
    public ChunksResult<SpongeChunkInfo> pollChunks() {
        ChunksResult<SpongeChunkInfo> data = new ChunksResult<>();

        for (ServerWorld world : this.server.worldManager().worlds()) {
            List<WorldChunk> chunks = Lists.newArrayList(world.loadedChunks());

            List<SpongeChunkInfo> list = new ArrayList<>(chunks.size());
            for (WorldChunk chunk : chunks) {
                list.add(new SpongeChunkInfo(chunk));
            }

            data.put(world.key().value(), list);
        }

        return data;
    }

    @Override
    public GameRulesResult pollGameRules() {
        GameRulesResult data = new GameRulesResult();

        Collection<ServerWorld> worlds = this.server.worldManager().worlds();
        for (ServerWorld world : worlds) {
            String worldName = world.key().value();

            world.properties().gameRules().forEach((gameRule, value) -> {
                String defaultValue = gameRule.defaultValue().toString();
                data.putDefault(gameRule.name(), defaultValue);

                data.put(gameRule.name(), worldName, value.toString());
            });
        }

        return data;
    }

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return this.server.packRepository().enabled().stream()
                .map(pack -> new DataPackInfo(
                        pack.id(),
                        PlainTextComponentSerializer.plainText().serialize(pack.description()),
                        "unknown"
                ))
                .collect(Collectors.toList());
    }

    static final class SpongeChunkInfo extends AbstractChunkInfo<EntityType<?>> {
        private final CountMap<EntityType<?>> entityCounts;

        SpongeChunkInfo(WorldChunk chunk) {
            super(chunk.chunkPosition().x(), chunk.chunkPosition().z());

            this.entityCounts = new CountMap.Simple<>(new HashMap<>());
            for (Entity entity : chunk.entities()) {
                this.entityCounts.increment(entity.type());
            }
        }

        @Override
        public CountMap<EntityType<?>> getEntityCounts() {
            return this.entityCounts;
        }

        @Override
        public String entityTypeName(EntityType<?> type) {
            return EntityTypes.registry().valueKey(type).value();
        }

    }
}

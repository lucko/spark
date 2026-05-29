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

package me.lucko.spark.common.platform.world;

import me.lucko.spark.proto.SparkProtos.WorldStatistics;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class WorldStatisticsProvider {
    private final AsyncWorldInfoProvider provider;

    public WorldStatisticsProvider(AsyncWorldInfoProvider provider) {
        this.provider = provider;
    }

    public WorldStatistics getWorldStatistics() {
        WorldInfoProvider.ChunksResult<? extends ChunkInfo<?>> result = this.provider.getChunks();
        if (result == null) {
            return null;
        }

        WorldStatistics.Builder stats = WorldStatistics.newBuilder();

        AtomicInteger combinedTotal = new AtomicInteger();
        CountMap<String> combined = new CountMap.Simple<>(new HashMap<>());

        result.getWorlds().forEach((worldName, chunks) -> {
            WorldStatistics.World.Builder builder = WorldStatistics.World.newBuilder();
            builder.setName(worldName);

            List<Region> regions = groupIntoRegions(chunks);

            int total = 0;

            for (Region region : regions) {
                builder.addRegions(regionToProto(region, combined));
                total += region.getTotalEntities().get();
            }

            builder.setTotalEntities(total);
            combinedTotal.addAndGet(total);

            stats.addWorlds(builder.build());
        });

        stats.setTotalEntities(combinedTotal.get());
        combined.asMap().forEach((key, value) -> stats.putEntityCounts(key, value.get()));

        WorldInfoProvider.GameRulesResult gameRules = this.provider.getGameRules();
        if (gameRules != null) {
            gameRules.getRules().forEach((ruleName, rule) -> stats.addGameRules(WorldStatistics.GameRule.newBuilder()
                    .setName(ruleName)
                    .setDefaultValue(rule.getDefaultValue())
                    .putAllWorldValues(rule.getWorldValues())
                    .build()
            ));
        }

        Collection<WorldInfoProvider.DataPackInfo> dataPacks = this.provider.getDataPacks();
        if (dataPacks != null) {
            dataPacks.forEach(dataPack -> stats.addDataPacks(WorldStatistics.DataPack.newBuilder()
                    .setName(dataPack.name())
                    .setDescription(dataPack.description())
                    .setSource(dataPack.source())
                    .build()
            ));
        }

        return stats.build();
    }

    private static WorldStatistics.Region regionToProto(Region region, CountMap<String> combined) {
        WorldStatistics.Region.Builder builder = WorldStatistics.Region.newBuilder();
        builder.setTotalEntities(region.getTotalEntities().get());
        for (ChunkInfo<?> chunk : region.getChunks()) {
            builder.addChunks(chunkToProto(chunk, combined));
        }
        return builder.build();
    }

    private static <E> WorldStatistics.Chunk chunkToProto(ChunkInfo<E> chunk, CountMap<String> combined) {
        WorldStatistics.Chunk.Builder builder = WorldStatistics.Chunk.newBuilder();
        builder.setX(chunk.getX());
        builder.setZ(chunk.getZ());
        builder.setTotalEntities(chunk.getEntityCounts().total().get());
        chunk.getEntityCounts().asMap().forEach((key, value) -> {
            String name = chunk.entityTypeName(key);
            int count = value.get();

            if (name == null) {
                name = "unknown[" + key.toString() + "]";
            }

            builder.putEntityCounts(name, count);
            combined.add(name, count);
        });
        return builder.build();
    }

    @VisibleForTesting
    static List<Region> groupIntoRegions(List<? extends ChunkInfo<?>> chunks) {
        List<Region> regions = new ArrayList<>();

        LinkedHashMap<ChunkCoordinate, ChunkInfo<?>> chunkMap = new LinkedHashMap<>(chunks.size());

        for (ChunkInfo<?> chunk : chunks) {
            CountMap<?> counts = chunk.getEntityCounts();
            if (counts.total().get() == 0) {
                continue;
            }
            chunkMap.put(new ChunkCoordinate(chunk.getX(), chunk.getZ()), chunk);
        }

        ArrayDeque<ChunkInfo<?>> queue = new ArrayDeque<>();
        ChunkCoordinate index = new ChunkCoordinate(); // avoid allocating per check

        while (!chunkMap.isEmpty()) {
            Map.Entry<ChunkCoordinate, ChunkInfo<?>> first = chunkMap.entrySet().iterator().next();
            ChunkInfo<?> firstValue = first.getValue();

            chunkMap.remove(first.getKey());

            Region region = new Region(firstValue);
            regions.add(region);

            queue.add(firstValue);

            ChunkInfo<?> queued;
            while ((queued = queue.pollFirst()) != null) {
                int queuedX = queued.getX();
                int queuedZ = queued.getZ();

                // merge adjacent chunks
                for (int dz = -1; dz <= 1; ++dz) {
                    for (int dx = -1; dx <= 1; ++dx) {
                        if ((dx | dz) == 0) {
                            continue;
                        }

                        index.setCoordinate(queuedX + dx, queuedZ + dz);
                        ChunkInfo<?> adjacent = chunkMap.remove(index);

                        if (adjacent == null) {
                            continue;
                        }

                        region.add(adjacent);
                        queue.add(adjacent);
                    }
                }
            }
        }

        return regions;
    }

    /**
     * A map of nearby chunks grouped together by Euclidean distance.
     */
    static final class Region {
        private final Set<ChunkInfo<?>> chunks;
        private final AtomicInteger totalEntities;

        private Region(ChunkInfo<?> initial) {
            this.chunks = new HashSet<>();
            this.chunks.add(initial);
            this.totalEntities = new AtomicInteger(initial.getEntityCounts().total().get());
        }

        public Set<ChunkInfo<?>> getChunks() {
            return this.chunks;
        }

        public AtomicInteger getTotalEntities() {
            return this.totalEntities;
        }

        public void add(ChunkInfo<?> chunk) {
            this.chunks.add(chunk);
            this.totalEntities.addAndGet(chunk.getEntityCounts().total().get());
        }
    }

    static final class ChunkCoordinate implements Comparable<ChunkCoordinate> {
        long key;

        ChunkCoordinate() {}

        ChunkCoordinate(int chunkX, int chunkZ) {
            this.setCoordinate(chunkX, chunkZ);
        }

        ChunkCoordinate(long key) {
            this.setKey(key);
        }

        public void setCoordinate(int chunkX, int chunkZ) {
            this.setKey(((long) chunkZ << 32) | (chunkX & 0xFFFFFFFFL));
        }

        public void setKey(long key) {
            this.key = key;
        }

        @Override
        public int hashCode() {
            // fastutil hash without the last step, as it is done by HashMap
            // doing the last step twice (h ^= (h >>> 16)) is both more expensive and destroys the hash
            long h = this.key * 0x9E3779B97F4A7C15L;
            h ^= h >>> 32;
            return (int) h;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ChunkCoordinate)) {
                return false;
            }
            return this.key == ((ChunkCoordinate) obj).key;
        }

        @Override
        public int compareTo(ChunkCoordinate other) {
            return Long.compare(this.key, other.key);
        }
    }
}

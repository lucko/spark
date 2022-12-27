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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class WorldStatisticsProvider {
    private final AsyncWorldInfoProvider provider;

    public WorldStatisticsProvider(AsyncWorldInfoProvider provider) {
        this.provider = provider;
    }

    public WorldStatistics getWorldStatistics() {
        WorldInfoProvider.ChunksResult<? extends ChunkInfo<?>> result = provider.getChunks();
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

    private static List<Region> groupIntoRegions(List<? extends ChunkInfo<?>> chunks) {
        List<Region> regions = new ArrayList<>();

        for (ChunkInfo<?> chunk : chunks) {
            CountMap<?> counts = chunk.getEntityCounts();
            if (counts.total().get() == 0) {
                continue;
            }

            boolean found = false;

            for (Region region : regions) {
                if (region.isAdjacent(chunk)) {
                    found = true;
                    region.add(chunk);

                    // if the chunk is adjacent to more than one region, merge the regions together
                    for (Iterator<Region> iterator = regions.iterator(); iterator.hasNext(); ) {
                        Region otherRegion = iterator.next();
                        if (region != otherRegion && otherRegion.isAdjacent(chunk)) {
                            iterator.remove();
                            region.merge(otherRegion);
                        }
                    }

                    break;
                }
            }

            if (!found) {
                regions.add(new Region(chunk));
            }
        }

        return regions;
    }

    /**
     * A map of nearby chunks grouped together by Euclidean distance.
     */
    private static final class Region {
        private static final int DISTANCE_THRESHOLD = 2;
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

        public boolean isAdjacent(ChunkInfo<?> chunk) {
            for (ChunkInfo<?> el : this.chunks) {
                if (squaredEuclideanDistance(el, chunk) <= DISTANCE_THRESHOLD) {
                    return true;
                }
            }
            return false;
        }

        public void add(ChunkInfo<?> chunk) {
            this.chunks.add(chunk);
            this.totalEntities.addAndGet(chunk.getEntityCounts().total().get());
        }

        public void merge(Region group) {
            this.chunks.addAll(group.getChunks());
            this.totalEntities.addAndGet(group.getTotalEntities().get());
        }

        private static long squaredEuclideanDistance(ChunkInfo<?> a, ChunkInfo<?> b) {
            long dx = a.getX() - b.getX();
            long dz = a.getZ() - b.getZ();
            return (dx * dx) + (dz * dz);
        }
    }

}

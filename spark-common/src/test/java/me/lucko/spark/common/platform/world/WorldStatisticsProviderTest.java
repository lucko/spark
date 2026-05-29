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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WorldStatisticsProviderTest {

    @Test
    public void testGroupIntoRegionsEmpty() {
        List<WorldStatisticsProvider.Region> regions = WorldStatisticsProvider.groupIntoRegions(ImmutableList.of());
        assertEquals(0, regions.size());
    }

    @Test
    public void testGroupIntoRegionsSingle() {
        TestChunkInfo chunk1 = new TestChunkInfo(0, 0);
        List<WorldStatisticsProvider.Region> regions = WorldStatisticsProvider.groupIntoRegions(ImmutableList.of(chunk1));

        assertEquals(1, regions.size());
        WorldStatisticsProvider.Region region = regions.get(0);

        Set<ChunkInfo<?>> chunks = region.getChunks();
        assertEquals(1, chunks.size());
        assertEquals(ImmutableSet.of(chunk1), chunks);
    }

    @Test
    public void testGroupIntoRegionsMultiple() {
        TestChunkInfo chunk1 = new TestChunkInfo(0, 0);
        TestChunkInfo chunk2 = new TestChunkInfo(0, 1);
        TestChunkInfo chunk3 = new TestChunkInfo(1, 0);
        TestChunkInfo chunk4 = new TestChunkInfo(0, 2);

        List<WorldStatisticsProvider.Region> regions = WorldStatisticsProvider.groupIntoRegions(ImmutableList.of(chunk1, chunk2, chunk3, chunk4));

        assertEquals(1, regions.size());

        WorldStatisticsProvider.Region region = regions.get(0);
        Set<ChunkInfo<?>> chunks = region.getChunks();
        assertEquals(4, chunks.size());
        assertEquals(ImmutableSet.of(chunk1, chunk2, chunk3, chunk4), chunks);
    }

    @Test
    public void testGroupIntoRegionsMultipleRegions() {
        TestChunkInfo chunk1 = new TestChunkInfo(0, 0);
        TestChunkInfo chunk2 = new TestChunkInfo(0, 1);
        TestChunkInfo chunk3 = new TestChunkInfo(1, 0);
        TestChunkInfo chunk4 = new TestChunkInfo(2, 2);

        List<WorldStatisticsProvider.Region> regions = WorldStatisticsProvider.groupIntoRegions(ImmutableList.of(chunk1, chunk2, chunk3, chunk4));

        assertEquals(2, regions.size());

        WorldStatisticsProvider.Region region1 = regions.get(0);
        Set<ChunkInfo<?>> chunks1 = region1.getChunks();
        assertEquals(3, chunks1.size());
        assertEquals(ImmutableSet.of(chunk1, chunk2, chunk3), chunks1);

        WorldStatisticsProvider.Region region2 = regions.get(1);
        Set<ChunkInfo<?>> chunks2 = region2.getChunks();
        assertEquals(1, chunks2.size());
        assertEquals(ImmutableSet.of(chunk4), chunks2);
    }

    private static final class TestChunkInfo implements ChunkInfo<String> {
        private final int x;
        private final int z;
        private final CountMap<String> entityCounts;

        public TestChunkInfo(int x, int z) {
            this.x = x;
            this.z = z;
            this.entityCounts = new CountMap.Simple<>(new HashMap<>());
            this.entityCounts.increment("test");
        }

        @Override
        public int getX() {
            return this.x;
        }

        @Override
        public int getZ() {
            return this.z;
        }

        @Override
        public CountMap<String> getEntityCounts() {
            return this.entityCounts;
        }

        @Override
        public String entityTypeName(String type) {
            return type;
        }
    }

}

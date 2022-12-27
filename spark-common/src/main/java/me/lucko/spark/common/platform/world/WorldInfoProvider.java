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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides information about worlds.
 */
public interface WorldInfoProvider {

    WorldInfoProvider NO_OP = new WorldInfoProvider() {
        @Override
        public CountsResult pollCounts() {
            return null;
        }

        @Override
        public ChunksResult<? extends ChunkInfo<?>> pollChunks() {
            return null;
        }
    };

    /**
     * Polls for counts.
     *
     * @return the counts
     */
    CountsResult pollCounts();

    /**
     * Polls for chunk information.
     *
     * @return the chunk information
     */
    ChunksResult<? extends ChunkInfo<?>> pollChunks();

    default boolean mustCallSync() {
        return true;
    }

    final class ChunksResult<T extends ChunkInfo<?>> {
        private final Map<String, List<T>> worlds = new HashMap<>();

        public void put(String worldName, List<T> chunks) {
            this.worlds.put(worldName, chunks);
        }

        public Map<String, List<T>> getWorlds() {
            return this.worlds;
        }
    }

    final class CountsResult {
        private final int players;
        private final int entities;
        private final int tileEntities;
        private final int chunks;

        public CountsResult(int players, int entities, int tileEntities, int chunks) {
            this.players = players;
            this.entities = entities;
            this.tileEntities = tileEntities;
            this.chunks = chunks;
        }

        public int players() {
            return this.players;
        }

        public int entities() {
            return this.entities;
        }

        public int tileEntities() {
            return this.tileEntities;
        }

        public int chunks() {
            return this.chunks;
        }
    }

}

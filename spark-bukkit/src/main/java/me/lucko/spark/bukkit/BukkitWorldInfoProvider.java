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

package me.lucko.spark.bukkit;

import me.lucko.spark.bukkit.common.AbstractWorldInfoProvider;
import me.lucko.spark.common.platform.world.WorldInfoProvider;

import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.BlockState;

public class BukkitWorldInfoProvider extends AbstractWorldInfoProvider implements WorldInfoProvider {
    private static final boolean SUPPORTS_PAPER_COUNT_METHODS;

    static {
        boolean supportsPaperCountMethods = false;
        try {
            World.class.getMethod("getEntityCount");
            World.class.getMethod("getTileEntityCount");
            World.class.getMethod("getChunkCount");
            supportsPaperCountMethods = true;
        } catch (Exception e) {
            // ignored
        }
        SUPPORTS_PAPER_COUNT_METHODS = supportsPaperCountMethods;
    }

    public BukkitWorldInfoProvider(Server server) {
        super(server);
    }

    @Override
    public CountsResult pollCounts() {
        int players = this.server.getOnlinePlayers().size();
        int entities = 0;
        int tileEntities = 0;
        int chunks = 0;

        for (World world : this.server.getWorlds()) {
            if (SUPPORTS_PAPER_COUNT_METHODS) {
                entities += world.getEntityCount();
                tileEntities += world.getTileEntityCount();
                chunks += world.getChunkCount();
            } else {
                entities += world.getEntities().size();

                Chunk[] chunksArray = world.getLoadedChunks();
                int nullChunks = 0;

                for (Chunk chunk : chunksArray) {
                    if (chunk == null) {
                        ++nullChunks;
                        continue;
                    }

                    BlockState[] tileEntitiesArray = chunk.getTileEntities();
                    tileEntities += tileEntitiesArray != null ? tileEntitiesArray.length : 0;
                }

                chunks += chunksArray.length - nullChunks;
            }
        }

        return new CountsResult(players, entities, tileEntities, chunks);
    }
}

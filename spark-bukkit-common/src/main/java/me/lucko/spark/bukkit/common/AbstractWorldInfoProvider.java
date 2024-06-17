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

package me.lucko.spark.bukkit.common;

import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;

import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractWorldInfoProvider implements WorldInfoProvider {

    protected final Server server;

    public AbstractWorldInfoProvider(Server server) {
        this.server = server;
    }

    @Override
    public ChunksResult<BukkitChunkInfo> pollChunks() {
        ChunksResult<BukkitChunkInfo> data = new ChunksResult<>();

        for (World world : this.server.getWorlds()) {
          Chunk[] chunks = world.getLoadedChunks();

          List<BukkitChunkInfo> list = new ArrayList<>(chunks.length);
          for (Chunk chunk : chunks) {
              if (chunk != null) {
               list.add(new BukkitChunkInfo(chunk));
              }
          }

          data.put(world.getName(), list);
        }

        return data;
    }

    static final class BukkitChunkInfo extends AbstractChunkInfo<EntityType> {
        private final CountMap<EntityType> entityCounts;

        BukkitChunkInfo(Chunk chunk) {
            super(chunk.getX(), chunk.getZ());

            this.entityCounts = new CountMap.EnumKeyed<>(EntityType.class);
            for (Entity entity : chunk.getEntities()) {
                if (entity != null) {
                    this.entityCounts.increment(entity.getType());
                }
            }
        }

      @Override
      public CountMap<EntityType> getEntityCounts() {
          return this.entityCounts;
      }

      @SuppressWarnings("deprecation")
      @Override
      public String entityTypeName(EntityType type) {
          return type.getName();
      }
  }
}

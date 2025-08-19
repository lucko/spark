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

package me.lucko.spark.paper;

import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Chunk;
import org.bukkit.GameRule;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class PaperWorldInfoProvider implements WorldInfoProvider {
    private final Server server;

    public PaperWorldInfoProvider(Server server) {
        this.server = server;
    }

    @Override
    public CountsResult pollCounts() {
        int players = this.server.getOnlinePlayers().size();
        int entities = 0;
        int tileEntities = 0;
        int chunks = 0;

        for (World world : this.server.getWorlds()) {
            entities += world.getEntityCount();
            tileEntities += world.getTileEntityCount();
            chunks += world.getChunkCount();
        }

        return new CountsResult(players, entities, tileEntities, chunks);
    }

    @Override
    public ChunksResult<PaperChunkInfo> pollChunks() {
        ChunksResult<PaperChunkInfo> data = new ChunksResult<>();

        for (World world : this.server.getWorlds()) {
            Chunk[] chunks = world.getLoadedChunks();

            List<PaperChunkInfo> list = new ArrayList<>(chunks.length);
            for (Chunk chunk : chunks) {
                if (chunk != null) {
                    list.add(new PaperChunkInfo(chunk));
                }
            }

            data.put(world.getName(), list);
        }

        return data;
    }

    @Override
    public GameRulesResult pollGameRules() {
        GameRulesResult data = new GameRulesResult();

        boolean addDefaults = true; // add defaults in the first iteration
        for (World world : this.server.getWorlds()) {
            for (String gameRule : world.getGameRules()) {
                GameRule<?> ruleObj = GameRule.getByName(gameRule);
                if (ruleObj == null) {
                    continue;
                }

                if (addDefaults) {
                    Object defaultValue = world.getGameRuleDefault(ruleObj);
                    data.putDefault(gameRule, Objects.toString(defaultValue));
                }

                Object value = world.getGameRuleValue(ruleObj);
                data.put(gameRule, world.getName(), Objects.toString(value));
            }

            addDefaults = false;
        }

        return data;
    }

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        this.server.getDatapackManager().refreshPacks();
        return this.server.getDatapackManager().getPacks().stream()
                .map(pack -> new DataPackInfo(
                        PlainTextComponentSerializer.plainText().serialize(pack.getTitle()),
                        PlainTextComponentSerializer.plainText().serialize(pack.getDescription()),
                        pack.getSource().toString().toLowerCase(Locale.ROOT).replace("_", "")
                ))
                .collect(Collectors.toList());
    }

    static final class PaperChunkInfo extends AbstractChunkInfo<EntityType> {
        private final CountMap<EntityType> entityCounts;

        PaperChunkInfo(Chunk chunk) {
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

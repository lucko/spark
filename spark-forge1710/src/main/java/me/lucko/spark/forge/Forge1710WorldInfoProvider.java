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

package me.lucko.spark.forge;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;

import java.util.*;
import java.util.stream.Collectors;

public abstract class Forge1710WorldInfoProvider implements WorldInfoProvider {
    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return Collections.emptyList();
    }

    protected void encodeGameRules(GameRulesResult result, GameRules rules, String worldName) {
        for (String rule : rules.getRules()) {
            if (rule == null) {
                continue;
            }
            String value = rules.getGameRuleStringValue(rule);
            if (value != null) {
                result.put(rule, worldName, value);
            }
        }

    }

    protected void setDefaultGameRules(GameRulesResult result) {
        GameRules vanillaRules = new GameRules();

        result.getRules().entrySet().stream().filter(entry -> entry.getValue().getDefaultValue() == null).map(Map.Entry::getKey).collect(Collectors.toList()).forEach(rule -> {
            String def = vanillaRules.getGameRuleStringValue(rule);
            result.putDefault(rule, def != null ? def : "");
        });
    }

    public static final class Server extends Forge1710WorldInfoProvider {
        private final MinecraftServer server;

        public Server(MinecraftServer server) {
            this.server = server;
        }

        @Override
        public ChunksResult<ForgeChunkInfo> pollChunks() {
            ChunksResult<ForgeChunkInfo> data = new ChunksResult<>();

            for (WorldServer level : this.server.worldServers) {
                ArrayList<ForgeChunkInfo> list = new ArrayList<>();
                for(Chunk chunk : (List<Chunk>)level.theChunkProviderServer.loadedChunks) {
                    list.add(new ForgeChunkInfo(chunk));
                }
                data.put(level.provider.getDimensionName(), list);
            }

            return data;
        }

        @Override
        public CountsResult pollCounts() {
            int players = this.server.getCurrentPlayerCount();
            int entities = 0;
            int chunks = 0;

            for (WorldServer level : this.server.worldServers) {
                entities += level.loadedEntityList.size();
                chunks += level.getChunkProvider().getLoadedChunkCount();
            }

            return new CountsResult(players, entities, -1, chunks);
        }

        @Override
        public GameRulesResult pollGameRules() {
            GameRulesResult data = new GameRulesResult();

            for (WorldServer world : server.worldServers) {
                encodeGameRules(data, world.getGameRules(), world.provider.getDimensionName());
            }

            setDefaultGameRules(data);

            return data;
        }
    }

    @SideOnly(Side.CLIENT)
    public static final class Client extends Forge1710WorldInfoProvider {
        private final Minecraft client;

        public Client(Minecraft client) {
            this.client = client;
        }

        @Override
        public ChunksResult<ForgeChunkInfo> pollChunks() {
            ChunksResult<ForgeChunkInfo> data = new ChunksResult<>();

            WorldClient level = this.client.theWorld;
            if (level == null) {
                return null;
            }

            ArrayList<ForgeChunkInfo> list = new ArrayList<>();
            IChunkProvider provider = level.getChunkProvider();
            if(provider instanceof ChunkProviderClient) {
                List<Chunk> chunks = ReflectionHelper.getPrivateValue(ChunkProviderClient.class, (ChunkProviderClient)provider, "chunkListing", "field_73237_c");
                for(Chunk chunk : chunks) {
                    list.add(new ForgeChunkInfo(chunk));
                }
            }

            data.put(level.provider.getDimensionName(), list);

            return data;
        }

        @Override
        public CountsResult pollCounts() {
            WorldClient level = this.client.theWorld;
            if (level == null) {
                return null;
            }

            return new CountsResult(-1, level.loadedEntityList.size(), -1, level.getChunkProvider().getLoadedChunkCount());
        }

        @Override
        public GameRulesResult pollGameRules() {
            WorldClient world = Minecraft.getMinecraft().theWorld;

            GameRulesResult data = new GameRulesResult();

            encodeGameRules(data, world.getGameRules(), world.provider.getDimensionName());

            setDefaultGameRules(data);

            return data;
        }
    }

    static final class ForgeChunkInfo extends AbstractChunkInfo<Class<? extends Entity>> {
        private final CountMap<Class<? extends Entity>> entityCounts;

        ForgeChunkInfo(Chunk chunk) {
            super(chunk.xPosition, chunk.zPosition);

            this.entityCounts = new CountMap.Simple<>(new HashMap<>());
            for(List<Entity> entityList : chunk.entityLists) {
                entityList.forEach(entity -> {
                    this.entityCounts.increment(entity.getClass());
                });
            }
        }

        @Override
        public CountMap<Class<? extends Entity>> getEntityCounts() {
            return this.entityCounts;
        }

        @Override
        public String entityTypeName(Class<? extends Entity> type) {
            return (String)EntityList.classToStringMapping.get(type);
        }
    }


}

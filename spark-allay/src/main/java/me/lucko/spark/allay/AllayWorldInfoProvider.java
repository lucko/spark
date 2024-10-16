package me.lucko.spark.allay;

import me.lucko.spark.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.common.platform.world.CountMap;
import me.lucko.spark.common.platform.world.WorldInfoProvider;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.Identifier;
import org.allaymc.api.world.DimensionInfo;
import org.allaymc.api.world.World;
import org.allaymc.api.world.chunk.Chunk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

/**
 * @author IWareQ
 */
public class AllayWorldInfoProvider implements WorldInfoProvider {

    @Override
    public CountsResult pollCounts() {
        var server = Server.getInstance();
        int players = server.getOnlinePlayers().size();
        int entities = 0;
        var blockEntities = 0;
        int chunks = 0;

        for (var world : server.getWorldPool().getWorlds().values()) {
            for (var dimension : world.getDimensions().values()) {
                entities += dimension.getEntities().size();
                blockEntities += dimension.getChunkService().getLoadedChunks()
                        .stream()
                        .mapToInt(chunk -> chunk.getBlockEntities().size())
                        .sum();
                chunks += dimension.getChunkService().getLoadedChunks().size();
            }
        }

        return new CountsResult(players, entities, blockEntities, chunks);
    }

    @Override
    public ChunksResult<AllayChunkInfo> pollChunks() {
        ChunksResult<AllayChunkInfo> result = new ChunksResult<>();

        for (var world : Server.getInstance().getWorldPool().getWorlds().values()) {
            for (var dimension : world.getDimensions().values()) {
                var chunks = dimension.getChunkService().getLoadedChunks();
                var chunkInfos = chunks.stream().map(AllayChunkInfo::new).toList();

                result.put(world.getWorldData().getName() + "_" + getDimensionName(dimension.getDimensionInfo()), chunkInfos);
            }
        }

        return result;
    }

    @Override
    public GameRulesResult pollGameRules() {
        GameRulesResult data = new GameRulesResult();
        for (World world : Server.getInstance().getWorldPool().getWorlds().values()) {
            for (var gameRuleEntry : world.getWorldData().getGameRules().getGameRules().entrySet()) {
                Object value = gameRuleEntry.getValue();
                data.put(gameRuleEntry.getKey().getName(), world.getWorldData().getName(), Objects.toString(value));
            }
        }

        return data;
    }

    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return null;
    }

    private String getDimensionName(DimensionInfo dimensionInfo) {
        return switch (dimensionInfo.dimensionId()) {
            case 0 -> "overworld";
            case 1 -> "the_nether";
            case 2 -> "the_end";
            default -> dimensionInfo.dimensionId() + "";
        };
    }

    public static class AllayChunkInfo extends AbstractChunkInfo<Identifier> {
        private final CountMap<Identifier> entityCounts = new CountMap.Simple<>(new HashMap<>());

        protected AllayChunkInfo(Chunk chunk) {
            super(chunk.getX(), chunk.getZ());
            chunk.getEntities().values().forEach(entity -> this.entityCounts.increment(entity.getEntityType().getIdentifier()));
        }

        @Override
        public CountMap<Identifier> getEntityCounts() {
            return this.entityCounts;
        }

        @Override
        public String entityTypeName(Identifier identifier) {
            return identifier.toString();
        }
    }
}

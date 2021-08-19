package me.lucko.spark.common.sampler.grouper;

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.proto.SparkProtos;

/**
 * Implementation of {@link ThreadGrouper} which groups all threads as one, under
 * the name "All".
 */
public class SingleThreadGrouper implements ThreadGrouper {

    @Override
    public String getGroup(long threadId, String threadName) {
        return "All";
    }

    @Override
    public SparkProtos.SamplerMetadata.DataAggregator.ThreadGrouper asProto() {
        return SparkProtos.SamplerMetadata.DataAggregator.ThreadGrouper.AS_ONE;
    }
}

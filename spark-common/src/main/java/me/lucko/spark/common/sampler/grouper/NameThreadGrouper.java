package me.lucko.spark.common.sampler.grouper;

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.proto.SparkProtos;

/**
 * Implementation of {@link ThreadGrouper} that just groups by thread name.
 */
public class NameThreadGrouper implements ThreadGrouper {
    @Override
    public String getGroup(long threadId, String threadName) {
        return threadName;
    }

    @Override
    public SparkProtos.SamplerMetadata.DataAggregator.ThreadGrouper asProto() {
        return SparkProtos.SamplerMetadata.DataAggregator.ThreadGrouper.BY_NAME;
    }
}

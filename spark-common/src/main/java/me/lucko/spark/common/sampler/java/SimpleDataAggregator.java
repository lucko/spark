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

package me.lucko.spark.common.sampler.java;

import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.aggregator.DataAggregator;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

import java.lang.management.ThreadInfo;
import java.util.concurrent.ExecutorService;

/**
 * Basic implementation of {@link DataAggregator}.
 */
public class SimpleDataAggregator extends JavaDataAggregator {
    public SimpleDataAggregator(ExecutorService workerPool, ThreadGrouper threadGrouper, int interval, boolean ignoreSleeping, boolean ignoreNative) {
        super(workerPool, threadGrouper, interval, ignoreSleeping, ignoreNative);
    }

    @Override
    public SamplerMetadata.DataAggregator getMetadata() {
        return SamplerMetadata.DataAggregator.newBuilder()
                .setType(SamplerMetadata.DataAggregator.Type.SIMPLE)
                .setThreadGrouper(this.threadGrouper.asProto())
                .build();
    }

    @Override
    public void insertData(ThreadInfo threadInfo, int window) {
        writeData(threadInfo, window);
    }

}

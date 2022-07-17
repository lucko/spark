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

package me.lucko.spark.api.profiler.dumper;

import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

/**
 * Uses the {@link ThreadMXBean} to generate {@link ThreadInfo} instances for the threads being
 * sampled.
 */
public interface ThreadDumper {

    /**
     * Generates {@link ThreadInfo} data for the sampled threads.
     *
     * @param threadBean the thread bean instance to obtain the data from
     * @return an array of generated thread info instances
     */
    ThreadInfo[] dumpThreads(ThreadMXBean threadBean);

    /**
     * Gets metadata about the thread dumper instance.
     */
    SamplerMetadata.ThreadDumper getMetadata();

    /**
     * Implementation of {@link ThreadDumper} that generates data for all threads.
     */
    ThreadDumper ALL = new ThreadDumper() {
        @Override
        public ThreadInfo[] dumpThreads(final ThreadMXBean threadBean) {
            return threadBean.dumpAllThreads(false, false);
        }

        @Override
        public SamplerMetadata.ThreadDumper getMetadata() {
            return SamplerMetadata.ThreadDumper.newBuilder()
                    .setType(SamplerMetadata.ThreadDumper.Type.ALL)
                    .build();
        }
    };

}

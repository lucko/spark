/*
 * This file is part of spark, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
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

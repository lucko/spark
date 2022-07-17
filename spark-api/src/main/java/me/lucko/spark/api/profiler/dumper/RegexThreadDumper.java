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

import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.util.StreamSupplier;
import me.lucko.spark.proto.SparkSamplerProtos;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ThreadDumper} that generates data for a regex matched set of threads.
 */
public final class RegexThreadDumper implements ThreadDumper {
    private final StreamSupplier<Thread> finder = SparkProvider.get().threadFinder();
    private final Set<Pattern> namePatterns;
    private final Map<Long, Boolean> cache = new HashMap<>();

    public RegexThreadDumper(Set<String> namePatterns) {
        this.namePatterns = namePatterns.stream()
                .map(regex -> {
                    try {
                        return Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    } catch (PatternSyntaxException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public ThreadInfo[] dumpThreads(ThreadMXBean threadBean) {
        return finder.get()
                .filter(thread -> {
                    Boolean result = this.cache.get(thread.getId());
                    if (result != null) {
                        return result;
                    }

                    for (Pattern pattern : this.namePatterns) {
                        if (pattern.matcher(thread.getName()).matches()) {
                            this.cache.put(thread.getId(), true);
                            return true;
                        }
                    }
                    this.cache.put(thread.getId(), false);
                    return false;
                })
                .map(thread -> threadBean.getThreadInfo(thread.getId(), Integer.MAX_VALUE))
                .filter(Objects::nonNull)
                .toArray(ThreadInfo[]::new);
    }

    @Override
    public SparkSamplerProtos.SamplerMetadata.ThreadDumper getMetadata() {
        return SparkSamplerProtos.SamplerMetadata.ThreadDumper.newBuilder()
                .setType(SparkSamplerProtos.SamplerMetadata.ThreadDumper.Type.REGEX)
                .addAllPatterns(this.namePatterns.stream().map(Pattern::pattern).collect(Collectors.toList()))
                .build();
    }
}

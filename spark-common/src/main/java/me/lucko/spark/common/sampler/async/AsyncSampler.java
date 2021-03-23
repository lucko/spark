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

package me.lucko.spark.common.sampler.async;

import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.platform.PlatformInfo;
import me.lucko.spark.common.sampler.Sampler;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.async.jfr.JfrReader;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.proto.SparkProtos;

import one.profiler.AsyncProfiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 * A sampler implementation using async-profiler.
 */
public class AsyncSampler implements Sampler {
    private final AsyncProfiler profiler;

    /** The instance used to generate thread information for use in sampling */
    private final ThreadDumper threadDumper;
    /** Responsible for aggregating and then outputting collected sampling data */
    private final AsyncDataAggregator dataAggregator;
    /** Flag to mark if the output has been completed */
    private boolean outputComplete = false;
    /** The temporary output file */
    private Path outputFile;

    /** The interval to wait between sampling, in microseconds */
    private final int interval;
    /** The time when sampling first began */
    private long startTime = -1;

    public AsyncSampler(int interval, ThreadDumper threadDumper, ThreadGrouper threadGrouper) {
        this.profiler = AsyncProfilerAccess.INSTANCE.getProfiler();
        this.threadDumper = threadDumper;
        this.dataAggregator = new AsyncDataAggregator(threadGrouper);
        this.interval = interval;
    }

    @Override
    public long getStartTime() {
        if (this.startTime == -1) {
            throw new IllegalStateException("Not yet started");
        }
        return this.startTime;
    }

    @Override
    public long getEndTime() {
        return -1;
    }

    @Override
    public CompletableFuture<? extends Sampler> getFuture() {
        return new CompletableFuture<>();
    }

    /**
     * Executes a profiler command.
     *
     * @param command the command to execute
     * @return the response
     */
    private String execute(String command) {
        try {
            return this.profiler.execute(command);
        } catch (IOException e) {
            throw new RuntimeException("Exception whilst executing profiler command", e);
        }
    }

    /**
     * Starts the profiler.
     */
    @Override
    public void start() {
        this.startTime = System.currentTimeMillis();

        try {
            this.outputFile = Files.createTempFile("spark-profile-", ".jfr.tmp");
            this.outputFile.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create temporary output file", e);
        }

        String command = "start,event=cpu,interval=" + this.interval + "us,threads,jfr,file=" + this.outputFile.toString();
        if (this.threadDumper instanceof ThreadDumper.Specific) {
            command += ",filter";
        }

        String resp = execute(command).trim();
        if (!resp.equalsIgnoreCase("profiling started")) {
            throw new RuntimeException("Unexpected response: " + resp);
        }

        if (this.threadDumper instanceof ThreadDumper.Specific) {
            ThreadDumper.Specific threadDumper = (ThreadDumper.Specific) this.threadDumper;
            for (Thread thread : threadDumper.getThreads()) {
                this.profiler.addThread(thread);
            }
        }
    }

    /**
     * Stops the profiler.
     */
    @Override
    public void stop() {
        this.profiler.stop();
    }

    @Override
    public SparkProtos.SamplerData toProto(PlatformInfo platformInfo, CommandSender creator, Comparator<? super Map.Entry<String, ThreadNode>> outputOrder, String comment, MergeMode mergeMode) {
        final SparkProtos.SamplerMetadata.Builder metadata = SparkProtos.SamplerMetadata.newBuilder()
                .setPlatform(platformInfo.toData().toProto())
                .setUser(creator.toData().toProto())
                .setStartTime(this.startTime)
                .setInterval(this.interval)
                .setThreadDumper(this.threadDumper.getMetadata())
                .setDataAggregator(this.dataAggregator.getMetadata());

        if (comment != null) {
            metadata.setComment(comment);
        }

        SparkProtos.SamplerData.Builder proto = SparkProtos.SamplerData.newBuilder();
        proto.setMetadata(metadata.build());

        aggregateOutput();

        List<Map.Entry<String, ThreadNode>> data = new ArrayList<>(this.dataAggregator.getData().entrySet());
        data.sort(outputOrder);

        for (Map.Entry<String, ThreadNode> entry : data) {
            proto.addThreads(entry.getValue().toProto(mergeMode));
        }

        return proto.build();
    }

    private void aggregateOutput() {
        if (this.outputComplete) {
            return;
        }
        this.outputComplete = true;

        Predicate<String> threadFilter;
        if (this.threadDumper instanceof ThreadDumper.Specific) {
            ThreadDumper.Specific threadDumper = (ThreadDumper.Specific) this.threadDumper;
            threadFilter = n -> threadDumper.getThreadNames().contains(n.toLowerCase());
        } else {
            threadFilter = n -> true;
        }

        // read the jfr file produced by async-profiler
        try (JfrReader reader = new JfrReader(this.outputFile)) {
            readSegments(reader, threadFilter);
        } catch (IOException e) {
            throw new RuntimeException("Read error", e);
        }

        // delete the output file after reading
        try {
            Files.deleteIfExists(this.outputFile);
        } catch (IOException e) {
            // ignore
        }
    }

    private void readSegments(JfrReader reader, Predicate<String> threadFilter) {
        List<JfrReader.Sample> samples = reader.samples;
        for (int i = 0; i < samples.size(); i++) {
            JfrReader.Sample sample = samples.get(i);

            long duration;
            if (i == 0) {
                // we don't really know the duration of the first sample, so just use the sampling
                // interval
                duration = this.interval;
            } else {
                // calculate the duration of the sample by calculating the time elapsed since the
                // previous sample
                duration = TimeUnit.NANOSECONDS.toMicros(sample.time - samples.get(i - 1).time);
            }

            String threadName = reader.threads.get(sample.tid);
            if (!threadFilter.test(threadName)) {
                continue;
            }

            // parse the segment and give it to the data aggregator
            ProfileSegment segment = parseSegment(reader, sample, threadName, duration);
            this.dataAggregator.insertData(segment);
        }
    }

    private static ProfileSegment parseSegment(JfrReader reader, JfrReader.Sample sample, String threadName, long duration) {
        JfrReader.StackTrace stackTrace = reader.stackTraces.get(sample.stackTraceId);
        int len = stackTrace.methods.length;

        AsyncStackTraceElement[] stack = new AsyncStackTraceElement[len];
        for (int i = 0; i < len; i++) {
            stack[i] = parseStackFrame(reader, stackTrace.methods[i]);
        }

        return new ProfileSegment(sample.tid, threadName, stack, duration);
    }

    private static AsyncStackTraceElement parseStackFrame(JfrReader reader, long methodId) {
        AsyncStackTraceElement result = reader.stackFrames.get(methodId);
        if (result != null) {
            return result;
        }

        JfrReader.MethodRef methodRef = reader.methods.get(methodId);
        JfrReader.ClassRef classRef = reader.classes.get(methodRef.cls);

        byte[] className = reader.symbols.get(classRef.name);
        byte[] methodName = reader.symbols.get(methodRef.name);

        if (className == null || className.length == 0) {
            // native call
            result = new AsyncStackTraceElement(
                    "native",
                    new String(methodName, StandardCharsets.UTF_8),
                    null
            );
        } else {
            // java method
            byte[] methodDesc = reader.symbols.get(methodRef.sig);
            result = new AsyncStackTraceElement(
                    new String(className, StandardCharsets.UTF_8).replace('/', '.'),
                    new String(methodName, StandardCharsets.UTF_8),
                    new String(methodDesc, StandardCharsets.UTF_8)
            );
        }

        reader.stackFrames.put(methodId, result);
        return result;
    }
}

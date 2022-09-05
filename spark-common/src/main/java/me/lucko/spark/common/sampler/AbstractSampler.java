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

package me.lucko.spark.common.sampler;

import me.lucko.spark.api.profiler.Profiler;
import me.lucko.spark.api.profiler.dumper.ThreadDumper;
import me.lucko.spark.api.profiler.report.ProfilerReport;
import me.lucko.spark.api.profiler.report.ReportConfiguration;
import me.lucko.spark.api.util.Sender;
import me.lucko.spark.api.util.UploadResult;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.modules.SamplerModule;
import me.lucko.spark.common.monitor.memory.GarbageCollectorStatistics;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.sampler.aggregator.DataAggregator;
import me.lucko.spark.common.sampler.async.AsyncSampler;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.util.ClassSourceLookup;
import me.lucko.spark.common.util.MethodDisambiguator;
import me.lucko.spark.proto.SparkSamplerProtos;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Base implementation class for {@link Sampler}s.
 */
public abstract class AbstractSampler implements Sampler {

    /** The manager associated with this sampler */
    protected final SamplerManager manager;

    /** The spark platform instance */
    protected final SparkPlatform platform;

    /** The interval to wait between sampling, in microseconds */
    protected final int interval;

    /** The instance used to generate thread information for use in sampling */
    protected final ThreadDumper threadDumper;

    /** The time when sampling first began */
    protected long startTime = -1;

    /** The game tick when sampling first began */
    protected int startTick = -1;

    /** The unix timestamp (in millis) when this sampler should automatically complete. */
    protected final long autoEndTime; // -1 for nothing

    /** A future to encapsulate the completion of this sampler instance */
    protected final CompletableFuture<Profiler.Sampler> future = new CompletableFuture<>();

    /** The garbage collector statistics when profiling started */
    protected Map<String, GarbageCollectorStatistics> initialGcStats;

    protected AbstractSampler(SamplerManager manager, SparkPlatform platform, int interval, ThreadDumper threadDumper, long autoEndTime) {
        this.manager = manager;
        this.platform = platform;
        this.interval = interval;
        this.threadDumper = threadDumper;
        this.autoEndTime = autoEndTime;
    }

    @Override
    public long getStartTime() {
        if (this.startTime == -1) {
            throw new IllegalStateException("Not yet started");
        }
        return this.startTime;
    }

    @Override
    public long getAutoEndTime() {
        return this.autoEndTime;
    }

    protected void recordInitialGcStats() {
        this.initialGcStats = GarbageCollectorStatistics.pollStats();
    }

    protected Map<String, GarbageCollectorStatistics> getInitialGcStats() {
        return this.initialGcStats;
    }

    @Override
    public void stop() {
        manager.markStopped(this);
    }

    @Override
    public void start() {
        manager.markStarted(this);
        this.startTime = System.currentTimeMillis();

        TickHook tickHook = this.platform.getTickHook();
        if (tickHook != null) {
            this.startTick = tickHook.getCurrentTick();
        }
    }

    @Override
    public ProfilerReport dumpReport(ReportConfiguration configuration) {
        return createReport(configuration);
    }

    private ProfilerReport createReport(ReportConfiguration configuration) {
        final MethodDisambiguator methodDisambiguator = new MethodDisambiguator();
        return new ProfilerReport() {
            final SparkSamplerProtos.SamplerData data = toProto(platform, configuration.getSender(), configuration.getThreadOrder()::compare, configuration.getComment(), configuration.separateParentCalls() ? MergeMode.separateParentCalls(methodDisambiguator) : MergeMode.sameMethod(methodDisambiguator), platform.createClassSourceLookup());

            UploadResult uploadResult;

            @Override
            @NonNull
            public UploadResult upload() throws IOException {
                if (uploadResult == null)
                    uploadResult = SamplerModule.postData(platform, data);
                return uploadResult;
            }

            @Override
            @NotNull
            public SparkSamplerProtos.SamplerData data() {
                return data;
            }

            @Override
            @NotNull
            public Path saveToFile(Path path) throws IOException {
                if (path.getParent() != null)
                    Files.createDirectories(path.getParent());
                Files.deleteIfExists(path);
                return Files.write(path, data.toByteArray());
            }
        };
    }

    @Override
    public CompletableFuture<ProfilerReport> onCompleted(ReportConfiguration configuration) {
        return onCompleted().thenApply(samp -> createReport(configuration));
    }

    @NonNull
    @Override
    public CompletableFuture<Profiler.Sampler> onCompleted() {
        return future;
    }

    protected void writeMetadataToProto(SamplerData.Builder proto, SparkPlatform platform, @org.jetbrains.annotations.Nullable Sender creator, @Nullable String comment, DataAggregator dataAggregator) {
        SamplerMetadata.Builder metadata = SamplerMetadata.newBuilder()
                .setPlatformMetadata(platform.getPlugin().getPlatformInfo().toData().toProto())
                .setStartTime(this.startTime)
                .setEndTime(System.currentTimeMillis())
                .setInterval(this.interval)
                .setThreadDumper(this.threadDumper.getMetadata())
                .setDataAggregator(dataAggregator.getMetadata());

        if (creator != null) {
            metadata.setCreator(creator.toProto());
        }

        if (comment != null) {
            metadata.setComment(comment);
        }

        if (this.startTick != -1) {
            TickHook tickHook = this.platform.getTickHook();
            if (tickHook != null) {
                int numberOfTicks = tickHook.getCurrentTick() - this.startTick;
                metadata.setNumberOfTicks(numberOfTicks);
            }
        }

        try {
            metadata.setPlatformStatistics(platform.getStatisticsProvider().getPlatformStatistics(getInitialGcStats()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            metadata.setSystemStatistics(platform.getStatisticsProvider().getSystemStatistics());
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ServerConfigProvider serverConfigProvider = platform.getPlugin().createServerConfigProvider();
            metadata.putAllServerConfigurations(serverConfigProvider.exportServerConfigurations());
        } catch (Exception e) {
            e.printStackTrace();
        }

        proto.setMetadata(metadata);
    }

    protected void writeDataToProto(SamplerData.Builder proto, DataAggregator dataAggregator, Comparator<ThreadNode> outputOrder, MergeMode mergeMode, ClassSourceLookup classSourceLookup) {
        List<ThreadNode> data = dataAggregator.exportData();
        data.sort(outputOrder);

        ClassSourceLookup.Visitor classSourceVisitor = ClassSourceLookup.createVisitor(classSourceLookup);

        for (ThreadNode entry : data) {
            proto.addThreads(entry.toProto(mergeMode));
            classSourceVisitor.visit(entry);
        }

        if (classSourceVisitor.hasMappings()) {
            proto.putAllClassSources(classSourceVisitor.getMapping());
        }
    }

    @Override
    public boolean isAsync() {
        return this instanceof AsyncSampler;
    }
}

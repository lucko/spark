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

import me.lucko.spark.api.profiler.dumper.ThreadDumper;
import me.lucko.spark.api.profiler.report.ProfilerReport;
import me.lucko.spark.api.profiler.report.ReportConfiguration;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.modules.SamplerModule;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.memory.GarbageCollectorStatistics;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.sampler.aggregator.DataAggregator;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.tick.TickHook;
import me.lucko.spark.common.util.ClassSourceLookup;
import me.lucko.spark.common.util.MethodDisambiguator;
import me.lucko.spark.proto.SparkSamplerProtos;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Base implementation class for {@link Sampler}s.
 */
public abstract class AbstractSampler implements Sampler {

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
    protected final CompletableFuture<Sampler> future = new CompletableFuture<>();

    /** The garbage collector statistics when profiling started */
    protected Map<String, GarbageCollectorStatistics> initialGcStats;

    protected AbstractSampler(SparkPlatform platform, int interval, ThreadDumper threadDumper, long autoEndTime) {
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

    @Override
    public CompletableFuture<Sampler> getFuture() {
        return this.future;
    }

    protected void recordInitialGcStats() {
        this.initialGcStats = GarbageCollectorStatistics.pollStats();
    }

    protected Map<String, GarbageCollectorStatistics> getInitialGcStats() {
        return this.initialGcStats;
    }

    @Override
    public void start() {
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
        final ReportConfiguration.Sender rSender = configuration.sender();
        final CommandSender sender = rSender == null ? null : new CommandSender() {
            @Override
            public String getName() {
                return rSender.name;
            }

            @Override
            public UUID getUniqueId() {
                return rSender.uuid;
            }

            @Override
            public void sendMessage(Component message) {

            }

            @Override
            public boolean hasPermission(String permission) {
                return true;
            }
        };
        return new ProfilerReport() {
            final SparkSamplerProtos.SamplerData data = toProto(platform, sender, configuration.threadOrder()::compare, configuration.comment(), configuration.separateParentCalls() ? MergeMode.separateParentCalls(methodDisambiguator) : MergeMode.sameMethod(methodDisambiguator), platform.createClassSourceLookup());

            String uploadedUrl;

            @Override
            public String upload() throws IOException {
                if (uploadedUrl == null)
                    uploadedUrl = SamplerModule.postData(platform, data);
                return uploadedUrl;
            }

            @Override
            public SparkSamplerProtos.SamplerData data() {
                return data;
            }

            @Override
            public Path saveToFile(Path path) throws IOException {
                return Files.write(path, data.toByteArray());
            }
        };
    }

    @Override
    public CompletableFuture<ProfilerReport> whenDone(ReportConfiguration configuration) {
        return getFuture().thenApply(samp -> createReport(configuration));
    }

    protected void writeMetadataToProto(SamplerData.Builder proto, SparkPlatform platform, @Nullable CommandSender creator, @Nullable String comment, DataAggregator dataAggregator) {
        SamplerMetadata.Builder metadata = SamplerMetadata.newBuilder()
                .setPlatformMetadata(platform.getPlugin().getPlatformInfo().toData().toProto())
                .setStartTime(this.startTime)
                .setEndTime(System.currentTimeMillis())
                .setInterval(this.interval)
                .setThreadDumper(this.threadDumper.getMetadata())
                .setDataAggregator(dataAggregator.getMetadata());

        if (creator != null)
             metadata.setCreator(creator.toData().toProto());

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
}

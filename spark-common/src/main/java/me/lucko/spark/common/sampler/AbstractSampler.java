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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.memory.GarbageCollectorStatistics;
import me.lucko.spark.common.platform.MetadataProvider;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.sampler.aggregator.DataAggregator;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.node.ThreadNode;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.common.sampler.window.ProtoTimeEncoder;
import me.lucko.spark.common.sampler.window.WindowStatisticsCollector;
import me.lucko.spark.common.ws.ViewerSocket;
import me.lucko.spark.proto.SparkProtos;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

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

    /** The unix timestamp (in millis) when this sampler should automatically complete. */
    protected final long autoEndTime; // -1 for nothing

    /** If the sampler is running in the background */
    protected boolean background;

    /** Collects statistics for each window in the sample */
    protected final WindowStatisticsCollector windowStatisticsCollector;

    /** A future to encapsulate the completion of this sampler instance */
    protected final CompletableFuture<Sampler> future = new CompletableFuture<>();

    /** The garbage collector statistics when profiling started */
    protected Map<String, GarbageCollectorStatistics> initialGcStats;

    /** A set of viewer sockets linked to the sampler */
    protected List<ViewerSocket> viewerSockets = new CopyOnWriteArrayList<>();

    protected AbstractSampler(SparkPlatform platform, SamplerSettings settings) {
        this.platform = platform;
        this.interval = settings.interval();
        this.threadDumper = settings.threadDumper();
        this.autoEndTime = settings.autoEndTime();
        this.background = settings.runningInBackground();
        this.windowStatisticsCollector = new WindowStatisticsCollector(platform);
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
    public boolean isRunningInBackground() {
        return this.background;
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
    }

    @Override
    public void stop(boolean cancelled) {
        this.windowStatisticsCollector.stop();
        for (ViewerSocket viewerSocket : this.viewerSockets) {
            viewerSocket.processSamplerStopped(this);
        }
    }

    @Override
    public void attachSocket(ViewerSocket socket) {
        this.viewerSockets.add(socket);
    }

    @Override
    public Collection<ViewerSocket> getAttachedSockets() {
        return this.viewerSockets;
    }

    protected void processWindowRotate() {
        this.viewerSockets.removeIf(socket -> {
            if (!socket.isOpen()) {
                return true;
            }

            socket.processWindowRotate(this);
            return false;
        });
    }

    protected void sendStatisticsToSocket() {
        try {
            this.viewerSockets.removeIf(socket -> !socket.isOpen());
            if (this.viewerSockets.isEmpty()) {
                return;
            }

            SparkProtos.PlatformStatistics platform = this.platform.getStatisticsProvider().getPlatformStatistics(getInitialGcStats(), false);
            SparkProtos.SystemStatistics system = this.platform.getStatisticsProvider().getSystemStatistics();

            for (ViewerSocket viewerSocket : this.viewerSockets) {
                viewerSocket.sendUpdatedStatistics(platform, system);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void writeMetadataToProto(SamplerData.Builder proto, SparkPlatform platform, CommandSender.Data creator, String comment, DataAggregator dataAggregator) {
        SamplerMetadata.Builder metadata = SamplerMetadata.newBuilder()
                .setSamplerMode(getMode().asProto())
                .setPlatformMetadata(platform.getPlugin().getPlatformInfo().toData().toProto())
                .setCreator(creator.toProto())
                .setStartTime(this.startTime)
                .setEndTime(System.currentTimeMillis())
                .setInterval(this.interval)
                .setThreadDumper(this.threadDumper.getMetadata())
                .setDataAggregator(dataAggregator.getMetadata());

        if (comment != null) {
            metadata.setComment(comment);
        }

        int totalTicks = this.windowStatisticsCollector.getTotalTicks();
        if (totalTicks != -1) {
            metadata.setNumberOfTicks(totalTicks);
        }

        try {
            metadata.setPlatformStatistics(platform.getStatisticsProvider().getPlatformStatistics(getInitialGcStats(), true));
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
            if (serverConfigProvider != null) {
                metadata.putAllServerConfigurations(serverConfigProvider.export());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            MetadataProvider extraMetadataProvider = platform.getPlugin().createExtraMetadataProvider();
            if (extraMetadataProvider != null) {
                metadata.putAllExtraPlatformMetadata(extraMetadataProvider.export());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Collection<SourceMetadata> knownSources = platform.getPlugin().getKnownSources();
        for (SourceMetadata source : knownSources) {
            metadata.putSources(source.getName().toLowerCase(Locale.ROOT), source.toProto());
        }

        proto.setMetadata(metadata);
    }

    protected void writeDataToProto(SamplerData.Builder proto, DataAggregator dataAggregator, MergeMode mergeMode, ClassSourceLookup classSourceLookup) {
        List<ThreadNode> data = dataAggregator.exportData();
        data.sort(Comparator.comparing(ThreadNode::getThreadLabel));

        ClassSourceLookup.Visitor classSourceVisitor = ClassSourceLookup.createVisitor(classSourceLookup);

        ProtoTimeEncoder timeEncoder = new ProtoTimeEncoder(getMode().valueTransformer(), data);
        int[] timeWindows = timeEncoder.getKeys();
        for (int timeWindow : timeWindows) {
            proto.addTimeWindows(timeWindow);
        }

        this.windowStatisticsCollector.ensureHasStatisticsForAllWindows(timeWindows);
        proto.putAllTimeWindowStatistics(this.windowStatisticsCollector.export());

        for (ThreadNode entry : data) {
            proto.addThreads(entry.toProto(mergeMode, timeEncoder));
            classSourceVisitor.visit(entry);
        }

        if (classSourceVisitor.hasClassSourceMappings()) {
            proto.putAllClassSources(classSourceVisitor.getClassSourceMapping());
        }

        if (classSourceVisitor.hasMethodSourceMappings()) {
            proto.putAllMethodSources(classSourceVisitor.getMethodSourceMapping());
        }

        if (classSourceVisitor.hasLineSourceMappings()) {
            proto.putAllLineSources(classSourceVisitor.getLineSourceMapping());
        }
    }
}

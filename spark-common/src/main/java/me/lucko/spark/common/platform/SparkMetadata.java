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

package me.lucko.spark.common.platform;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.monitor.memory.GarbageCollectorStatistics;
import me.lucko.spark.common.platform.serverconfig.ServerConfigProvider;
import me.lucko.spark.common.sampler.source.SourceMetadata;
import me.lucko.spark.proto.SparkHeapProtos.HeapMetadata;
import me.lucko.spark.proto.SparkProtos.HealthMetadata;
import me.lucko.spark.proto.SparkProtos.PlatformMetadata;
import me.lucko.spark.proto.SparkProtos.PlatformStatistics;
import me.lucko.spark.proto.SparkProtos.SystemStatistics;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;

public class SparkMetadata {

    public static SparkMetadata gather(SparkPlatform platform, CommandSender.Data creator, Map<String, GarbageCollectorStatistics> initialGcStats) {
        PlatformMetadata platformMetadata = platform.getPlugin().getPlatformInfo().toData().toProto();

        PlatformStatistics platformStatistics = null;
        try {
            platformStatistics = platform.getStatisticsProvider().getPlatformStatistics(initialGcStats, true);
        } catch (Exception e) {
            platform.getPlugin().log(Level.WARNING, "Failed to gather platform statistics", e);
        }

        SystemStatistics systemStatistics = null;
        try {
            systemStatistics = platform.getStatisticsProvider().getSystemStatistics();
        } catch (Exception e) {
            platform.getPlugin().log(Level.WARNING, "Failed to gather system statistics", e);
        }

        long generatedTime = System.currentTimeMillis();

        Map<String, String> serverConfigurations = null;
        try {
            ServerConfigProvider serverConfigProvider = platform.getPlugin().createServerConfigProvider();
            if (serverConfigProvider != null) {
                serverConfigurations = serverConfigProvider.export();
            }
        } catch (Exception e) {
            platform.getPlugin().log(Level.WARNING, "Failed to gather server configurations", e);
        }

        Collection<SourceMetadata> sources = platform.getPlugin().getKnownSources();

        Map<String, String> extraPlatformMetadata = null;
        try {
            MetadataProvider extraMetadataProvider = platform.getPlugin().createExtraMetadataProvider();
            if (extraMetadataProvider != null) {
                extraPlatformMetadata = extraMetadataProvider.export();
            }
        } catch (Exception e) {
            platform.getPlugin().log(Level.WARNING, "Failed to gather extra platform metadata", e);
        }

        return new SparkMetadata(creator, platformMetadata, platformStatistics, systemStatistics, generatedTime, serverConfigurations, sources, extraPlatformMetadata);
    }

    private final CommandSender.Data creator;
    private final PlatformMetadata platformMetadata;
    private final PlatformStatistics platformStatistics;
    private final SystemStatistics systemStatistics;
    private final long generatedTime;
    private final Map<String, String> serverConfigurations;
    private final Collection<SourceMetadata> sources;
    private final Map<String, String> extraPlatformMetadata;

    public SparkMetadata(CommandSender.Data creator, PlatformMetadata platformMetadata, PlatformStatistics platformStatistics, SystemStatistics systemStatistics, long generatedTime, Map<String, String> serverConfigurations, Collection<SourceMetadata> sources, Map<String, String> extraPlatformMetadata) {
        this.creator = creator;
        this.platformMetadata = platformMetadata;
        this.platformStatistics = platformStatistics;
        this.systemStatistics = systemStatistics;
        this.generatedTime = generatedTime;
        this.serverConfigurations = serverConfigurations;
        this.sources = sources;
        this.extraPlatformMetadata = extraPlatformMetadata;
    }

    @SuppressWarnings("DuplicatedCode")
    public void writeTo(HealthMetadata.Builder builder) {
        if (this.creator != null) builder.setCreator(this.creator.toProto());
        if (this.platformMetadata != null) builder.setPlatformMetadata(this.platformMetadata);
        if (this.platformStatistics != null) builder.setPlatformStatistics(this.platformStatistics);
        if (this.systemStatistics != null) builder.setSystemStatistics(this.systemStatistics);
        builder.setGeneratedTime(this.generatedTime);
        if (this.serverConfigurations != null) builder.putAllServerConfigurations(this.serverConfigurations);
        if (this.sources != null) {
            for (SourceMetadata source : this.sources) {
                builder.putSources(source.getName().toLowerCase(Locale.ROOT), source.toProto());
            }
        }
        if (this.extraPlatformMetadata != null) builder.putAllExtraPlatformMetadata(this.extraPlatformMetadata);
    }

    @SuppressWarnings("DuplicatedCode")
    public void writeTo(SamplerMetadata.Builder builder) {
        if (this.creator != null) builder.setCreator(this.creator.toProto());
        if (this.platformMetadata != null) builder.setPlatformMetadata(this.platformMetadata);
        if (this.platformStatistics != null) builder.setPlatformStatistics(this.platformStatistics);
        if (this.systemStatistics != null) builder.setSystemStatistics(this.systemStatistics);
        builder.setEndTime(this.generatedTime);
        if (this.serverConfigurations != null) builder.putAllServerConfigurations(this.serverConfigurations);
        if (this.sources != null) {
            for (SourceMetadata source : this.sources) {
                builder.putSources(source.getName().toLowerCase(Locale.ROOT), source.toProto());
            }
        }
        if (this.extraPlatformMetadata != null) builder.putAllExtraPlatformMetadata(this.extraPlatformMetadata);
    }

    @SuppressWarnings("DuplicatedCode")
    public void writeTo(HeapMetadata.Builder builder) {
        if (this.creator != null) builder.setCreator(this.creator.toProto());
        if (this.platformMetadata != null) builder.setPlatformMetadata(this.platformMetadata);
        if (this.platformStatistics != null) builder.setPlatformStatistics(this.platformStatistics);
        if (this.systemStatistics != null) builder.setSystemStatistics(this.systemStatistics);
        builder.setGeneratedTime(this.generatedTime);
        if (this.serverConfigurations != null) builder.putAllServerConfigurations(this.serverConfigurations);
        if (this.sources != null) {
            for (SourceMetadata source : this.sources) {
                builder.putSources(source.getName().toLowerCase(Locale.ROOT), source.toProto());
            }
        }
        if (this.extraPlatformMetadata != null) builder.putAllExtraPlatformMetadata(this.extraPlatformMetadata);
    }

}

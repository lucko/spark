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

package me.lucko.spark.common.heapdump;

import me.lucko.spark.api.heap.HeapAnalysis;
import me.lucko.spark.api.heap.HeapSummaryReport;
import me.lucko.spark.api.util.Sender;
import me.lucko.spark.api.util.UploadResult;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.modules.HeapAnalysisModule;
import me.lucko.spark.proto.SparkHeapProtos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HeapAnalysisProvider implements HeapAnalysis {
    private final SparkPlatform platform;

    public HeapAnalysisProvider(SparkPlatform platform) {
        this.platform = platform;
    }

    @Override
    public @NotNull HeapSummaryReport summary(Sender sender) {
        final SparkHeapProtos.HeapData data = HeapDumpSummary.createNew().toProto(platform, sender);
        return new HeapSummaryReport() {
            UploadResult uploadResult;

            @Override
            @NonNull
            public UploadResult upload() throws IOException {
                if (uploadResult == null)
                    uploadResult = HeapAnalysisModule.upload(platform, data);
                return uploadResult;
            }

            @NotNull
            @Override
            public SparkHeapProtos.HeapData data() {
                return data;
            }

            @Override
            public @NotNull Path saveToFile(Path path) throws IOException {
                return Files.write(path, data.toByteArray());
            }
        };
    }

    @Override
    public @NotNull Path dumpHeap(Path outputPath, boolean liveOnly) throws Exception {
        HeapDump.dumpHeap(outputPath, liveOnly);
        return outputPath;
    }
}

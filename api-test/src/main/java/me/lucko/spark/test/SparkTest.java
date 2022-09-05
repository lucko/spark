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

package me.lucko.spark.test;

import com.google.protobuf.CodedInputStream;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.profiler.Profiler;
import me.lucko.spark.api.profiler.ProfilerConfiguration;
import me.lucko.spark.api.profiler.dumper.SpecificThreadDumper;
import me.lucko.spark.api.profiler.dumper.ThreadDumper;
import me.lucko.spark.api.profiler.report.ProfilerReport;
import me.lucko.spark.api.profiler.report.ReportConfiguration;
import me.lucko.spark.api.profiler.thread.ThreadGrouper;
import me.lucko.spark.api.util.ErrorHandler;
import me.lucko.spark.api.util.UploadResult;
import me.lucko.spark.proto.SparkSamplerProtos;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static net.minecraft.commands.Commands.literal;

@Mod("sparktest")
@Mod.EventBusSubscriber
public class SparkTest {

    private static Profiler profiler;
    private static Path savePath;

    public SparkTest() {
        // Mod loading is parallel, so we're not assured that spark will be loaded before us
        // As such, get the profiler once spark loads
        SparkProvider.whenLoaded(spark -> profiler = spark.profiler(12) /* Request a profiler capable of managing 12 active samplers */);

        savePath = FMLPaths.GAMEDIR.get().resolve("sparktest");
    }

    @SubscribeEvent
    static void serverStop(final ServerStoppingEvent event) {
        profiler.stop();
    }

    @SubscribeEvent
    static void registerCommand(final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("sparktest")
                .then(literal("test1")
                        .executes(throwingCommand(SparkTest::test1)))
                .then(literal("test2")
                        .executes(throwingCommand(SparkTest::test2))));
    }

    private static void test1(CommandContext<CommandSourceStack> ctx) throws Exception {
        final var source = ctx.getSource();
        source.sendFailure(Component.literal("Building sampler... stand by."));
        // Create the sampler
        final Profiler.Sampler sampler = profiler.createSamplerThrowing(ProfilerConfiguration.builder()
                .dumper(new SpecificThreadDumper(ServerLifecycleHooks.getCurrentServer().getRunningThread()))
                .grouper(ThreadGrouper.BY_NAME)
                .ignoreSleeping()
                .samplingInterval(12)
                .forceJavaSampler()
                .duration(Duration.ofSeconds(20))
                .build());

        sampler.start(); // Start the sampler

        source.sendSuccess(Component.literal("Started sampler. Please await the results in the next 20 seconds."), false);

        // Await sampler completion and execute callback once the sampler is completed
        sampler.onCompleted(ReportConfiguration.builder()
                        .separateParentCalls(true).build())
                .whenComplete(LamdbaExceptionUtils.rethrowBiConsumer((report, t) -> {
                    final SamplerData data = report.data();
                    source.sendSuccess(Component.literal("Profiling done. Profiled threads: " + data.getThreadsList()
                            .stream()
                            .map(SparkSamplerProtos.ThreadNode::getName)
                            .toList()), false);
                    final Path path = report.saveToFile(savePath.resolve("test1.sparkprofile"));
                    try (final var is = Files.newInputStream(path)) {
                        final SamplerData fromBytes = SparkSamplerProtos.SamplerData.parseFrom(is);
                        final var isEqual = data.equals(fromBytes);
                        if (isEqual) {
                            source.sendSuccess(Component.literal("Results from bytes and from memory are equal!"), false);
                        } else {
                            source.sendFailure(Component.literal("Results from bytes and from memory do not match!"));
                        }
                    }
                }));
    }

    private static void test2(final CommandContext<CommandSourceStack> context) throws Exception {
        final var source = context.getSource();
        source.sendFailure(Component.literal("Building sampler... Please stand by."));
        // Create the sampler
        final Profiler.Sampler sampler = profiler.createSamplerThrowing(ProfilerConfiguration.builder()
                .dumper(ThreadDumper.ALL)
                .grouper(ThreadGrouper.AS_ONE)
                .ignoreNative()
                .build());

        sampler.start(); // Start the profiler
        source.sendSuccess(Component.literal("Profiler started..."), true);
        Thread.sleep(1000 * 5); // Wait 5 seconds
        sampler.stop(); // Stop the profiler

        // Dump the report
        final ProfilerReport report = sampler.dumpReport(ReportConfiguration.onlySender("My test"));
        final Path saveFile = report.saveToFile(savePath.resolve("test2.sparkprofile")); // Save the report
        final UploadResult uploadResult = report.upload();
        try (final var localIs = Files.newInputStream(saveFile);
                final var onlineIs = URI.create(uploadResult.getBytebinUrl()).toURL().openStream()) {
            final SamplerData data = report.data();
            final CodedInputStream localCd = CodedInputStream.newInstance(localIs);
            localCd.setRecursionLimit(Integer.MAX_VALUE);
            final SamplerData fromLocal = SamplerData.parseFrom(localCd);
            final CodedInputStream onlineCd = CodedInputStream.newInstance(onlineIs);
            onlineCd.setRecursionLimit(Integer.MAX_VALUE);
            final SamplerData fromOnline = SamplerData.parseFrom(onlineCd);
            if (data.equals(fromLocal) && fromLocal.equals(fromOnline)) {
                source.sendSuccess(Component.literal("Results from local file, memory and Bytebin are equal!"), false);
            } else {
                source.sendFailure(Component.literal("Results do not match!"));
            }
        }
    }

    private static <S> Command<S> throwingCommand(LamdbaExceptionUtils.Consumer_WithExceptions<CommandContext<S>, Exception> consumer) {
        return ctx -> {
            try {
                consumer.accept(ctx);
                return 1;
            } catch (Exception e) {
                throw new CommandRuntimeException(Component.literal(e.toString()));
            }
        };
    }
}

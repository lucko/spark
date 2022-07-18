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

package me.lucko.spark.common.command.modules;

import com.google.common.collect.Iterables;
import me.lucko.spark.api.SparkProvider;
import me.lucko.spark.api.profiler.Profiler;
import me.lucko.spark.api.profiler.dumper.RegexThreadDumper;
import me.lucko.spark.api.profiler.dumper.SpecificThreadDumper;
import me.lucko.spark.api.profiler.dumper.ThreadDumper;
import me.lucko.spark.api.profiler.report.ProfilerReport;
import me.lucko.spark.api.profiler.report.ReportConfiguration;
import me.lucko.spark.api.profiler.thread.ThreadGrouper;
import me.lucko.spark.api.profiler.thread.ThreadOrder;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.activitylog.Activity;
import me.lucko.spark.common.command.Arguments;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.command.tabcomplete.CompletionSupplier;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.sampler.ProfilerService;
import me.lucko.spark.common.sampler.SamplerBuilder;
import me.lucko.spark.proto.SparkSamplerProtos;
import net.kyori.adventure.text.event.ClickEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;

public class SamplerModule implements CommandModule {
    private static final String SPARK_SAMPLER_MEDIA_TYPE = "application/x-spark-sampler";

    private final Profiler profiler;

    public SamplerModule(SparkPlatform platform) {
        profiler = new ProfilerService(platform, 1);
    }

    @Override
    public void close() {
        profiler.stop();
    }

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("profiler", "sampler")
                .argumentUsage("info", null)
                .argumentUsage("stop", null)
                .argumentUsage("cancel", null)
                .argumentUsage("interval", "interval millis")
                .argumentUsage("thread", "thread name")
                .argumentUsage("only-ticks-over", "tick length millis")
                .argumentUsage("timeout", "timeout seconds")
                .argumentUsage("regex --thread", "thread regex")
                .argumentUsage("combine-all", null)
                .argumentUsage("not-combined", null)
                .argumentUsage("force-java-sampler", null)
                .argumentUsage("stop --comment", "comment")
                .argumentUsage("stop --order-by-time", null)
                .argumentUsage("stop --save-to-file", null)
                .executor((platform, sender, resp, args) -> profiler(platform, resp, args))
                .tabCompleter((platform, sender, arguments) -> {
                    if (arguments.contains("--info") || arguments.contains("--cancel")) {
                        return Collections.emptyList();
                    }

                    if (arguments.contains("--stop") || arguments.contains("--upload")) {
                        return TabCompleter.completeForOpts(arguments, "--order-by-time", "--comment", "--save-to-file");
                    }

                    List<String> opts = new ArrayList<>(Arrays.asList("--info", "--stop", "--cancel",
                            "--timeout", "--regex", "--combine-all", "--not-combined", "--interval",
                            "--only-ticks-over", "--force-java-sampler"));
                    opts.removeAll(arguments);
                    opts.add("--thread"); // allowed multiple times

                    return TabCompleter.create()
                            .from(0, CompletionSupplier.startsWith(opts))
                            .complete(arguments);
                })
                .build()
        );
    }

    private void profiler(SparkPlatform platform, CommandResponseHandler resp, Arguments arguments) {
        if (arguments.boolFlag("info")) {
            profilerInfo(resp);
            return;
        }

        if (arguments.boolFlag("cancel")) {
            profilerCancel(resp);
            return;
        }

        if (arguments.boolFlag("stop") || arguments.boolFlag("upload")) {
            profilerStop(platform, resp, arguments);
            return;
        }

        profilerStart(platform, resp, arguments);
    }

    private void profilerStart(SparkPlatform platform, CommandResponseHandler resp, Arguments arguments) {
        resp.broadcastPrefixed(text("Initializing a new profiler, please wait..."));
        
        int timeoutSeconds = arguments.intFlag("timeout");
        double intervalMillis = arguments.doubleFlag("interval");
        if (intervalMillis <= 0) {
            intervalMillis = 4;
        }

        boolean ignoreSleeping = arguments.boolFlag("ignore-sleeping");
        boolean ignoreNative = arguments.boolFlag("ignore-native");
        boolean forceJavaSampler = arguments.boolFlag("force-java-sampler");

        Set<String> threads = arguments.stringFlag("thread");
        ThreadDumper threadDumper;
        if (threads.isEmpty()) {
            // use the server thread
            threadDumper = platform.getPlugin().getDefaultThreadDumper();
        } else if (threads.contains("*")) {
            threadDumper = ThreadDumper.ALL;
        } else {
            if (arguments.boolFlag("regex")) {
                threadDumper = new RegexThreadDumper(threads);
            } else {
                // specific matches
                threadDumper = new SpecificThreadDumper(threads);
            }
        }

        ThreadGrouper threadGrouper;
        if (arguments.boolFlag("combine-all")) {
            threadGrouper = ThreadGrouper.AS_ONE;
        } else if (arguments.boolFlag("not-combined")) {
            threadGrouper = ThreadGrouper.BY_NAME;
        } else {
            threadGrouper = ThreadGrouper.BY_POOL;
        }

        int ticksOver = arguments.intFlag("only-ticks-over");

        SamplerBuilder builder = new SamplerBuilder();
        builder.dumper(threadDumper);
        builder.grouper(threadGrouper);
        if (timeoutSeconds != -1) {
            builder.completeAfter(timeoutSeconds, TimeUnit.SECONDS);
        }
        builder.samplingInterval(intervalMillis);
        builder.ignoreSleeping(ignoreSleeping);
        builder.ignoreNative(ignoreNative);
        builder.forceJavaSampler(forceJavaSampler);
        if (ticksOver != -1) {
            builder.minimumTickDuration(ticksOver);
        }
        final Profiler.Sampler sampler = profiler.createSampler(builder.build(), e -> resp.replyPrefixed(text(e, RED)));
        if (sampler == null) // Feedback is handled in the consumer
            return;

        sampler.start();

        resp.broadcastPrefixed(text()
                .append(text("Profiler now active!", GOLD))
                .append(space())
                .append(text("(" + (sampler.isAsync() ? "async" : "built-in java") + ")", DARK_GRAY))
                .build()
        );
        if (timeoutSeconds == -1) {
            resp.broadcastPrefixed(text("Use '/" + platform.getPlugin().getCommandName() + " profiler --stop' to stop profiling and upload the results."));
        } else {
            resp.broadcastPrefixed(text("The results will be automatically returned after the profiler has been running for " + timeoutSeconds + " seconds."));
        }

        final CompletableFuture<Profiler.Sampler> future = sampler.onCompleted();

        // send message if profiling fails
        future.whenCompleteAsync((s, throwable) -> {
            if (throwable != null) {
                resp.broadcastPrefixed(text("Profiler operation failed unexpectedly. Error: " + throwable, RED));
                throwable.printStackTrace();
            }
        });

        // await the result
        if (timeoutSeconds != -1) {
            ThreadOrder threadOrder = arguments.boolFlag("order-by-time") ? ThreadOrder.BY_TIME : ThreadOrder.BY_NAME;
            String comment = Iterables.getFirst(arguments.stringFlag("comment"), null);
            boolean sepPar = arguments.boolFlag("separate-parent-calls");
            boolean saveToFile = arguments.boolFlag("save-to-file");
            sampler.onCompleted(configuration(resp, comment, sepPar, threadOrder)).thenAcceptAsync(report -> {
                resp.broadcastPrefixed(text("The active profiler has completed! Uploading results..."));
                handleUpload(platform, resp, report, saveToFile);
            });
        }
    }

    private void profilerInfo(CommandResponseHandler resp) {
        final Profiler.Sampler active = activeSampler();
        if (active == null) {
            resp.replyPrefixed(text("There isn't an active profiler running."));
        } else {
            long timeout = active.getAutoEndTime();
            if (timeout == -1) {
                resp.replyPrefixed(text("There is an active profiler currently running, with no defined timeout."));
            } else {
                long timeoutDiff = (timeout - System.currentTimeMillis()) / 1000L;
                resp.replyPrefixed(text("There is an active profiler currently running, due to timeout in " + timeoutDiff + " seconds."));
            }

            long runningTime = (System.currentTimeMillis() - active.getStartTime()) / 1000L;
            resp.replyPrefixed(text("It has been profiling for " + runningTime + " seconds so far."));
        }
    }

    private void profilerCancel(CommandResponseHandler resp) {
        if (activeSampler() == null) {
            resp.replyPrefixed(text("There isn't an active profiler running."));
        } else {
            close();
            resp.broadcastPrefixed(text("The active profiler has been cancelled.", GOLD));
        }
    }

    private void profilerStop(SparkPlatform platform, CommandResponseHandler resp, Arguments arguments) {
        final Profiler.Sampler sampler = activeSampler();
        if (sampler == null) {
            resp.replyPrefixed(text("There isn't an active profiler running."));
        } else {
            sampler.stop();
            resp.broadcastPrefixed(text("The active profiler has been stopped! Uploading results..."));
            final ThreadOrder threadOrder = arguments.boolFlag("order-by-time") ? ThreadOrder.BY_TIME : ThreadOrder.BY_NAME;
            String comment = Iterables.getFirst(arguments.stringFlag("comment"), null);
            boolean sepParentCalls = arguments.boolFlag("separate-parent-calls");
            boolean saveToFile = arguments.boolFlag("save-to-file");
            handleUpload(platform, resp, sampler.dumpReport(configuration(resp, comment, sepParentCalls, threadOrder)), saveToFile);
        }
    }
    
    private Profiler.Sampler activeSampler() {
        if (profiler.activeSamplers().isEmpty()) return null;
        return profiler.activeSamplers().get(0);
    }

    public static String postData(SparkPlatform platform, SparkSamplerProtos.SamplerData output) throws IOException {
        String key = platform.getBytebinClient().postContent(output, SPARK_SAMPLER_MEDIA_TYPE).key();
        return platform.getViewerUrl() + key;
    }

    private ReportConfiguration configuration(CommandResponseHandler resp, String comment, boolean separateParentCalls, ThreadOrder order) {
        return ReportConfiguration.builder()
                .order(order)
                .comment(comment)
                .separateParentCalls(separateParentCalls)
                .sender(resp.sender().asSender())
                .build();
    }

    private void handleUpload(SparkPlatform platform, CommandResponseHandler resp, ProfilerReport report, boolean saveToFileFlag) {
        boolean saveToFile = false;
        if (saveToFileFlag) {
            saveToFile = true;
        } else {
            try {
                final String url = report.upload();

                resp.broadcastPrefixed(text("Profiler results:", GOLD));
                resp.broadcast(text()
                        .content(url)
                        .color(GRAY)
                        .clickEvent(ClickEvent.openUrl(url))
                        .build()
                );

                platform.getActivityLog().addToLog(Activity.urlActivity(resp.sender(), System.currentTimeMillis(), "Profiler", url));
            } catch (Exception e) {
                resp.broadcastPrefixed(text("An error occurred whilst uploading the results. Attempting to save to disk instead.", RED));
                e.printStackTrace();
                saveToFile = true;
            }
        }

        if (saveToFile) {
            Path file = platform.resolveSaveFile("profile", "sparkprofile");
            try {
                report.saveToFile(file);

                resp.broadcastPrefixed(text()
                        .content("Profile written to: ")
                        .color(GOLD)
                        .append(text(file.toString(), GRAY))
                        .build()
                );
                resp.broadcastPrefixed(text("You can read the profile file using the viewer web-app - " + platform.getViewerUrl(), GRAY));

                platform.getActivityLog().addToLog(Activity.fileActivity(resp.sender(), System.currentTimeMillis(), "Profiler", file.toString()));
            } catch (IOException e) {
                resp.broadcastPrefixed(text("An error occurred whilst saving the data.", RED));
                e.printStackTrace();
            }
        }
    }
}

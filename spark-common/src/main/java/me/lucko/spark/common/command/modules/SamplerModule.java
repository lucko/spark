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
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.activitylog.ActivityLog.Activity;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.CommandResponseHandler;
import me.lucko.spark.common.command.tabcomplete.CompletionSupplier;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.sampler.Sampler;
import me.lucko.spark.common.sampler.SamplerBuilder;
import me.lucko.spark.common.sampler.ThreadDumper;
import me.lucko.spark.common.sampler.ThreadGrouper;
import me.lucko.spark.common.sampler.ThreadNodeOrder;
import me.lucko.spark.common.sampler.async.AsyncSampler;
import me.lucko.spark.common.sampler.node.MergeMode;
import me.lucko.spark.common.sampler.tick.TickHook;
import me.lucko.spark.common.util.MethodDisambiguator;
import net.kyori.adventure.text.event.ClickEvent;
import okhttp3.MediaType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class SamplerModule implements CommandModule {
    private static final MediaType SPARK_SAMPLER_MEDIA_TYPE = MediaType.parse("application/x-spark-sampler");

    /** The sampler instance currently running, if any */
    private Sampler activeSampler = null;

    @Override
    public void close() {
        if (this.activeSampler != null) {
            this.activeSampler.stop();
            this.activeSampler = null;
        }
    }

    @Override
    public void registerCommands(Consumer<Command> consumer) {
        consumer.accept(Command.builder()
                .aliases("profiler", "sampler")
                .argumentUsage("info", null)
                .argumentUsage("stop", null)
                .argumentUsage("cancel", null)
                .argumentUsage("timeout", "timeout seconds")
                .argumentUsage("comment", "comment")
                .argumentUsage("thread", "thread name")
                .argumentUsage("regex", null)
                .argumentUsage("combine-all", null)
                .argumentUsage("not-combined", null)
                .argumentUsage("interval", "interval millis")
                .argumentUsage("only-ticks-over", "tick length millis")
                .argumentUsage("ignore-sleeping", null)
                .argumentUsage("ignore-native", null)
                .argumentUsage("force-java-sampler", null)
                .argumentUsage("order-by-time", null)
                .argumentUsage("separate-parent-calls", null)
                .executor((platform, sender, resp, arguments) -> {
                    if (arguments.boolFlag("info")) {
                        if (this.activeSampler == null) {
                            resp.replyPrefixed(text("There isn't an active sampling task running."));
                        } else {
                            long timeout = this.activeSampler.getEndTime();
                            if (timeout == -1) {
                                resp.replyPrefixed(text("There is an active sampler currently running, with no defined timeout."));
                            } else {
                                long timeoutDiff = (timeout - System.currentTimeMillis()) / 1000L;
                                resp.replyPrefixed(text("There is an active sampler currently running, due to timeout in " + timeoutDiff + " seconds."));
                            }

                            long runningTime = (System.currentTimeMillis() - this.activeSampler.getStartTime()) / 1000L;
                            resp.replyPrefixed(text("It has been sampling for " + runningTime + " seconds so far."));
                        }
                        return;
                    }

                    if (arguments.boolFlag("cancel")) {
                        if (this.activeSampler == null) {
                            resp.replyPrefixed(text("There isn't an active sampling task running."));
                        } else {
                            close();
                            resp.broadcastPrefixed(text("The active sampling task has been cancelled.", GOLD));
                        }
                        return;
                    }

                    if (arguments.boolFlag("stop") || arguments.boolFlag("upload")) {
                        if (this.activeSampler == null) {
                            resp.replyPrefixed(text("There isn't an active sampling task running."));
                        } else {
                            this.activeSampler.stop();
                            resp.broadcastPrefixed(text("The active sampling operation has been stopped! Uploading results..."));
                            ThreadNodeOrder threadOrder = arguments.boolFlag("order-by-time") ? ThreadNodeOrder.BY_TIME : ThreadNodeOrder.BY_NAME;
                            String comment = Iterables.getFirst(arguments.stringFlag("comment"), null);
                            MethodDisambiguator methodDisambiguator = new MethodDisambiguator();
                            MergeMode mergeMode = arguments.boolFlag("separate-parent-calls") ? MergeMode.separateParentCalls(methodDisambiguator) : MergeMode.sameMethod(methodDisambiguator);
                            handleUpload(platform, resp, this.activeSampler, threadOrder, comment, mergeMode);
                            this.activeSampler = null;
                        }
                        return;
                    }

                    int timeoutSeconds = arguments.intFlag("timeout");
                    if (timeoutSeconds != -1 && timeoutSeconds <= 10) {
                        resp.replyPrefixed(text("The specified timeout is not long enough for accurate results to be formed. " +
                                "Please choose a value greater than 10.", RED));
                        return;
                    }

                    if (timeoutSeconds != -1 && timeoutSeconds < 30) {
                        resp.replyPrefixed(text("The accuracy of the output will significantly improve when sampling is able to run for longer periods. " +
                                "Consider setting a timeout value over 30 seconds."));
                    }

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
                            threadDumper = new ThreadDumper.Regex(threads);
                        } else {
                            // specific matches
                            threadDumper = new ThreadDumper.Specific(threads);
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
                    TickHook tickHook = null;
                    if (ticksOver != -1) {
                        tickHook = platform.getTickHook();
                        if (tickHook == null) {
                            resp.replyPrefixed(text("Tick counting is not supported!", RED));
                            return;
                        }
                    }

                    if (this.activeSampler != null) {
                        resp.replyPrefixed(text("An active sampler is already running."));
                        return;
                    }

                    resp.broadcastPrefixed(text("Initializing a new profiler, please wait..."));

                    SamplerBuilder builder = new SamplerBuilder();
                    builder.threadDumper(threadDumper);
                    builder.threadGrouper(threadGrouper);
                    if (timeoutSeconds != -1) {
                        builder.completeAfter(timeoutSeconds, TimeUnit.SECONDS);
                    }
                    builder.samplingInterval(intervalMillis);
                    builder.ignoreSleeping(ignoreSleeping);
                    builder.ignoreNative(ignoreNative);
                    builder.forceJavaSampler(forceJavaSampler);
                    if (ticksOver != -1) {
                        builder.ticksOver(ticksOver, tickHook);
                    }
                    Sampler sampler = this.activeSampler = builder.start();

                    resp.broadcastPrefixed(text()
                            .append(text("Profiler now active!", GOLD))
                            .append(space())
                            .append(text("(" + (sampler instanceof AsyncSampler ? "async" : "built-in java") + ")", DARK_GRAY))
                            .build()
                    );
                    if (timeoutSeconds == -1) {
                        resp.broadcastPrefixed(text("Use '/" + platform.getPlugin().getCommandName() + " profiler --stop' to stop profiling and upload the results."));
                    } else {
                        resp.broadcastPrefixed(text("The results will be automatically returned after the profiler has been running for " + timeoutSeconds + " seconds."));
                    }

                    CompletableFuture<? extends Sampler> future = this.activeSampler.getFuture();

                    // send message if profiling fails
                    future.whenCompleteAsync((s, throwable) -> {
                        if (throwable != null) {
                            resp.broadcastPrefixed(text("Sampling operation failed unexpectedly. Error: " + throwable.toString(), RED));
                            throwable.printStackTrace();
                        }
                    });

                    // set activeSampler to null when complete.
                    future.whenCompleteAsync((s, throwable) -> {
                        if (sampler == this.activeSampler) {
                            this.activeSampler = null;
                        }
                    });

                    // await the result
                    if (timeoutSeconds != -1) {
                        ThreadNodeOrder threadOrder = arguments.boolFlag("order-by-time") ? ThreadNodeOrder.BY_TIME : ThreadNodeOrder.BY_NAME;
                        String comment = Iterables.getFirst(arguments.stringFlag("comment"), null);
                        MethodDisambiguator methodDisambiguator = new MethodDisambiguator();
                        MergeMode mergeMode = arguments.boolFlag("separate-parent-calls") ? MergeMode.separateParentCalls(methodDisambiguator) : MergeMode.sameMethod(methodDisambiguator);
                        future.thenAcceptAsync(s -> {
                            resp.broadcastPrefixed(text("The active sampling operation has completed! Uploading results..."));
                            handleUpload(platform, resp, s, threadOrder, comment, mergeMode);
                        });
                    }
                })
                .tabCompleter((platform, sender, arguments) -> {
                    if (arguments.contains("--info") || arguments.contains("--cancel")) {
                        return Collections.emptyList();
                    }

                    if (arguments.contains("--stop") || arguments.contains("--upload")) {
                        return TabCompleter.completeForOpts(arguments, "--order-by-time", "--separate-parent-calls", "--comment");
                    }

                    List<String> opts = new ArrayList<>(Arrays.asList("--info", "--stop", "--cancel",
                            "--timeout", "--regex", "--combine-all", "--not-combined", "--interval",
                            "--only-ticks-over", "--ignore-sleeping", "--ignore-native", "--force-java-sampler",
                            "--order-by-time", "--separate-parent-calls", "--comment"));
                    opts.removeAll(arguments);
                    opts.add("--thread"); // allowed multiple times

                    return TabCompleter.create()
                            .from(0, CompletionSupplier.startsWith(opts))
                            .complete(arguments);
                })
                .build()
        );
    }

    private void handleUpload(SparkPlatform platform, CommandResponseHandler resp, Sampler sampler, ThreadNodeOrder threadOrder, String comment, MergeMode mergeMode) {
        platform.getPlugin().executeAsync(() -> {
            byte[] output = sampler.formCompressedDataPayload(platform.getPlugin().getPlatformInfo(), resp.sender(), threadOrder, comment, mergeMode);
            try {
                String key = SparkPlatform.BYTEBIN_CLIENT.postContent(output, SPARK_SAMPLER_MEDIA_TYPE, false).key();
                String url = SparkPlatform.VIEWER_URL + key;

                resp.broadcastPrefixed(text("Sampling results:", GOLD));
                resp.broadcast(text()
                        .content(url)
                        .color(GRAY)
                        .clickEvent(ClickEvent.openUrl(url))
                        .build()
                );

                platform.getActivityLog().addToLog(Activity.urlActivity(resp.sender(), System.currentTimeMillis(), "Sampler", url));
            } catch (IOException e) {
                resp.broadcastPrefixed(text("An error occurred whilst uploading the results.", RED));
                e.printStackTrace();
            }
        });
    }
}

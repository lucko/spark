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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.Command;
import me.lucko.spark.common.command.CommandModule;
import me.lucko.spark.common.command.tabcomplete.CompletionSupplier;
import me.lucko.spark.common.command.tabcomplete.TabCompleter;
import me.lucko.spark.common.http.Bytebin;
import me.lucko.spark.sampler.Sampler;
import me.lucko.spark.sampler.SamplerBuilder;
import me.lucko.spark.sampler.ThreadDumper;
import me.lucko.spark.sampler.ThreadGrouper;
import me.lucko.spark.sampler.TickCounter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class SamplerModule<S> implements CommandModule<S> {

    /** Guards {@link #activeSampler} */
    private final Object[] activeSamplerMutex = new Object[0];
    /** The WarmRoast instance currently running, if any */
    private Sampler activeSampler = null;

    @Override
    public void registerCommands(Consumer<Command<S>> consumer) {
        consumer.accept(Command.<S>builder()
                .aliases("start")
                .argumentUsage("timeout", "timeout seconds")
                .argumentUsage("thread", "thread name")
                .argumentUsage("not-combined", null)
                .argumentUsage("interval", "interval millis")
                .argumentUsage("only-ticks-over", "tick length millis")
                .argumentUsage("include-line-numbers", null)
                .executor((platform, sender, arguments) -> {
                    int timeoutSeconds = arguments.intFlag("timeout");
                    if (timeoutSeconds != -1 && timeoutSeconds <= 10) {
                        platform.sendPrefixedMessage(sender, "&cThe specified timeout is not long enough for accurate results to be formed. Please choose a value greater than 10.");
                        return;
                    }

                    if (timeoutSeconds != -1 && timeoutSeconds < 30) {
                        platform.sendPrefixedMessage(sender, "&7The accuracy of the output will significantly improve when sampling is able to run for longer periods. Consider setting a timeout value over 30 seconds.");
                    }

                    int intervalMillis = arguments.intFlag("interval");
                    if (intervalMillis <= 0) {
                        intervalMillis = 4;
                    }

                    boolean includeLineNumbers = arguments.boolFlag("include-line-numbers");

                    Set<String> threads = arguments.stringFlag("thread");
                    ThreadDumper threadDumper;
                    if (threads.isEmpty()) {
                        // use the server thread
                        threadDumper = platform.getDefaultThreadDumper();
                    } else if (threads.contains("*")) {
                        threadDumper = ThreadDumper.ALL;
                    } else {
                        threadDumper = new ThreadDumper.Specific(threads);
                    }

                    ThreadGrouper threadGrouper;
                    if (arguments.boolFlag("not-combined")) {
                        threadGrouper = ThreadGrouper.BY_NAME;
                    } else {
                        threadGrouper = ThreadGrouper.BY_POOL;
                    }

                    int ticksOver = arguments.intFlag("only-ticks-over");
                    TickCounter tickCounter = null;
                    if (ticksOver != -1) {
                        try {
                            tickCounter = platform.newTickCounter();
                        } catch (UnsupportedOperationException e) {
                            platform.sendPrefixedMessage(sender, "&cTick counting is not supported!");
                            return;
                        }
                    }

                    Sampler sampler;
                    synchronized (this.activeSamplerMutex) {
                        if (this.activeSampler != null) {
                            platform.sendPrefixedMessage(sender, "&7An active sampler is already running.");
                            return;
                        }

                        platform.sendPrefixedMessage("&7Initializing a new profiler, please wait...");

                        SamplerBuilder builder = new SamplerBuilder();
                        builder.threadDumper(threadDumper);
                        builder.threadGrouper(threadGrouper);
                        if (timeoutSeconds != -1) {
                            builder.completeAfter(timeoutSeconds, TimeUnit.SECONDS);
                        }
                        builder.samplingInterval(intervalMillis);
                        builder.includeLineNumbers(includeLineNumbers);
                        if (ticksOver != -1) {
                            builder.ticksOver(ticksOver, tickCounter);
                        }
                        sampler = this.activeSampler = builder.start();

                        platform.sendPrefixedMessage("&bProfiler now active!");
                        if (timeoutSeconds == -1) {
                            platform.sendPrefixedMessage("&7Use '/" + platform.getLabel() + " stop' to stop profiling and upload the results.");
                        } else {
                            platform.sendPrefixedMessage("&7The results will be automatically returned after the profiler has been running for " + timeoutSeconds + " seconds.");
                        }
                    }

                    CompletableFuture<Sampler> future = sampler.getFuture();

                    // send message if profiling fails
                    future.whenCompleteAsync((s, throwable) -> {
                        if (throwable != null) {
                            platform.sendPrefixedMessage("&cSampling operation failed unexpectedly. Error: " + throwable.toString());
                            throwable.printStackTrace();
                        }
                    });

                    // set activeSampler to null when complete.
                    future.whenCompleteAsync((s, throwable) -> {
                        synchronized (this.activeSamplerMutex) {
                            if (sampler == this.activeSampler) {
                                this.activeSampler = null;
                            }
                        }
                    });

                    // await the result
                    if (timeoutSeconds != -1) {
                        future.thenAcceptAsync(s -> {
                            platform.sendPrefixedMessage("&7The active sampling operation has completed! Uploading results...");
                            handleUpload(platform, s);
                        });
                    }
                })
                .tabCompleter((platform, sender, arguments) -> {
                    List<String> opts = new ArrayList<>(Arrays.asList("--timeout", "--interval",
                            "--not-combined", "--only-ticks-over", "--include-line-numbers"));
                    opts.removeAll(arguments);
                    opts.add("--thread"); // allowed multiple times

                    return TabCompleter.create()
                            .from(0, CompletionSupplier.startsWith(opts))
                            .complete(arguments);
                })
                .build()
        );

        consumer.accept(Command.<S>builder()
                .aliases("info")
                .executor((platform, sender, arguments) -> {
                    synchronized (this.activeSamplerMutex) {
                        if (this.activeSampler == null) {
                            platform.sendPrefixedMessage(sender, "&7There isn't an active sampling task running.");
                        } else {
                            long timeout = this.activeSampler.getEndTime();
                            if (timeout == -1) {
                                platform.sendPrefixedMessage(sender, "&7There is an active sampler currently running, with no defined timeout.");
                            } else {
                                long timeoutDiff = (timeout - System.currentTimeMillis()) / 1000L;
                                platform.sendPrefixedMessage(sender, "&7There is an active sampler currently running, due to timeout in " + timeoutDiff + " seconds.");
                            }

                            long runningTime = (System.currentTimeMillis() - this.activeSampler.getStartTime()) / 1000L;
                            platform.sendPrefixedMessage(sender, "&7It has been sampling for " + runningTime + " seconds so far.");
                        }
                    }
                })
                .build()
        );

        consumer.accept(Command.<S>builder()
                .aliases("stop", "upload", "paste")
                .executor((platform, sender, arguments) -> {
                    synchronized (this.activeSamplerMutex) {
                        if (this.activeSampler == null) {
                            platform.sendPrefixedMessage(sender, "&7There isn't an active sampling task running.");
                        } else {
                            this.activeSampler.cancel();
                            platform.sendPrefixedMessage("&7The active sampling operation has been stopped! Uploading results...");
                            handleUpload(platform, this.activeSampler);
                            this.activeSampler = null;
                        }
                    }
                })
                .build()
        );

        consumer.accept(Command.<S>builder()
                .aliases("cancel")
                .executor((platform, sender, arguments) -> {
                    synchronized (this.activeSamplerMutex) {
                        if (this.activeSampler == null) {
                            platform.sendPrefixedMessage(sender, "&7There isn't an active sampling task running.");
                        } else {
                            this.activeSampler.cancel();
                            this.activeSampler = null;
                            platform.sendPrefixedMessage("&bThe active sampling task has been cancelled.");
                        }
                    }
                })
                .build()
        );
    }

    private void handleUpload(SparkPlatform<S> platform, Sampler sampler) {
        platform.runAsync(() -> {
            byte[] output = sampler.formCompressedDataPayload();
            try {
                String pasteId = Bytebin.postCompressedContent(output);
                platform.sendPrefixedMessage("&bSampling results:");
                platform.sendLink(SparkPlatform.VIEWER_URL + pasteId);
            } catch (IOException e) {
                platform.sendPrefixedMessage("&cAn error occurred whilst uploading the results.");
                e.printStackTrace();
            }
        });
    }
}

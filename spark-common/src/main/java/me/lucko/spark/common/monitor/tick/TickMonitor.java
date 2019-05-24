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

package me.lucko.spark.common.monitor.tick;

import com.sun.management.GarbageCollectionNotificationInfo;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.monitor.memory.GarbageCollectionMonitor;
import me.lucko.spark.common.sampler.TickCounter;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;

import java.text.DecimalFormat;
import java.util.DoubleSummaryStatistics;

public abstract class TickMonitor implements TickCounter.TickTask, GarbageCollectionMonitor.Listener, AutoCloseable {
    private static final DecimalFormat df = new DecimalFormat("#.##");

    private final SparkPlatform platform;
    private final TickCounter tickCounter;
    private final int zeroTick;
    private final GarbageCollectionMonitor garbageCollectionMonitor;
    private final int percentageChangeThreshold;

    // data
    private volatile double lastTickTime = 0;
    private State state = null;
    private final DoubleSummaryStatistics averageTickTime = new DoubleSummaryStatistics();
    private double avg;

    public TickMonitor(SparkPlatform platform, TickCounter tickCounter, int percentageChangeThreshold, boolean monitorGc) {
        this.platform = platform;
        this.tickCounter = tickCounter;
        this.zeroTick = tickCounter.getCurrentTick();
        this.percentageChangeThreshold = percentageChangeThreshold;

        if (monitorGc) {
            this.garbageCollectionMonitor =  new GarbageCollectionMonitor();
            this.garbageCollectionMonitor.addListener(this);
        } else {
            this.garbageCollectionMonitor = null;
        }
    }

    public int getCurrentTick() {
        return this.tickCounter.getCurrentTick() - this.zeroTick;
    }

    protected abstract void sendMessage(Component message);

    @Override
    public void close() {
        if (this.garbageCollectionMonitor != null) {
            this.garbageCollectionMonitor.close();
        }
    }

    @Override
    public void onTick(TickCounter counter) {
        double now = ((double) System.nanoTime()) / 1000000d;

        // init
        if (this.state == null) {
            this.state = State.SETUP;
            this.lastTickTime = now;
            sendMessage(TextComponent.of("Tick monitor started. Before the monitor becomes fully active, the server's " +
                    "average tick rate will be calculated over a period of 120 ticks (approx 6 seconds)."));
            return;
        }

        // find the diff
        double last = this.lastTickTime;
        double diff = now - last;
        boolean ignore = last == 0;
        this.lastTickTime = now;

        if (ignore) {
            return;
        }

        // form averages
        if (this.state == State.SETUP) {
            this.averageTickTime.accept(diff);

            // move onto the next state
            if (this.averageTickTime.getCount() >= 120) {
                this.platform.getPlugin().runAsync(() -> {
                    sendMessage(TextComponent.of("Analysis is now complete.", TextColor.GOLD));
                    sendMessage(TextComponent.builder("").color(TextColor.GRAY)
                            .append(TextComponent.of(">", TextColor.WHITE))
                            .append(TextComponent.space())
                            .append(TextComponent.of("Max: "))
                            .append(TextComponent.of(df.format(this.averageTickTime.getMax())))
                            .append(TextComponent.of("ms"))
                            .build()
                    );
                    sendMessage(TextComponent.builder("").color(TextColor.GRAY)
                            .append(TextComponent.of(">", TextColor.WHITE))
                            .append(TextComponent.space())
                            .append(TextComponent.of("Min: "))
                            .append(TextComponent.of(df.format(this.averageTickTime.getMin())))
                            .append(TextComponent.of("ms"))
                            .build()
                    );
                    sendMessage(TextComponent.builder("").color(TextColor.GRAY)
                            .append(TextComponent.of(">", TextColor.WHITE))
                            .append(TextComponent.space())
                            .append(TextComponent.of("Avg: "))
                            .append(TextComponent.of(df.format(this.averageTickTime.getAverage())))
                            .append(TextComponent.of("ms"))
                            .build()
                    );
                    sendMessage(TextComponent.of("Starting now, any ticks with >" + this.percentageChangeThreshold + "% increase in " +
                            "duration compared to the average will be reported."));
                });

                this.avg = this.averageTickTime.getAverage();
                this.state = State.MONITORING;
            }
        }

        if (this.state == State.MONITORING) {
            double increase = diff - this.avg;
            if (increase <= 0) {
                return;
            }

            double percentageChange = (increase * 100d) / this.avg;
            if (percentageChange > this.percentageChangeThreshold) {
                this.platform.getPlugin().runAsync(() -> {
                    sendMessage(TextComponent.builder("").color(TextColor.GRAY)
                            .append(TextComponent.of("Tick "))
                            .append(TextComponent.of("#" + getCurrentTick(), TextColor.DARK_GRAY))
                            .append(TextComponent.of(" lasted "))
                            .append(TextComponent.of(df.format(diff), TextColor.GOLD))
                            .append(TextComponent.of(" ms. "))
                            .append(TextComponent.of("("))
                            .append(TextComponent.of(df.format(percentageChange) + "%", TextColor.GOLD))
                            .append(TextComponent.of(" increase from avg)"))
                            .build()
                    );
                });
            }
        }
    }

    @Override
    public void onGc(GarbageCollectionNotificationInfo data) {
        if (this.state == State.SETUP) {
            // set lastTickTime to zero so this tick won't be counted in the average
            this.lastTickTime = 0;
            return;
        }

        String gcType;
        if (data.getGcAction().equals("end of minor GC")) {
            gcType = "Young Gen GC";
        } else if (data.getGcAction().equals("end of major GC")) {
            gcType = "Old Gen GC";
        } else {
            gcType = data.getGcAction();
        }

        this.platform.getPlugin().runAsync(() -> {
            sendMessage(TextComponent.builder("").color(TextColor.GRAY)
                    .append(TextComponent.of("Tick "))
                    .append(TextComponent.of("#" + getCurrentTick(), TextColor.DARK_GRAY))
                    .append(TextComponent.of(" included "))
                    .append(TextComponent.of("GC", TextColor.RED))
                    .append(TextComponent.of(" lasting "))
                    .append(TextComponent.of(df.format(data.getGcInfo().getDuration()), TextColor.GOLD))
                    .append(TextComponent.of(" ms. (type = " + gcType + ")"))
                    .build()
            );
        });
    }

    private enum State {
        SETUP,
        MONITORING
    }
}

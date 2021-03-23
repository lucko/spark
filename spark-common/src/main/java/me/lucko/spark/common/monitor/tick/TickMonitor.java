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
import me.lucko.spark.common.tick.TickHook;

import net.kyori.adventure.text.Component;

import java.text.DecimalFormat;
import java.util.DoubleSummaryStatistics;

import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.GOLD;
import static net.kyori.adventure.text.format.NamedTextColor.GRAY;
import static net.kyori.adventure.text.format.NamedTextColor.RED;
import static net.kyori.adventure.text.format.NamedTextColor.WHITE;

/**
 * Monitoring process for the server/client tick rate.
 */
public abstract class TickMonitor implements TickHook.Callback, GarbageCollectionMonitor.Listener, AutoCloseable {
    private static final DecimalFormat DF = new DecimalFormat("#.##");

    /** The spark platform */
    private final SparkPlatform platform;
    /** The tick hook being used as the source for tick information. */
    private final TickHook tickHook;
    /** The index of the tick when the monitor first started */
    private final int zeroTick;
    /** The active garbage collection monitor, if enabled */
    private final GarbageCollectionMonitor garbageCollectionMonitor;
    /** The predicate used to decide if a tick should be reported. */
    private final ReportPredicate reportPredicate;

    /**
     * Enum representing the various phases in a tick monitors lifetime.
     */
    private enum Phase {
        /** Tick monitor is in the setup phase where it determines the average tick rate. */
        SETUP,
        /** Tick monitor is in the monitoring phase where it listens for ticks that exceed the threshold. */
        MONITORING
    }

    /** The phase the monitor is in */
    private Phase phase = null;
    /** Gets the system timestamp of the last recorded tick */
    private volatile double lastTickTime = 0;
    /** Used to calculate the average tick time during the SETUP phase. */
    private final DoubleSummaryStatistics averageTickTimeCalc = new DoubleSummaryStatistics();
    /** The average tick time, defined at the end of the SETUP phase. */
    private double averageTickTime;

    public TickMonitor(SparkPlatform platform, TickHook tickHook, ReportPredicate reportPredicate, boolean monitorGc) {
        this.platform = platform;
        this.tickHook = tickHook;
        this.zeroTick = tickHook.getCurrentTick();
        this.reportPredicate = reportPredicate;

        if (monitorGc) {
            this.garbageCollectionMonitor =  new GarbageCollectionMonitor();
            this.garbageCollectionMonitor.addListener(this);
        } else {
            this.garbageCollectionMonitor = null;
        }
    }

    public int getCurrentTick() {
        return this.tickHook.getCurrentTick() - this.zeroTick;
    }

    protected abstract void sendMessage(Component message);

    public void start() {
        this.tickHook.addCallback(this);
    }

    @Override
    public void close() {
        this.tickHook.removeCallback(this);

        if (this.garbageCollectionMonitor != null) {
            this.garbageCollectionMonitor.close();
        }
    }

    @Override
    public void onTick(int currentTick) {
        double now = ((double) System.nanoTime()) / 1000000d;

        // init
        if (this.phase == null) {
            this.phase = Phase.SETUP;
            this.lastTickTime = now;
            sendMessage(text("Tick monitor started. Before the monitor becomes fully active, the server's " +
                    "average tick rate will be calculated over a period of 120 ticks (approx 6 seconds)."));
            return;
        }

        // find the diff
        double last = this.lastTickTime;
        double tickDuration = now - last;
        this.lastTickTime = now;

        if (last == 0) {
            return;
        }

        // form averages
        if (this.phase == Phase.SETUP) {
            this.averageTickTimeCalc.accept(tickDuration);

            // move onto the next state
            if (this.averageTickTimeCalc.getCount() >= 120) {
                this.platform.getPlugin().executeAsync(() -> {
                    sendMessage(text("Analysis is now complete.", GOLD));
                    sendMessage(text()
                            .color(GRAY)
                            .append(text(">", WHITE))
                            .append(space())
                            .append(text("Max: "))
                            .append(text(DF.format(this.averageTickTimeCalc.getMax())))
                            .append(text("ms"))
                            .build()
                    );
                    sendMessage(text()
                            .color(GRAY)
                            .append(text(">", WHITE))
                            .append(space())
                            .append(text("Min: "))
                            .append(text(DF.format(this.averageTickTimeCalc.getMin())))
                            .append(text("ms"))
                            .build()
                    );
                    sendMessage(text()
                            .color(GRAY)
                            .append(text(">", WHITE))
                            .append(space())
                            .append(text("Average: "))
                            .append(text(DF.format(this.averageTickTimeCalc.getAverage())))
                            .append(text("ms"))
                            .build()
                    );
                    sendMessage(this.reportPredicate.monitoringStartMessage());
                });

                this.averageTickTime = this.averageTickTimeCalc.getAverage();
                this.phase = Phase.MONITORING;
            }
        }

        if (this.phase == Phase.MONITORING) {
            double increase = tickDuration - this.averageTickTime;
            double percentageChange = (increase * 100d) / this.averageTickTime;
            if (this.reportPredicate.shouldReport(tickDuration, increase, percentageChange)) {
                this.platform.getPlugin().executeAsync(() -> {
                    sendMessage(text()
                            .color(GRAY)
                            .append(text("Tick "))
                            .append(text("#" + getCurrentTick(), DARK_GRAY))
                            .append(text(" lasted "))
                            .append(text(DF.format(tickDuration), GOLD))
                            .append(text(" ms. "))
                            .append(text("("))
                            .append(text(DF.format(percentageChange) + "%", GOLD))
                            .append(text(" increase from avg)"))
                            .build()
                    );
                });
            }
        }
    }

    @Override
    public void onGc(GarbageCollectionNotificationInfo data) {
        if (this.phase == Phase.SETUP) {
            // set lastTickTime to zero so this tick won't be counted in the average
            this.lastTickTime = 0;
            return;
        }

        this.platform.getPlugin().executeAsync(() -> {
            sendMessage(text()
                    .color(GRAY)
                    .append(text("Tick "))
                    .append(text("#" + getCurrentTick(), DARK_GRAY))
                    .append(text(" included "))
                    .append(text("GC", RED))
                    .append(text(" lasting "))
                    .append(text(DF.format(data.getGcInfo().getDuration()), GOLD))
                    .append(text(" ms. (type = " + GarbageCollectionMonitor.getGcType(data) + ")"))
                    .build()
            );
        });
    }

}

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

package me.lucko.spark.common;

import me.lucko.spark.profiler.TickCounter;

import java.text.DecimalFormat;
import java.util.DoubleSummaryStatistics;

public abstract class TickMonitor implements Runnable {
    private static final DecimalFormat df = new DecimalFormat("#.##");

    private final TickCounter tickCounter;
    private final int percentageChangeThreshold;

    // data
    private double lastTickTime = 0;
    private State state = null;
    private DoubleSummaryStatistics averageTickTime = new DoubleSummaryStatistics();
    private double avg;

    public TickMonitor(TickCounter tickCounter, int percentageChangeThreshold) {
        this.tickCounter = tickCounter;
        this.percentageChangeThreshold = percentageChangeThreshold;

        this.tickCounter.start();
        this.tickCounter.addTickTask(this);
    }

    protected abstract void sendMessage(String message);

    public void close() {
        this.tickCounter.close();
    }

    @Override
    public void run() {
        double now = ((double) System.nanoTime()) / 1000000d;

        // init
        if (this.state == null) {
            this.state = State.SETUP;
            this.lastTickTime = now;
            sendMessage("Tick monitor started. Before the monitor becomes fully active, the server's " +
                    "average tick rate will be calculated over a period of 120 ticks (approx 6 seconds).");
            return;
        }

        // find the diff
        double diff = now - this.lastTickTime;
        this.lastTickTime = now;

        // form averages
        if (this.state == State.SETUP) {
            this.averageTickTime.accept(diff);

            // move onto the next state
            if (this.averageTickTime.getCount() >= 120) {

                sendMessage("&bAnalysis is now complete.");
                sendMessage("&f> &7Max: " + df.format(this.averageTickTime.getMax()) + "ms");
                sendMessage("&f> &7Min: " + df.format(this.averageTickTime.getMin()) + "ms");
                sendMessage("&f> &7Avg: " + df.format(this.averageTickTime.getAverage()) + "ms");
                sendMessage("Starting now, any ticks with >" + this.percentageChangeThreshold + "% increase in " +
                        "duration compared to the average will be reported.");

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
                sendMessage("&7Tick &8#" + this.tickCounter.getCurrentTick() + " &7lasted &b" + df.format(diff) + "&7 milliseconds. " +
                        "&7(&b" + df.format(percentageChange) + "% &7increase from average)");
            }
        }
    }

    private enum State {
        SETUP,
        MONITORING
    }
}

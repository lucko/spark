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

import me.lucko.spark.common.sampler.TickCounter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

/**
 * Calculates the servers TPS (ticks per second) rate.
 *
 * <p>The code use to calculate the TPS is the same as the code used by the Minecraft server itself.
 * This means that this class will output values the same as the /tps command.</p>
 *
 * <p>We calculate our own values instead of pulling them from the server for two reasons. Firstly,
 * it's easier - pulling from the server requires reflection code on each of the platforms, we'd
 * rather avoid that. Secondly, it allows us to generate rolling averages over a shorter period of
 * time.</p>
 */
public class TpsCalculator implements TickCounter.TickTask {

    private static final long SEC_IN_NANO = TimeUnit.SECONDS.toNanos(1);
    private static final int TPS = 20;
    private static final int SAMPLE_INTERVAL = 20;
    private static final BigDecimal TPS_BASE = new BigDecimal(SEC_IN_NANO).multiply(new BigDecimal(SAMPLE_INTERVAL));

    private final TpsRollingAverage avg5Sec = new TpsRollingAverage(5);
    private final TpsRollingAverage avg10Sec = new TpsRollingAverage(10);
    private final TpsRollingAverage avg1Min = new TpsRollingAverage(60);
    private final TpsRollingAverage avg5Min = new TpsRollingAverage(60 * 5);
    private final TpsRollingAverage avg15Min = new TpsRollingAverage(60 * 15);

    private final TpsRollingAverage[] averages = new TpsRollingAverage[]{
            this.avg5Sec, this.avg10Sec, this.avg1Min, this.avg5Min, this.avg15Min
    };

    private long last = 0;

    @Override
    public void onTick(TickCounter counter) {
        if (counter.getCurrentTick() % SAMPLE_INTERVAL != 0) {
            return;
        }

        long now = System.nanoTime();

        if (this.last == 0) {
            this.last = now;
            return;
        }

        long diff = now - this.last;
        BigDecimal currentTps = TPS_BASE.divide(new BigDecimal(diff), 30, RoundingMode.HALF_UP);
        BigDecimal total = currentTps.multiply(new BigDecimal(diff));

        for (TpsRollingAverage rollingAverage : this.averages) {
            rollingAverage.add(currentTps, diff, total);
        }

        this.last = now;
    }

    public double avg5Sec() {
        return this.avg5Sec.getAverage();
    }

    public double avg10Sec() {
        return this.avg10Sec.getAverage();
    }

    public double avg1Min() {
        return this.avg1Min.getAverage();
    }

    public double avg5Min() {
        return this.avg5Min.getAverage();
    }

    public double avg15Min() {
        return this.avg15Min.getAverage();
    }



    /**
     * Rolling average calculator taken.
     *
     * <p>This code is taken from PaperMC/Paper, licensed under MIT.</p>
     *
     * @author aikar (PaperMC) https://github.com/PaperMC/Paper/blob/master/Spigot-Server-Patches/0021-Further-improve-server-tick-loop.patch
     */
    public static final class TpsRollingAverage {
        private final int size;
        private long time;
        private BigDecimal total;
        private int index = 0;
        private final BigDecimal[] samples;
        private final long[] times;

        TpsRollingAverage(int size) {
            this.size = size;
            this.time = size * SEC_IN_NANO;
            this.total = new BigDecimal(TPS).multiply(new BigDecimal(SEC_IN_NANO)).multiply(new BigDecimal(size));
            this.samples = new BigDecimal[size];
            this.times = new long[size];
            for (int i = 0; i < size; i++) {
                this.samples[i] = new BigDecimal(TPS);
                this.times[i] = SEC_IN_NANO;
            }
        }

        public void add(BigDecimal x, long t, BigDecimal total) {
            this.time -= this.times[this.index];
            this.total = this.total.subtract(this.samples[this.index].multiply(new BigDecimal(this.times[this.index])));
            this.samples[this.index] = x;
            this.times[this.index] = t;
            this.time += t;
            this.total = this.total.add(total);
            if (++this.index == this.size) {
                this.index = 0;
            }
        }

        public double getAverage() {
            return this.total.divide(new BigDecimal(this.time), 30, RoundingMode.HALF_UP).doubleValue();
        }
    }

}

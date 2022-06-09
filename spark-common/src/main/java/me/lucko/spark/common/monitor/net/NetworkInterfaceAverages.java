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

package me.lucko.spark.common.monitor.net;

import me.lucko.spark.common.util.RollingAverage;

import java.math.BigDecimal;

public final class NetworkInterfaceAverages {
    private final RollingAverage rxBytesPerSecond;
    private final RollingAverage txBytesPerSecond;
    private final RollingAverage rxPacketsPerSecond;
    private final RollingAverage txPacketsPerSecond;

    NetworkInterfaceAverages(int windowSize) {
        this.rxBytesPerSecond = new RollingAverage(windowSize);
        this.txBytesPerSecond = new RollingAverage(windowSize);
        this.rxPacketsPerSecond = new RollingAverage(windowSize);
        this.txPacketsPerSecond = new RollingAverage(windowSize);
    }

    void accept(NetworkInterfaceInfo info, RateCalculator rateCalculator) {
        this.rxBytesPerSecond.add(rateCalculator.calculate(info.getReceivedBytes()));
        this.txBytesPerSecond.add(rateCalculator.calculate(info.getTransmittedBytes()));
        this.rxPacketsPerSecond.add(rateCalculator.calculate(info.getReceivedPackets()));
        this.txPacketsPerSecond.add(rateCalculator.calculate(info.getTransmittedPackets()));
    }

    interface RateCalculator {
        BigDecimal calculate(long value);
    }

    public RollingAverage bytesPerSecond(Direction direction) {
        switch (direction) {
            case RECEIVE:
                return rxBytesPerSecond();
            case TRANSMIT:
                return txBytesPerSecond();
            default:
                throw new AssertionError();
        }
    }

    public RollingAverage packetsPerSecond(Direction direction) {
        switch (direction) {
            case RECEIVE:
                return rxPacketsPerSecond();
            case TRANSMIT:
                return txPacketsPerSecond();
            default:
                throw new AssertionError();
        }
    }

    public RollingAverage rxBytesPerSecond() {
        return this.rxBytesPerSecond;
    }

    public RollingAverage rxPacketsPerSecond() {
        return this.rxPacketsPerSecond;
    }

    public RollingAverage txBytesPerSecond() {
        return this.txBytesPerSecond;
    }

    public RollingAverage txPacketsPerSecond() {
        return this.txPacketsPerSecond;
    }
}

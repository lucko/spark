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

import net.kyori.adventure.text.Component;

/**
 * A predicate to test whether a tick should be reported.
 */
public interface ReportPredicate {

    /**
     * Tests whether a tick should be reported.
     *
     * @param duration         the tick duration
     * @param increaseFromAvg  the difference between the ticks duration and the average
     * @param percentageChange the percentage change between the ticks duration and the average
     * @return true if the tick should be reported, false otherwise
     */
    boolean shouldReport(double duration, double increaseFromAvg, double percentageChange);

    /**
     * Gets a component to describe how the predicate will select ticks to report.
     *
     * @return the component
     */
    Component monitoringStartMessage();

    final class PercentageChangeGt implements ReportPredicate {
        private final double threshold;

        public PercentageChangeGt(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public boolean shouldReport(double duration, double increaseFromAvg, double percentageChange) {
            if (increaseFromAvg <= 0) {
                return false;
            }
            return percentageChange > this.threshold;
        }

        @Override
        public Component monitoringStartMessage() {
            return Component.text("Starting now, any ticks with >" + this.threshold + "% increase in " +
                    "duration compared to the average will be reported.");
        }
    }

    final class DurationGt implements ReportPredicate {
        private final double threshold;

        public DurationGt(double threshold) {
            this.threshold = threshold;
        }

        @Override
        public boolean shouldReport(double duration, double increaseFromAvg, double percentageChange) {
            if (increaseFromAvg <= 0) {
                return false;
            }
            return duration > this.threshold;
        }

        @Override
        public Component monitoringStartMessage() {
            return Component.text("Starting now, any ticks with duration >" + this.threshold + " will be reported.");
        }
    }

}

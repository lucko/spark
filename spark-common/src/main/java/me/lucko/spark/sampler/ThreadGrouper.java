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

package me.lucko.spark.sampler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Function for grouping threads together
 */
@FunctionalInterface
public interface ThreadGrouper {

    /**
     * Gets the group for the given thread.
     *
     * @param threadName the name of the thread
     * @return the group
     */
    String getGroup(String threadName);

    /**
     * Implementation of {@link ThreadGrouper} that just groups by thread name.
     */
    ThreadGrouper BY_NAME = threadName -> threadName;

    /**
     * Implementation of {@link ThreadGrouper} that attempts to group by the name of the pool
     * the thread originated from.
     *
     * <p>The regex pattern used to match pools expects a digit at the end of the thread name,
     * separated from the pool name with any of one or more of ' ', '-', or '#'.</p>
     */
    ThreadGrouper BY_POOL = new ThreadGrouper() {
        private final Pattern pattern = Pattern.compile("^(.*?)[-# ]+\\d+$");

        @Override
        public String getGroup(String threadName) {
            Matcher matcher = this.pattern.matcher(threadName);
            if (!matcher.matches()) {
                return threadName;
            }

            return matcher.group(1).trim() + " (Combined)";
        }
    };

    /**
     * Implementation of {@link ThreadGrouper} which groups all threads as one, under
     * the name "All".
     */
    ThreadGrouper AS_ONE = threadName -> "All";

}

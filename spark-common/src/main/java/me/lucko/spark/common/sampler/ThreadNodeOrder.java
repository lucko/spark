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

package me.lucko.spark.common.sampler;

import me.lucko.spark.common.sampler.node.ThreadNode;

import java.util.Comparator;
import java.util.Map;

/**
 * Methods of ordering {@link ThreadNode}s in the output data.
 */
public enum ThreadNodeOrder implements Comparator<Map.Entry<String, ThreadNode>> {

    /**
     * Order by the name of the thread (alphabetically)
     */
    BY_NAME {
        @Override
        public int compare(Map.Entry<String, ThreadNode> o1, Map.Entry<String, ThreadNode> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    },

    /**
     * Order by the time taken by the thread (most time taken first)
     */
    BY_TIME {
        @Override
        public int compare(Map.Entry<String, ThreadNode> o1, Map.Entry<String, ThreadNode> o2) {
            return -Double.compare(o1.getValue().getTotalTime(), o2.getValue().getTotalTime());
        }
    }

}

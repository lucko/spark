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

package me.lucko.spark.common.platform.world;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A map of (key) -> count.
 *
 * @param <T> the key type
 */
public interface CountMap<T> {

    /**
     * Increment the counter for the given key
     *
     * @param key the key
     */
    void increment(T key);

    /**
     * Add to the counter for the given key
     *
     * @param key the key
     */
    void add(T key, int delta);

    AtomicInteger total();

    Map<T, AtomicInteger> asMap();

    /**
     * A simple {@link CountMap} backed by the provided {@link Map}
     *
     * @param <T> the key type
     */
    class Simple<T> implements CountMap<T> {
        private final Map<T, AtomicInteger> counts;
        private final AtomicInteger total;

        public Simple(Map<T, AtomicInteger> counts) {
            this.counts = counts;
            this.total = new AtomicInteger();
        }

        @Override
        public void increment(T key) {
            AtomicInteger counter = this.counts.get(key);
            if (counter == null) {
                counter = new AtomicInteger();
                this.counts.put(key, counter);
            }
            counter.incrementAndGet();
            this.total.incrementAndGet();
        }

        @Override
        public void add(T key, int delta) {
            AtomicInteger counter = this.counts.get(key);
            if (counter == null) {
                counter = new AtomicInteger();
                this.counts.put(key, counter);
            }
            counter.addAndGet(delta);
            this.total.addAndGet(delta);
        }

        @Override
        public AtomicInteger total() {
            return this.total;
        }

        @Override
        public Map<T, AtomicInteger> asMap() {
            return this.counts;
        }
    }

    /**
     * A {@link CountMap} backed by an {@link EnumMap}.
     *
     * @param <T> the key type - must be an enum
     */
    class EnumKeyed<T extends Enum<T>> extends Simple<T> {
        public EnumKeyed(Class<T> keyClass) {
            super(new EnumMap<>(keyClass));
        }
    }

}

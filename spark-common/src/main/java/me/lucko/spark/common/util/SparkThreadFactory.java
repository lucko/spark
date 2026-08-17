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

package me.lucko.spark.common.util;

import me.lucko.spark.common.util.log.SparkStaticLogger;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class SparkThreadFactory implements ThreadFactory {

    static final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = (t, e) -> {
        SparkStaticLogger.log(Level.SEVERE, "Uncaught exception thrown in thread " + t.getName(), e);
    };

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;
    private final boolean daemon;

    public SparkThreadFactory() {
        this("spark-worker-pool", true);
    }

    public SparkThreadFactory(String prefix, boolean daemon) {
        this.namePrefix = prefix + "-" +
                poolNumber.getAndIncrement() +
                "-thread-";
        this.daemon = daemon;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, this.namePrefix + this.threadNumber.getAndIncrement());
        t.setUncaughtExceptionHandler(EXCEPTION_HANDLER);
        t.setDaemon(this.daemon);
        return t;
    }

}

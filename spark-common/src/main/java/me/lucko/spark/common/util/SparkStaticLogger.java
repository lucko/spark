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

import java.util.logging.Level;

/**
 * Special logger for use by classes that don't easily have access to a
 * {@link me.lucko.spark.common.SparkPlatform} instance.
 *
 * <p>This avoids warnings on platforms like Paper that get upset if plugins use
 * {@link System#out} or {@link System#err}.</p>
 */
public enum SparkStaticLogger {
    ;

    private static Logger logger = null;

    public synchronized static void setLogger(Logger logger) {
        if (SparkStaticLogger.logger == null) {
            SparkStaticLogger.logger = logger;
        }
    }

    public static void log(Level level, String msg) {
        Logger logger = SparkStaticLogger.logger;
        if (logger == null) {
            if (level.intValue() >= 1000) {
                System.err.println(msg);
            } else {
                System.out.println(msg);
            }
            return;
        }

        logger.log(level, msg);
    }

    public interface Logger {
        void log(Level level, String msg);
    }

}

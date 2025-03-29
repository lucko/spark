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

package me.lucko.spark.common.util.log;

import java.util.logging.Level;

public interface Logger {

    /**
     * Print to the plugin logger.
     *
     * @param level the log level
     * @param msg the message
     */
    void log(Level level, String msg);

    /**
     * Print to the plugin logger.
     *
     * @param level the log level
     * @param msg the message
     * @param throwable the throwable
     */
    void log(Level level, String msg, Throwable throwable);

    /**
     * A fallback logger
     */
    Logger FALLBACK = new Logger() {
        @Override
        public void log(Level level, String msg) {
            if (level.intValue() >= 1000) {
                System.err.println(msg);
            } else {
                System.out.println(msg);
            }
        }

        @Override
        public void log(Level level, String msg, Throwable throwable) {
            if (isSevere(level)) {
                System.err.println(msg);
                if (throwable != null) {
                    throwable.printStackTrace(System.err);
                }
            } else {
                System.out.println(msg);
                if (throwable != null) {
                    throwable.printStackTrace(System.out);
                }
            }
        }
    };

    static boolean isSevere(Level level) {
        return level.intValue() >= 1000;
    }

    static boolean isWarning(Level level) {
        return level.intValue() >= 900;
    }
}

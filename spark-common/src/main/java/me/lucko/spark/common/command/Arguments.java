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

package me.lucko.spark.common.command;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for parsing command-flag like arguments from raw space split strings.
 */
public class Arguments {
    private static final Pattern FLAG_REGEX = Pattern.compile("^--(.+)$");

    private final List<String> rawArgs;
    private final SetMultimap<String, String> parsedArgs;
    private String parsedSubCommand = null;

    public Arguments(List<String> rawArgs, boolean allowSubCommand) {
        this.rawArgs = rawArgs;
        this.parsedArgs = HashMultimap.create();

        String flag = null;
        List<String> value = null;

        for (int i = 0; i < this.rawArgs.size(); i++) {
            String arg = this.rawArgs.get(i);

            Matcher matcher = FLAG_REGEX.matcher(arg);
            boolean matches = matcher.matches();

            if (i == 0 && allowSubCommand && !matches) {
                this.parsedSubCommand = arg;
            } else if (flag == null || matches) {
                if (!matches) {
                    throw new ParseException("Expected flag at position " + i + " but got '" + arg + "' instead!");
                }

                // store existing value, if present
                if (flag != null) {
                    this.parsedArgs.put(flag, String.join(" ", value));
                }

                flag = matcher.group(1).toLowerCase();
                value = new ArrayList<>();
            } else {
                // part of a value
                value.add(arg);
            }
        }

        // store remaining value, if present
        if (flag != null) {
            this.parsedArgs.put(flag, String.join(" ", value));
        }
    }

    public List<String> raw() {
        return this.rawArgs;
    }

    public String subCommand() {
        return this.parsedSubCommand;
    }

    public int intFlag(String key) {
        Iterator<String> it = this.parsedArgs.get(key).iterator();
        if (it.hasNext()) {
            try {
                return Math.abs(Integer.parseInt(it.next()));
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid input for '" + key + "' argument. Please specify a number!");
            }
        }
        return -1; // undefined
    }

    public double doubleFlag(String key) {
        Iterator<String> it = this.parsedArgs.get(key).iterator();
        if (it.hasNext()) {
            try {
                return Math.abs(Double.parseDouble(it.next()));
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid input for '" + key + "' argument. Please specify a number!");
            }
        }
        return -1; // undefined
    }

    public Set<String> stringFlag(String key) {
        return this.parsedArgs.get(key);
    }

    public boolean boolFlag(String key) {
        return this.parsedArgs.containsKey(key);
    }

    public static final class ParseException extends IllegalArgumentException {
        public ParseException(String s) {
            super(s);
        }
    }
}

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

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArgumentsTest {

    @Test
    public void testInitialParse() {
        Arguments arguments = new Arguments(ImmutableList.of("hello"), true);
        assertEquals("hello", arguments.subCommand());

        Arguments.ParseException exception = assertThrowsExactly(
                Arguments.ParseException.class,
                () -> new Arguments(ImmutableList.of("hello"), false)
        );
        assertEquals("Expected flag at position 0 but got 'hello' instead!", exception.getMessage());

        exception = assertThrowsExactly(
                Arguments.ParseException.class,
                () -> new Arguments(ImmutableList.of("hello", "world"), true)
        );
        assertEquals("Expected flag at position 1 but got 'world' instead!", exception.getMessage());
    }

    @Test
    public void testStringFlag() {
        Arguments arguments = new Arguments(ImmutableList.of("--test-flag", "hello"), false);

        Set<String> values = arguments.stringFlag("test-flag");
        assertEquals(1, values.size());
        assertEquals("hello", values.iterator().next());
    }

    @Test
    public void testStringFlagWithSpace() {
        Arguments arguments = new Arguments(ImmutableList.of("--test-flag", "hello", "world"), false);

        Set<String> values = arguments.stringFlag("test-flag");
        assertEquals(1, values.size());
        assertEquals("hello world", values.iterator().next());
    }

    @Test
    public void testStringFlagWithMultipleValues() {
        Arguments arguments = new Arguments(ImmutableList.of("--test-flag", "hello", "--test-flag", "world"), false);

        Set<String> values = arguments.stringFlag("test-flag");
        assertEquals(2, values.size());
        assertEquals(ImmutableList.of("hello", "world"), ImmutableList.copyOf(values));
    }

    @Test
    public void testMissingStringFlag() {
        Arguments arguments = new Arguments(ImmutableList.of("--test-flag", "hello"), false);

        Set<String> values = arguments.stringFlag("missing-flag");
        assertEquals(0, values.size());
    }

    @Test
    public void testIntFlag() {
        Arguments arguments = new Arguments(ImmutableList.of("--test-flag", "123", "--negative-test", "-100"), false);

        int value = arguments.intFlag("test-flag");
        assertEquals(123, value);

        value = arguments.intFlag("negative-test");
        assertEquals(100, value);
    }

    @Test
    public void testMissingIntFlag() {
        Arguments arguments = new Arguments(ImmutableList.of("--test-flag", "hello"), false);

        int value = arguments.intFlag("missing-flag");
        assertEquals(-1, value);
    }

    @Test
    public void testDoubleFlag() {
        Arguments arguments = new Arguments(ImmutableList.of("--test-flag", "123.45", "--negative-test", "-100.5"), false);

        double value = arguments.doubleFlag("test-flag");
        assertEquals(123.45, value, 0.0001);

        value = arguments.doubleFlag("negative-test");
        assertEquals(100.5, value, 0.0001);
    }

    @Test
    public void testMissingDoubleFlag() {
        Arguments arguments = new Arguments(ImmutableList.of("--test-flag", "hello"), false);

        double value = arguments.doubleFlag("missing-flag");
        assertEquals(-1, value);
    }

    @Test
    public void testBooleanFlag() {
        Arguments arguments = new Arguments(ImmutableList.of("--test-flag"), false);

        boolean value = arguments.boolFlag("test-flag");
        assertTrue(value);

        value = arguments.boolFlag("negative-test");
        assertFalse(value);
    }

}

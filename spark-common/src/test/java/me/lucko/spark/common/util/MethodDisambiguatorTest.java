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

import me.lucko.spark.test.TestClass;
import me.lucko.spark.common.util.MethodDisambiguator.MethodDescription;
import me.lucko.spark.common.util.classfinder.FallbackClassFinder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class MethodDisambiguatorTest {

    private static final MethodDisambiguator DISAMBIGUATOR = new MethodDisambiguator(FallbackClassFinder.INSTANCE);

    @ParameterizedTest
    @CsvSource({
            "25, test(Ljava/lang/String;)V",
            "26, test(Ljava/lang/String;)V",
            "27, test(Ljava/lang/String;)V",
            "28, test(Ljava/lang/String;)V",
            "31, test(I)V",
            "32, test(I)V",
            "33, test(I)V",
            "34, test(I)V",
            "37, test(Z)V",
            "38, test(Z)V",
            "39, test(Z)V",
            "40, test(Z)V",
    })
    public void testSuccessfulDisambiguate(int line, String expectedDesc) {
        MethodDescription method = DISAMBIGUATOR.disambiguate(TestClass.class.getName(), "test", line).orElse(null);
        assertNotNull(method);
        assertEquals(expectedDesc, method.toString());
    }

    @ParameterizedTest
    @ValueSource(ints = {24, 29, 100})
    public void testUnsuccessfulDisambiguate(int line) {
        MethodDescription method = DISAMBIGUATOR.disambiguate(TestClass.class.getName(), "test", line).orElse(null);
        assertNull(method);
    }

}

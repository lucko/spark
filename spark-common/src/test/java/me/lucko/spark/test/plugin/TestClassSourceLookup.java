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

package me.lucko.spark.test.plugin;

import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.test.TestClass;
import me.lucko.spark.test.TestClass2;
import org.jspecify.annotations.Nullable;

public enum TestClassSourceLookup implements ClassSourceLookup {
    INSTANCE;

    @Override
    public String identify(Class<?> clazz) {
        if (clazz == TestClass.class) {
            return "test";
        } else if (clazz == TestClass2.class) {
            return "test2";
        }
        return null;
    }

    @Override
    public boolean supportsIdentifyingMethodCalls() {
        return true;
    }

    @Override
    public @Nullable String identify(MethodCall methodCall) {
        MethodCall a = new MethodCall("me.lucko.spark.test.TestClass2", "testA", "()V");
        MethodCall b = new MethodCall("me.lucko.spark.test.TestClass2", "testB", "()V");

        if (methodCall.equals(a)) {
            return "test2_A";
        } else if (methodCall.equals(b)) {
            return "test2_B";
        }

        return null;
    }

    @Override
    public @Nullable String identify(MethodCallByLine methodCall) {
        MethodCallByLine a = new MethodCallByLine("me.lucko.spark.test.TestClass2", "testA", 45);
        MethodCallByLine b = new MethodCallByLine("me.lucko.spark.test.TestClass2", "testB", 49);

        if (methodCall.equals(a)) {
            return "test2_A";
        } else if (methodCall.equals(b)) {
            return "test2_B";
        }

        return null;
    }
}

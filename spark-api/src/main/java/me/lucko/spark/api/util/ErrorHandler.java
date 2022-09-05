/*
 * This file is part of spark, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.spark.api.util;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Interface used for reporting errors during execution of methods.
 */
@FunctionalInterface
public interface ErrorHandler {
    /**
     * Accepts and reports an error.
     *
     * @param error   the error to report
     * @param message a detailed message of the error
     */
    void accept(ErrorType error, String message);

    /**
     * Represents the type of an error.
     *
     * @see #accept(ErrorType, String)
     */
    enum ErrorType {
        /**
         * Indicates that the maximum amount of active samplers the profiler can manage has been reached.
         */
        MAX_AMOUNT_REACHED,
        /**
         * Indicates that the platform does not support tick counting.
         */
        TICK_COUNTING_NOT_SUPPORTED,
        /**
         * Indicates that an invalid duration that the sampler should run for has been supplied.
         *
         * @see me.lucko.spark.api.profiler.Profiler.Sampler#MINIMUM_DURATION
         */
        INVALID_DURATION,

        /**
         * A more general error; indicates that an invalid argument for constructing the sampler has been provided. <br>
         * The message will include more information.
         */
        INVALID_ARGUMENT,

        /**
         * Represents an 'unknown' error type. <br>
         * The message will include more information.
         */
        UNKNOWN
    }

    /**
     * Creates an {@link ErrorHandler} that throws exceptions.
     *
     * @param supplier a factory to use for creating the exceptions
     * @param <T>      the type of the exception
     * @return the handler
     */
    @NotNull
    static <T extends Throwable> ErrorHandler throwing(@NotNull BiFunction<ErrorType, String, T> supplier) {
        return (e, msg) -> throwAsUnchecked(supplier.apply(e, msg));
    }

    /**
     * Creates an {@link ErrorHandler} that throws exceptions. <br>
     * Note: the message passed in the {@code supplier} is obtained in the following way:
     * {@code errorType + ": " + message}
     *
     * @param supplier a factory to use for creating the exceptions
     * @param <T>      the type of the exception
     * @return the handler
     */
    @NotNull
    static <T extends Throwable> ErrorHandler throwingConcat(@NotNull Function<String, T> supplier) {
        return throwing((e, msg) -> supplier.apply(e + ": " + msg));
    }

    @ApiStatus.Internal
    @SuppressWarnings("unchecked")
    static <E extends Throwable> void throwAsUnchecked(Throwable exception) throws E {
        throw (E) exception;
    }
}
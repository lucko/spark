package me.lucko.spark.api.util;

import java.util.stream.Stream;

/**
 * A {@link java.util.function.Supplier supplier} returning a stream of the type {@code T}.
 *
 * @param <T> the type of the stream
 */
@FunctionalInterface
public interface StreamSupplier<T> {
    Stream<T> get();
}

package me.lucko.spark.api.profiler.dumper;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Utility to cache the creation of a {@link ThreadDumper} targeting
 * the game (server/client) thread.
 */
public final class GameThreadDumper implements Supplier<ThreadDumper> {
    private SpecificThreadDumper dumper = null;

    @Override
    public ThreadDumper get() {
        return Objects.requireNonNull(this.dumper, "dumper");
    }

    public void setThread(Thread thread) {
        this.dumper = new SpecificThreadDumper(new long[] {thread.getId()});
    }
}

package me.lucko.spark.api.profiler.report;

import me.lucko.spark.api.profiler.thread.ThreadNode;
import me.lucko.spark.api.profiler.thread.ThreadOrder;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

/**
 * Configuration for {@link ProfilerReport reports}.
 */
public interface ReportConfiguration {
    static ReportConfigurationBuilder builder() {
        return new ReportConfigurationBuilder();
    }

    /**
     * Gets the ordering used by the report.
     *
     * @return the ordering used by the report
     * @see ThreadOrder
     */
    Comparator<ThreadNode> threadOrder();

    /**
     * Gets the sender of the report
     *
     * @return the report's sender, or else {@code null}
     */
    @Nullable
    Sender sender();

    /**
     * If the thread viewer should separate parent calls.
     *
     * @return if the thread viewer should separate parent calls
     */
    boolean separateParentCalls();

    /**
     * Gets the comment of the report.
     *
     * @return the report's comment
     */
    @Nullable
    String comment();

    class Sender {
        public final String name;
        public final UUID uuid;

        public Sender(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }
}

package me.lucko.spark.api.profiler.report;

import me.lucko.spark.api.profiler.thread.ThreadNode;
import me.lucko.spark.api.profiler.thread.ThreadOrder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

public class ReportConfigurationBuilder {
    private Comparator<ThreadNode> order = ThreadOrder.BY_NAME;
    private ReportConfiguration.Sender sender;
    private boolean separateParentCalls;
    private String comment;

    /**
     * Sets the order used by this builder.
     * @param order the order
     * @return the builder
     * @see ThreadOrder
     */
    public ReportConfigurationBuilder order(@NonNull Comparator<ThreadNode> order) {
        this.order = order;
        return this;
    }

    public ReportConfigurationBuilder sender(@Nullable ReportConfiguration.Sender sender) {
        this.sender = sender;
        return this;
    }

    public ReportConfigurationBuilder sender(@NonNull String name, @NonNull UUID uuid) {
        return sender(new ReportConfiguration.Sender(name, uuid));
    }

    public ReportConfigurationBuilder separateParentCalls(boolean separateParentCalls) {
        this.separateParentCalls = separateParentCalls;
        return this;
    }

    public ReportConfigurationBuilder comment(@Nullable String comment) {
        this.comment = comment;
        return this;
    }

    public ReportConfiguration build() {
        return new ReportConfiguration() {
            @Override
            public Comparator<ThreadNode> threadOrder() {
                return order;
            }

            @Override
            public @Nullable Sender sender() {
                return sender;
            }

            @Override
            public boolean separateParentCalls() {
                return separateParentCalls;
            }

            @Override
            public @Nullable String comment() {
                return comment;
            }
        };
    }
}

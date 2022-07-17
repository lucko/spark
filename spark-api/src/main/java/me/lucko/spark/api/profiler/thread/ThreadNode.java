package me.lucko.spark.api.profiler.thread;

/**
 * Represents a thread
 */
public interface ThreadNode {
    /**
     * Gets the label of this thread.
     *
     * @return the label
     */
    String getLabel();

    /**
     * Gets the group of this thread.
     *
     * @return the group
     */
    String getGroup();

    /**
     * Gets the total lifetime of this thread.
     *
     * @return the lifetime
     */
    double getTotalTime();
}

package me.lucko.spark.profiler;

import java.util.Map;

/**
 * Aggregates sampling data.
 */
public interface DataAggregator {

    /**
     * Called before the sampler begins to insert data
     */
    default void start() {

    }

    /**
     * Forms the output data
     *
     * @return the output data
     */
    Map<String, StackNode> getData();

    /**
     * Inserts sampling data into this aggregator
     *
     * @param threadName the name of the thread
     * @param stack the call stack
     */
    void insertData(String threadName, StackTraceElement[] stack);

}

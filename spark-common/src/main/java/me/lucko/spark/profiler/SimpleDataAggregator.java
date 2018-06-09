package me.lucko.spark.profiler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link DataAggregator} that makes use of a "worker" thread pool for inserting
 * data.
 */
public class SimpleDataAggregator implements DataAggregator {

    /** A map of root stack nodes for each thread with sampling data */
    private final Map<String, StackNode> threadData = new ConcurrentHashMap<>();

    /** The worker pool used for sampling */
    private final ExecutorService workerPool;

    /** The instance used to group threads together */
    private final ThreadGrouper threadGrouper;

    /** The interval to wait between sampling, in milliseconds */
    private final int interval;

    public SimpleDataAggregator(ExecutorService workerPool, ThreadGrouper threadGrouper, int interval) {
        this.workerPool = workerPool;
        this.threadGrouper = threadGrouper;
        this.interval = interval;
    }

    @Override
    public void insertData(String threadName, StackTraceElement[] stack) {
        try {
            String group = this.threadGrouper.getGroup(threadName);
            StackNode node = this.threadData.computeIfAbsent(group, StackNode::new);
            node.log(stack, this.interval);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, StackNode> getData() {
        // wait for all pending data to be inserted
        this.workerPool.shutdown();
        try {
            this.workerPool.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return this.threadData;
    }
}

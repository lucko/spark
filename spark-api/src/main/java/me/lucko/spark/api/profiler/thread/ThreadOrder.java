package me.lucko.spark.api.profiler.thread;

import java.util.Comparator;

/**
 * Methods of ordering {@link ThreadNode}s in the output data.
 */
public enum ThreadOrder implements Comparator<ThreadNode> {

    /**
     * Order by the name of the thread (alphabetically)
     */
    BY_NAME {
        @Override
        public int compare(ThreadNode o1, ThreadNode o2) {
            return o1.getLabel().compareTo(o2.getLabel());
        }
    },

    /**
     * Order by the time taken by the thread (most time taken first)
     */
    BY_TIME {
        @Override
        public int compare(ThreadNode o1, ThreadNode o2) {
            return -Double.compare(o1.getTotalTime(), o2.getTotalTime());
        }
    }
}

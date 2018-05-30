package me.lucko.spark.profiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Function for grouping threads together
 */
@FunctionalInterface
public interface ThreadGrouper {

    /**
     * Gets the group for the given thread.
     *
     * @param threadName the name of the thread
     * @return the group
     */
    String getGroup(String threadName);

    /**
     * Implementation of {@link ThreadGrouper} that just groups by thread name.
     */
    ThreadGrouper BY_NAME = new ByName();

    final class ByName implements ThreadGrouper {
        @Override
        public String getGroup(String threadName) {
            return threadName;
        }
    }

    /**
     * Implementation of {@link ThreadGrouper} that attempts to group by the name of the pool
     * the thread originated from.
     */
    ThreadGrouper BY_POOL = new ByPool();

    final class ByPool implements ThreadGrouper {
        private static final Pattern THREAD_POOL_PATTERN = Pattern.compile("^(.*)[-#] ?\\d+$");

        @Override
        public String getGroup(String threadName) {
            Matcher matcher = THREAD_POOL_PATTERN.matcher(threadName);
            if (!matcher.matches()) {
                return threadName;
            }

            return matcher.group(1).trim() + " (Combined)";
        }
    }

}

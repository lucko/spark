package me.lucko.spark.api.profiler;

/**
 * Group the threads by a specific mode. Either grouping them by name, pool or combined into a single group.
 */
public enum GrouperChoice {
    SINGLE,
    NAME,
    POOL
}

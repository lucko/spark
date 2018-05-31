package me.lucko.spark.profiler;

/**
 * A hook with the game's "tick loop".
 */
public interface TickCounter {

    /**
     * Starts the counter
     */
    void start();

    /**
     * Stops the counter
     */
    void close();

    /**
     * Gets the current tick number
     *
     * @return the current tick
     */
    long getCurrentTick();

    /**
     * Adds a task to be called each time the tick increments
     *
     * @param runnable the task
     */
    void addTickTask(Runnable runnable);

    /**
     * Removes a tick task
     *
     * @param runnable the task
     */
    void removeTickTask(Runnable runnable);

}

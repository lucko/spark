<h1 align="center">
	<img
		alt="spark"
		src="https://i.imgur.com/pkZ1k3R.png">
</h1>

<h3 align="center">
	spark is a performance profiling plugin based on sk89q's <a href="https://github.com/sk89q/WarmRoast">WarmRoast profiler</a>
</h3>

<p align="center">
	<strong>
		<a href="https://ci.lucko.me/job/spark/">Downloads</a>
		•
		<a href="https://github.com/lucko/spark/wiki">Wiki</a>
		•
		<a href="https://github.com/lucko/spark/issues">Issues</a>
	</strong>
</p>

___

## What does it do?

spark is made up of a number of components, each detailed separately below.

|                 CPU Profiler                  |            Memory Inspection             |          Server Health Reporting           |
| :-------------------------------------------: | :--------------------------------------: | :----------------------------------------: |
|     ![](https://i.imgur.com/ggSGzRq.png)      |   ![](https://i.imgur.com/BsdTxqA.png)   |    ![](https://i.imgur.com/SrKEmA6.png)    |
| Diagnose performance issues with your server. | Diagnose memory issues with your server. | Keep track of your servers overall health. |

### :zap: CPU Profiler

spark's CPU profiler is an improved version of the popular WarmRoast profiler by sk89q. It can be used to diagnose performance issues ("lag", low tick rate, etc).

It is:

* **Lightweight** - can be ran on production servers with minimal impact.
* **Easy to use** - no configuration or setup necessary, just install the plugin.
* **Quick to produce results** - running for just ~30 seconds is enough to produce useful insights into problematic areas for performance.
* **Customisable** - can be tuned to target specific threads, sample at a specific interval, record only "laggy" periods, etc
* **Highly readable** - simple tree structure lends itself to easy analysis and interpretation. The viewer can also apply deobfuscation mappings.

It works by sampling statistical data about the servers activity, and constructing a call graph based on this data. The call graph is then displayed in an online viewer for further analysis by the user.

### :zap: Memory Inspection

spark includes a number of tools which are useful for diagnosing memory issues with a server.

* **Heap Summary** - take & analyse a basic snapshot of the servers memory
  * A simple view of the JVM's heap, see memory usage and instance counts for each class
  * Not intended to be a full replacement of proper memory analysis tools. (see below)
* **Heap Dump** - take a full (HPROF) snapshot of the servers memory
  * Dumps (& optionally compresses) a full snapshot of JVM's heap.
  * This snapshot can then be inspected using conventional analysis tools.
* **GC Monitoring** - monitor garbage collection activity on the server
  * Allows the user to relate GC activity to game server hangs, and easily see how long they are taking & how much memory is being free'd.
  * Observe frequency/duration of young/old generation garbage collections to inform which GC tuning flags to use

### :zap: Server Health Reporting

spark can report a number of metrics summarising the servers overall health.

These metrics include:

* **TPS** - ticks per second, to a more accurate degree indicated by the /tps command
* **Tick Durations** - how long each tick is taking (min, max and average)
* **CPU Usage** - how much of the CPU is being used by the server process, and by the overall system
* **Memory Usage** - how much memory is being used by the process
* **Disk Usage** - how much disk space is free/being used by the system

As well as providing tick rate averages, spark can also **monitor individual ticks** - sending a report whenever a single tick's duration exceeds a certain threshold. This can be used to identify trends and the nature of performance issues, relative to other system or game events.

For a comparison between spark, WarmRoast, Minecraft timings and other profiles, see this [page](https://github.com/lucko/spark/wiki/spark-vs-WarmRoast-timings-etc) on the spark wiki.

## License

spark is a fork of [WarmRoast](https://github.com/sk89q/WarmRoast), which is [licensed under the GNU General Public License](https://github.com/sk89q/WarmRoast/blob/3fe5e5517b1c529d95cf9f43fd8420c66db0092a/src/main/java/com/sk89q/warmroast/WarmRoast.java#L1-L17).

Therefore, spark is also licensed under the GNU General Public License.

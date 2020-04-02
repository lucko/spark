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
* **Memory Usage** - much much memory is being used by the process
* **Disk Usage** - how much disk space is free/being used by the system

As well as providing tick rate averages, spark can also **monitor individual ticks** - sending a report whenever a single tick's duration exceeds a certain threshold. This can be used to identify trends and the nature of performance issues, relative to other system or game events.



## spark's CPU Profiler vs others

### WarmRoast

Whilst the CPU profiler in spark is based on WarmRoast, it has been improved over time and differs from upstream in the following ways:

* Installation and usage is significantly easier.
  * Access to the underlying server machine is not needed.
  * No need to expose/navigate to a temporary web server (open ports, disable firewall?, go to temp webpage)
* Profiling output can be quickly viewed & shared with others.
* Deobfuscation mappings can be applied without extra setup, and CraftBukkit and Fabric sources are supported in addition to MCP (Searge) names.
* Sampler & viewer components have both been significantly optimized.
  * Now able to sample at a higher rate & use less memory doing so
* Additional customisation options added.
  * Ability to filter output by "laggy ticks" only, group threads from thread pools together, etc
  * Ability to filter output to parts of the call tree containing specific methods or classes
* Sampling accuracy improved
  * The profiler groups by distinct methods, and not just by method name

### Minecraft Timings

Aikar's [timings](https://github.com/aikar/timings) system (built into Spigot and Sponge) is similar to spark in the sense that it also records data about server performance and presents this for analysis.

Timings can do the following things that spark does not:

* Count the number of times certain things (events, entity ticking, etc) occur within the recorded period
* Display output in a way that is more easily understandable by server admins unfamiliar with reading profiler data
* Break down server activity by "friendly" descriptions of the nature of the work being performed

If these things are important to you, then timings is likely a better option.

However, if they are less important, then spark has a few advantages:

* Each area of analysis does not need to be manually defined - spark will record data for everything.
  * For example, timings might identify that a certain listener in plugin x is taking up a lot of CPU time processing the PlayerMoveEvent, but it won't tell you which part of the processing is slow - spark will.
* For programmers interested in optimizing plugins or the server software (or server admins wishing to report issues), the spark output is usually more useful.
  * Timings is not detailed enough to give information about slow areas of code. 

### Other Java profilers

* spark (a sampling profiler) is typically less numerically accurate compared to other profiling methods (e.g. instrumentation), but allows the target program to run at near full speed.
  * In practice, sampling profilers can often provide a more accurate picture of the target program's execution than other approaches, as they are not as intrusive to the target program, and thus don't have as many side effects.
* With spark it is not necessary to inject a Java agent when starting the server.
* Easy to apply deobfuscation mappings.
* spark is more than good enough for the vast majority of performance issues likely to be encountered on Minecraft servers, but may fall short when analysing performance of code ahead of time (in other words before it becomes a bottleneck / issue).



## License

spark is a fork of [WarmRoast](https://github.com/sk89q/WarmRoast), which is [licensed under the GNU General Public License](https://github.com/sk89q/WarmRoast/blob/3fe5e5517b1c529d95cf9f43fd8420c66db0092a/src/main/java/com/sk89q/warmroast/WarmRoast.java#L1-L17).

Therefore, spark is also licensed under the GNU General Public License.

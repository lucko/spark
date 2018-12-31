# :zap: spark
spark is a performance profiling plugin based on sk89q's [WarmRoast profiler](https://github.com/sk89q/WarmRoast).

The latest downloads are [available on Jenkins](https://ci.lucko.me/job/spark/).

## What does it do?

spark is made up of a number of components, each detailed separately below.

### CPU Profiler (process sampling)
This is the primary component of spark - a lightweight CPU sampler with corresponding web analysis view based on WarmRoast.

The sampler records statistical data about which actions take up the most processing time. These statistics can then be used to diagnose potential performance issues with certain parts of the server or specific plugins.

Once the data has been recorded, a "call graph" can be formed and displayed in a web browser for analysis.

A profiler like the one in spark will not magically fix "lag" - they are merely a tool to help diagnose the cause of poor performance.

### Tick Monitor (server tick monitoring)

This component monitors the speed at which the game server is processing "ticks". Can be used to spot trends and identify the nature of a performance issue, relative to other system events. (garbage collection, game actions, etc)

### Memory Inspection (heap analysis & GC monitoring)

This component provides a function which can be used to take basic snapshots of system memory usage, including information about potentially problematic classes, estimated sizes and instance counts corresponding to objects in the JVM.

Unlike the other "profiler"-like functionality in spark, this component is *not* intended to be a full replacement for proper memory analysis tools. It just shows a simplified view.

spark also includes functionality which allows "full" hprof snapshots to be taken. These can be then analysed with conventional memory analysis tools.

## Features

### WarmRoast features

These features are carried over from the upstream "WarmRoast" project.

* The viewer is entirely web-based — no specialist software is required to view the output, just a web browser!
* Output is arranged as a stack of nested method calls, making it easy to interpret the output
* Nodes can be expanded and collapsed to reveal timing details and further child nodes.
* The overall CPU usage and contribution of a particular method can be seen at a glance.
* See the percentage of CPU time for each method relative to its parent methods.
* Sampling frequency can be adjusted.
* Virtually no overheads or side effects on the target program (the server)

### spark features

WarmRoast is an amazing tool for server admins, but it has a few flaws.

* It is not accessible to some users, because in order to use it, you need to have direct SSH (or equivalent) access to the server. (not possible on shared hosts)
* It can be somewhat clunky to setup and start (typical steps: ssh into server machine, open up ports / disable firewall rules?, start process, identify target VM, allow profiler to run for a bit, open a web browser & navigate to the temporary web page hosted by the application. not ideal!)
* It's not easy to share profiling data with other developers or admins.
* Java Development Kit must be installed on the target machine.

I've attempted to address these flaws in spark.

* Profiling is managed entirely using in-game or console commands.
* You don't need to have direct access to the server machine - just install the plugin as you would normally.
* Data is uploaded to a "pastebin"-esque site to be viewed - a temporary web server is not needed, and you can easily share your analysis with others!
* It is not necessary to install any special Java agents or provide a path to the Java Development Kit

Other benefits of spark compared with other profilers:

* MCP (Mod Coder Pack) deobfuscation mappings can be applied to method names directly in the viewer
  * This works for both partially deobfuscated Bukkit mappings, as well as for Sponge/Forge (Searge) mappings
* No specialist software is required to view the output, just a web browser.

### spark vs "Real Profilers"
The spark (WarmRoast) profiler operates using a technique known as [sampling](https://en.wikipedia.org/wiki/Profiling_(computer_programming)#Statistical_profilers). A sampling profiler works by probing the target programs call stack at regular intervals in order to determine how frequently certain actions are being performed. In practice, sampling profilers can often provide a more accurate picture of the target program's execution than other approaches, as they are not as intrusive to the target program, and thus don't have as many side effects.

Sampling profiles are typically less numerically accurate and specific than other profiling methods (e.g. instrumentation), but allow the target program to run at near full speed.

The resultant data is not exact, but a statistical approximation. The accuracy of the output improves as the sampler runs for longer durations, or as the sampling interval is made more frequent.

### spark vs "Minecraft Timings"

Aikar's [timings](https://github.com/aikar/timings) system (built into Spigot and Sponge) is similar to spark/WarmRoast, in the sense that it also analyses the CPU activity of the server.

timings will generally be slightly more accurate than spark, but is (arguably?!) less useful, as each area of analysis has to be manually defined.

For example, timings might identify that a certain listener in plugin x is taking up a lot of CPU time processing the PlayerMoveEvent, but it won't tell you which part of the processing is slow. spark/WarmRoast on the other hand *will* show this information, right down to the name of the method call causing the issue.

## Installation

To install, add the **spark.jar** file to your servers plugins/mods directory, and then restart your server.

## Commands

All commands require the `spark` permission.

Note that `/sparkb`, `/sparkv`, and `/sparkc` must be used instead of `/spark` on BungeeCord, Velocity and Forge Client installs respectively. 

___
#### `/spark start`
Starts a new profiling operation.

**Arguments**
* `--timeout <timeout>`
	* Specifies how long the profiler should run before automatically stopping. Measured in seconds.
	* If left unspecified, the profiler will run indefinitely, until it is stopped
* `--thread <thread name>`
	* Specifies the name of the thread to be profiled.
	* If left unspecified, the profiler will only sample the main "server thread".
	* The `*` character can be used in place of a name to mark that all threads should be profiled
* `--regex`
	* Specifies that the set of threads defined should be interpreted as regex patterns.
* `--not-combined`
	* Specifies that threads from a pool should not be combined into a single node.
* `--interval <interval>`
	* Specifies the interval between samples. Measured in milliseconds.
	* Lower values will improve the accuracy of the results, but may result in server lag.
	* If left unspecified, a default interval of 10 milliseconds is used.
* `--only-ticks-over <tick length millis>`
	* Specifies that entries should only be included if they were part of a tick that took longer than the specified duration to execute.
* `--include-line-numbers`
	* Specifies that line numbers of method calls should be recorded and included in the sampler output.
___
#### `/spark info`
Prints information about the active profiler, if present.

___
#### `/spark stop`
Ends the current profiling operation, uploads the resultant data, and returns a link to the viewer.

___
#### `/spark cancel`
Cancels the current profiling operation, and discards any recorded data without uploading it.

___
#### `/spark monitoring`
Starts/stops the tick monitoring system.

**Arguments**
* `--threshold <percentage increase>`
	* Specifies the report threshold, measured as a percentage increase from the average tick duration.
* `--without-gc`
	* Specifies that GC notifications should not be shown.

___
#### `/spark heapsummary`
Creates a new memory (heap) dump summary, uploads the resultant data, and returns a link to the viewer.

**Arguments**
* `--run-gc-before`
	* Specifies that before recording data, spark should *suggest* that the system performs garbage collection.

___
#### `/spark heapdump`
Creates a new heapdump (.hprof snapshot) file and saves to the disk.

**Arguments**
* `--run-gc-before`
	* Specifies that before recording data, spark should *suggest* that the system performs garbage collection.
* `--include-non-live`
	* Specifies that "non-live" objects should be included. (objects that are not reachable from others)

## License

spark is a fork of [WarmRoast](https://github.com/sk89q/WarmRoast), which is [licensed under the GNU General Public License](https://github.com/sk89q/WarmRoast/blob/3fe5e5517b1c529d95cf9f43fd8420c66db0092a/src/main/java/com/sk89q/warmroast/WarmRoast.java#L1-L17).

Therefore, spark is also licensed under the GNU General Public License.
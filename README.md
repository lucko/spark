# :zap: spark
Spark is a CPU profiling plugin based on sk89q's [WarmRoast profiler](https://github.com/sk89q/WarmRoast).

## What does it do?

Effectively, it monitors the activity of the server, and records statistical data about which actions take up the most processing time. These statistics can then be used to diagnose potential performance issues with certain parts of the server or specific plugins.

Once the data has been recorded, a "call graph" can be formed and displayed in a web browser for analysis.

spark will not fix "lag" - it is a tool to help diagnose the cause of poor performance.

## About

### WarmRoast features

These features are carried over from the upstream "WarmRoast" project.

* The viewer is entirely web-based— no specialist software is required to view the output, just a web browser!
* Output is arranged as a stack of nested method calls, making it easy to interpret the output
* Nodes can be expanded and collapsed to reveal timing details and further child nodes.
* The overall CPU usage and contribution of a particular method can be seen at a glance.
* See the percentage of CPU time for each method relative to its parent methods.
* Sampling frequency can be adjusted.
* Virtually no overheads or side effects on the target program (the server)

### spark features

WarmRoast is an amazing tool for server admins, but it has a few flaws.

* It is not accessible to some people, because in order to use it, you need to have direct SSH (or equivalent) access to the server. (not possible on shared hosts)
* It can be somewhat clunky to setup and start - firstly, you need to connect to the machine of the server you want to profile. Then, you need to remember the PID of the server, or identify it in a list of running VM display names (not easy when multiple servers are running!) - then allow the profiler to run for a bit, before navigating to a temporary web server hosted by the application.
* It's not easy to share profiling data with other developers or admins.
* You need to have the Java Development Kit installed on your machine.

I've attempted to address these flaws in spark.

* Profiling is managed entirely using in-game or console commands. You don't need to have direct access to the server machine - just install the plugin as you would normally.
* Data is uploaded to a "pastebin"-esque site to be viewed - a temporary web server is not needed, and you can easily share your analysis with others!
* It is not necessary to install any special Java agents or provide a path to the Java Development Kit

Other benefits of spark compared with other profilers:

* MCP (Mod Coder Pack) deobfuscation mappings can be applied to method names directly in the viewer
  * This works for both partially deobfuscated Bukkit mappings, as well as for Sponge/Forge (Searge) mappings
* No specialist software is required to view the output, just a web browser.

### How does it work?
The spark (WarmRoast) profiler operates using a technique known as [sampling](https://en.wikipedia.org/wiki/Profiling_(computer_programming)#Statistical_profilers). A sampling profiler works by probing the target programs call stack at regular intervals in order to determine how frequently certain actions are being performed. In practice, sampling profilers can often provide a more accurate picture of the target program's execution than other approaches, as they are not as intrusive to the target program, and thus don't have as many side effects.

Sampling profiles are typically less numerically accurate and specific than other profiling methods (e.g. instrumentation), but allow the target program to run at near full speed.

The resultant data is not exact, but a statistical approximation. The accuracy of the output improves as the sampler runs for longer durations, or as the sampling interval is made more frequent.

### spark vs "Minecraft Timings"

Aikar's [timings](https://github.com/aikar/timings) system (built into Spigot and Sponge) is similar to spark/WarmRoast, in the sense that it also analyses the CPU activity of the server.

timings will generally be slightly more accurate than spark, but is (arguably?!) less useful, as each area of analysis has to be manually defined.

For example, timings might identify that a certain listener in plugin x is taking up a lot of CPU time processing the PlayerMoveEvent, but it won't tell you which part of the processing is slow. spark/WarmRoast on the other hand *will* show this information, right down to the name of the method call causing the bad performance.

## Installation

To install, add the **spark.jar** file to your servers plugins/mods directory, and then restart your server.

## Commands

All commands require the `spark.profiler` permission.

___
#### `/profiler start`
Starts a new profiling operation.

**Arguments**
* `--timeout <timeout>`
	* Specifies how long the profiler should run before automatically stopping. Measured in seconds.
	* If left unspecified, the profiler will run indefinitely, until it is stopped
* `--thread <thread name>`
	* Specifies the name of the thread to be profiled.
	* If left unspecified, the profiler will only sample the main "server thread".
	* The `*` character can be used in place of a name to mark that all threads should be profiled
* `--interval <interval>`
	* Specifies the interval between samples. Measured in milliseconds.
	* Lower values will improve the accuracy of the results, but may result in server lag.
	* If left unspecified, a default interval of 10 milliseconds is used.
___
#### `/profiler info`
Prints information about the active profiler, if present.

___
#### `/profiler stop`
Ends the current profiling operation, uploads the resultant data, and returns a link to view the call graph.

___
#### `/profiler cancel`
Cancels the current profiling operation, and discards any recorded data without uploading it.

## License

spark is a fork of [WarmRoast](https://github.com/sk89q/WarmRoast), which is [licensed under the GNU General Public License](https://github.com/sk89q/WarmRoast/blob/3fe5e5517b1c529d95cf9f43fd8420c66db0092a/src/main/java/com/sk89q/warmroast/WarmRoast.java#L1-L17).

Therefore, spark is also licensed under the GNU General Public License.
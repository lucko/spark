WarmRoast
=========

WarmRoast is an easy-to-use CPU sampling tool for JVM applications, but particularly suited for Minecraft servers/clients.

* Adjustable sampling frequency.
* Supports loading MCP mappings for deobfuscating class and method names.
* Web-based — perform the profiling on a remote server and view the results in your browser.
 * Collapse and expand nodes to see details.
 * Easily view CPU usage per method at a glance.
 * Hover to highlight all child methods as a group.
 * See the percentage of CPU time for each method relative to its parent methods.
 * Maintains style and function with use of "File -> Save As" (in tested browsers).

**Download Latest Version:** http://builds.enginehub.org/job/warmroast/last-successful/

Java 7 and above is required to use WarmRoast.

Screenshots
-----------

![Sample output](http://i.imgur.com/Iy7kJ7f.png)

Usage
-----

1. Note the path of your JDK.

2. Download WarmRoast.

3. Replace `PATH_TO_JDK` in the following commands with the path to your JDK and execute the program.

**Note:** The example command line below includes `--thread "Server thread"`, which filters all threads but the main server thread. You can remove it to show all threads.

**Modded/vanilla servers:** If you are using a modded server, get a copy of [MCP](http://mcp.ocean-labs.de/index.php/MCP_Releases) for your server's Minecraft version, copy the files from conf/ somewhere, and point WarmRoast to it with `--mappings path/to/folder`. This helps readability a lot. Bukkit uses its own mapping, so a pure non-modded Bukkit server can't use MCP mappings.

### Linux ###

    java -Djava.library.path=PATH_TO_JDK/jre/bin -cp /path/to/jdk/lib/tools.jar:warmroast-1.0.0-SNAPSHOT.jar com.sk89q.warmroast.WarmRoast --thread "Server thread"

### Windows ###

An example `PATH_TO_JDK` would be `C:\Program Files\Java\jdk1.7.0_45`

    java -Djava.library.path=PATH_TO_JDK/jre/bin -cp PATH_TO_JDK/lib/tools.jar;warmroast-1.0.0-SNAPSHOT.jar com.sk89q.warmroast.WarmRoast --thread "Server thread"

Parameters
----------

    Usage: warmroast [options]
      Options:
        --bind
           The address to bind the HTTP server to
           Default: 0.0.0.0
           
        -h, --help
           Default: false
           
        --interval
           The sample rate, in milliseconds
           Default: 100
           
        -m, --mappings
           A directory with joined.srg and methods.csv
           
        --name
           The name of the VM to attach to
           
        --pid
           The PID of the VM to attach to
           
        -p, --port
           The port to bind the HTTP server to
           Default: 23000
           
        -t, --thread
           Optionally specify a thread to log only
           
        --timeout
           The number of seconds before ceasing sampling (optional)

Hint: `--thread "Server thread"` is useful for Minecraft servers.

License
-------

The project is licensed under the GNU General Public License, version 3.

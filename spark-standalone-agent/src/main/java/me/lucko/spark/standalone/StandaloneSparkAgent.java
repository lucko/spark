/*
 * This file is part of spark.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.lucko.spark.standalone;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StandaloneSparkAgent {

    // Entry point when the agent is run as a normal jar
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar spark-standalone-agent.jar <pid> [args...]");

            List<VirtualMachineDescriptor> vms = VirtualMachine.list();
            if (vms.isEmpty()) {
                return;
            }

            System.out.println("Current JVM processes:");
            for (VirtualMachineDescriptor vm : vms) {
                System.out.println("  pid=" + vm.id() + " (" + vm.displayName() + ")");
            }

            return;
        }

        try {
            VirtualMachine vm = VirtualMachine.attach(args[0]);
            URI agentPath = StandaloneSparkAgent.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            String arguments = String.join(",", Arrays.copyOfRange(args, 1, args.length));
            vm.loadAgent(Paths.get(agentPath).toAbsolutePath().toString(), arguments);
            System.out.println("[spark] Agent loaded successfully.");
            vm.detach();
        } catch (Throwable e) {
            System.err.println("Failed to attach agent to process " + args[0]);
            e.printStackTrace(System.err);
        }
    }

    // Entry point when the agent is loaded via -javaagent
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("[spark] Loading standalone agent... (premain)");
        init(agentArgs, instrumentation);
    }

    // Entry point when the agent is loaded via VirtualMachine#loadAgent
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        System.out.println("[spark] Loading standalone agent... (agentmain)");
        init(agentArgs, instrumentation);
    }

    private static void init(String agentArgs, Instrumentation instrumentation) {
        try {
            Map<String, String> arguments = new HashMap<>();
            if (agentArgs == null) {
                agentArgs = "";
            }
            for (String arg : agentArgs.split(",")) {
                if (arg.contains("=")) {
                    String[] parts = arg.split("=", 2);
                    arguments.put(parts[0], parts[1]);
                } else {
                    arguments.put(arg, "true");
                }
            }
            new StandaloneSparkPlugin(instrumentation, arguments);
        } catch (Throwable e) {
            System.err.println("[spark] Loading failed :(");
            e.printStackTrace(System.err);
        }
    }

}

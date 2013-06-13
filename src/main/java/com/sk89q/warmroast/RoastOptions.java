/*
 * WarmRoast
 * Copyright (C) 2013 Albert Pham <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.sk89q.warmroast;

import com.beust.jcommander.Parameter;

public class RoastOptions {
    
    @Parameter(names = { "-h", "--help" }, help = true)
    public boolean help;

    @Parameter(names = { "--bind" }, description = "The address to bind the HTTP server to")
    public String bindAddress = "0.0.0.0";

    @Parameter(names = { "-p", "--port" }, description = "The port to bind the HTTP server to")
    public Integer port = 23000;

    @Parameter(names = { "--pid" }, description = "The PID of the VM to attach to")
    public Integer pid;

    @Parameter(names = { "--name" }, description = "The name of the VM to attach to")
    public String vmName;

    @Parameter(names = { "-t", "--thread" }, description = "Optionally specify a thread to log only")
    public String threadName;

    @Parameter(names = { "-m", "--mappings" }, description = "A directory with joined.srg and methods.csv")
    public String mappingsDir;

    @Parameter(names = { "--interval" }, description = "The sample rate, in milliseconds")
    public Integer interval = 100;

}

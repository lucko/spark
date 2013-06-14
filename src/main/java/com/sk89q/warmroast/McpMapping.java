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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;

import au.com.bytecode.opencsv.CSVReader;

public class McpMapping {

    private static final Pattern clPattern = 
            Pattern.compile("CL: (?<obfuscated>[^ ]+) (?<actual>[^ ]+)");
    private static final Pattern mdPattern = 
            Pattern.compile("MD: (?<obfuscatedClass>[^ /]+)/(?<obfuscatedMethod>[^ ]+) " +
            		"[^ ]+ (?<method>[^ ]+) [^ ]+");

    private final Map<String, ClassMapping> classes = new HashMap<>();
    private final Map<String, String> methods = new HashMap<>();
    
    public ClassMapping mapClass(String obfuscated) {
        return classes.get(obfuscated);
    }

    public void read(File joinedFile, File methodsFile) throws IOException {
        try (FileReader r = new FileReader(methodsFile)) {
            try (CSVReader reader = new CSVReader(r)) {
                List<String[]> entries = reader.readAll();
                processMethodNames(entries);
            }
        }
        
        List<String> lines = FileUtils.readLines(joinedFile, "UTF-8");
        processClasses(lines);
        processMethods(lines);
    }
    
    public String mapMethodId(String id) {
        return methods.get(id);
    }
    
    public String fromMethodId(String id) {
        String method = methods.get(id);
        if (method == null) {
            return id;
        }
        return method;
    }
    
    private void processMethodNames(List<String[]> entries) {
        boolean first = true;
        for (String[] entry : entries) {
            if (entry.length < 2) {
                continue;
            }
            if (first) { // Header
                first = false;
                continue;
            }
            methods.put(entry[0], entry[1]);
        }
    }
    
    private void processClasses(List<String> lines) {
        for (String line : lines) {
            Matcher m = clPattern.matcher(line);
            if (m.matches()) {
                String obfuscated = m.group("obfuscated");
                String actual = m.group("actual").replace("/", ".");
                classes.put(obfuscated, new ClassMapping(obfuscated, actual));
            }
        }
    }
    
    private void processMethods(List<String> lines) {
        for (String line : lines) {
            Matcher m = mdPattern.matcher(line);
            if (m.matches()) {
                String obfuscatedClass = m.group("obfuscatedClass");
                String obfuscatedMethod = m.group("obfuscatedMethod");
                String method = m.group("method");
                String methodId = method.substring(method.lastIndexOf('/') + 1);
                ClassMapping mapping = mapClass(obfuscatedClass);
                if (mapping != null) {
                    mapping.addMethod(obfuscatedMethod, 
                            fromMethodId(methodId));
                }
            }
        }
    }

}

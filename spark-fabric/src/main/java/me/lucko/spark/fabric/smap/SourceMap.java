/*
 * SMAPSourceDebugExtension.java - Parse source debug extensions and
 * enhance stack traces.
 *
 * Copyright (c) 2012 Michael Schierl
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * - Neither name of the copyright holders nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND THE CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR THE CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package me.lucko.spark.fabric.smap;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to parse "SMAP" (source map) information from loaded Java classes.
 *
 * @author <a href="https://stackoverflow.com/a/11299757">Michael Schierl</a>
 */
public class SourceMap {

    private final String generatedFileName;
    private final String firstStratum;
    private final Map<Integer, FileInfo> fileinfo = new HashMap<>();
    private final Map<Integer, int[]> reverseLineMapping = new HashMap<>();

    private static final Pattern LINE_INFO_PATTERN = Pattern.compile("([0-9]+)(?:#([0-9]+))?(?:,([0-9]+))?:([0-9]+)(?:,([0-9]+))?");

    public SourceMap(String value) {
        String[] lines = value.split("\n");
        if (!lines[0].equals("SMAP") || !lines[3].startsWith("*S ") || !lines[4].equals("*F")) {
            throw new IllegalArgumentException(value);
        }

        this.generatedFileName = lines[1];
        this.firstStratum = lines[3].substring(3);

        int idx = 5;
        while (!lines[idx].startsWith("*")) {
            String infoline = lines[idx++];
            String path = null;

            if (infoline.startsWith("+ ")) {
                path = lines[idx++];
                infoline = infoline.substring(2);
            }

            int pos = infoline.indexOf(" ");
            int filenum = Integer.parseInt(infoline.substring(0, pos));
            String name = infoline.substring(pos + 1);

            this.fileinfo.put(filenum, new FileInfo(name, path == null ? name : path));
        }

        if (lines[idx].equals("*L")) {
            idx++;
            int lastLFI = 0;

            while (!lines[idx].startsWith("*")) {
                Matcher m = LINE_INFO_PATTERN.matcher(lines[idx++]);
                if (!m.matches()) {
                    throw new IllegalArgumentException(lines[idx - 1]);
                }

                int inputStartLine = Integer.parseInt(m.group(1));
                int lineFileID = m.group(2) == null ? lastLFI : Integer.parseInt(m.group(2));
                int repeatCount = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));
                int outputStartLine = Integer.parseInt(m.group(4));
                int outputLineIncrement = m.group(5) == null ? 1 : Integer.parseInt(m.group(5));

                for (int i = 0; i < repeatCount; i++) {
                    int[] inputMapping = new int[] { lineFileID, inputStartLine + i };
                    int baseOL = outputStartLine + i * outputLineIncrement;

                    for (int ol = baseOL; ol < baseOL + outputLineIncrement; ol++) {
                        if (!this.reverseLineMapping.containsKey(ol)) {
                            this.reverseLineMapping.put(ol, inputMapping);
                        }
                    }
                }

                lastLFI = lineFileID;
            }
        }
    }

    public String getGeneratedFileName() {
        return this.generatedFileName;
    }

    public String getFirstStratum() {
        return this.firstStratum;
    }

    public Map<Integer, FileInfo> getFileInfo() {
        return this.fileinfo;
    }

    public Map<Integer, int[]> getReverseLineMapping() {
        return this.reverseLineMapping;
    }

    public record FileInfo(String name, String path) { }
}
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

import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.service.MixinService;

import java.util.*;
import java.util.regex.*;

/**
 * Utility class to parse Source Debug Extensions and enhance stack traces.
 *
 * Note that only the first stratum is parsed and used.
 *
 * @author Michael Schierl
 */
public class SMAPSourceDebugExtension {

    /**
     * Enhance a stack trace with information from source debug extensions.
     *
     * @param t
     *            Throwable whose stack trace should be enhanced
     * @param keepOriginalFrames
     *            Whether to keep the original frames referring to Java source
     *            or drop them
     * @param packageNames
     *            Names of packages that should be scanned for source debug
     *            extensions, or empty to scan all packages
     */
    public static void enhanceStackTrace(Throwable t, boolean keepOriginalFrames, String... packageNames) {
        enhanceStackTrace(t, new HashMap<>(), keepOriginalFrames);
    }

    /**
     * Enhance a stack trace with information from source debug extensions.
     * Provide a custom cache of already resolved and parsed source debug
     * extensions, to avoid parsing them for every new exception.
     *
     * @param t                  Throwable whose stack trace should be enhanced
     * @param cache              Cache to be used and filled
     * @param keepOriginalFrames Whether to keep the original frames referring to Java source
     *                           or drop them
     */
    public static void enhanceStackTrace(Throwable t, Map<String, SMAPSourceDebugExtension> cache, boolean keepOriginalFrames) {
        StackTraceElement[] elements = t.getStackTrace();
        List<StackTraceElement> newElements = null;
        for (int i = 0; i < elements.length; i++) {
            String className = elements[i].getClassName();
            SMAPSourceDebugExtension smap = getSMAPSourceDebugExtension(className, cache);
            StackTraceElement newFrame = null;
            if (smap != null) {
                int[] inputLineInfo = smap.reverseLineMapping.get(elements[i].getLineNumber());
                if (inputLineInfo != null && elements[i].getFileName().equals(smap.generatedFileName)) {
                    FileInfo inputFileInfo = smap.fileinfo.get(inputLineInfo[0]);
                    if (inputFileInfo != null) {
                        final IMixinInfo mixin = findMixin(inputFileInfo.path);
                        newFrame = new StackTraceElement(elements[i].getClassName(), elements[i].getMethodName(),
                                findBestName(inputFileInfo.name, inputFileInfo.path, "Unspecified") + (mixin != null ? " [%s]".formatted(mixin.getConfig().getName()) : ""), inputLineInfo[1]);
                    }
                }
            }
            if (newFrame != null) {
                if (newElements == null) {
                    newElements = new ArrayList<>(Arrays.asList(elements).subList(0, i));
                }
                if (keepOriginalFrames)
                    newElements.add(elements[i]);
                newElements.add(newFrame);
            } else if (newElements != null) {
                newElements.add(elements[i]);
            }
        }
        if (newElements != null) {
            t.setStackTrace(newElements.toArray(StackTraceElement[]::new));
        }
        if (t.getCause() != null)
            enhanceStackTrace(t.getCause(), cache, keepOriginalFrames);
    }

    @Nullable
    public static SMAPSourceDebugExtension getSMAPSourceDebugExtension(String className, Map<String, SMAPSourceDebugExtension> cache) {
        SMAPSourceDebugExtension smap = cache.get(className);
        if (smap == null) {
            String value = SMAPPool.getPooled(className);
            if (value == null) {
                try {
                    final ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(className.replace('.', '/'));
                    if (classNode != null) {
                        value = classNode.sourceDebug;
                    }
                } catch (Exception ignored) {
                }
            }
            if (value != null) {
                value = value.replaceAll("\r\n?", "\n");
                if (value.startsWith("SMAP\n")) {
                    smap = new SMAPSourceDebugExtension(value);
                    cache.put(className, smap);
                }
            }
        }
        return smap;
    }

    public static IMixinInfo getMixinConfigFor(String className, int lineNumber, Map<String, SMAPSourceDebugExtension> cache) {
        final SMAPSourceDebugExtension smap = getSMAPSourceDebugExtension(className, cache);
        if (smap != null) {
            int[] inputLineInfo = smap.reverseLineMapping.get(lineNumber);
            if (inputLineInfo != null) {
                FileInfo inputFileInfo = smap.fileinfo.get(inputLineInfo[0]);
                if (inputFileInfo != null) {
                    return findMixin(inputFileInfo.path);
                }
            }
        }
        return null;
    }

    private static String findBestName(String name, String path, String fallback) {
        if (path != null) return path;
        if (name != null && !name.equals("null")) return name;
        return fallback;
    }

    private static IMixinInfo findMixin(String path) {
        if (path != null && path.endsWith(".java")) {
            ClassInfo info = ClassInfo.fromCache(path.substring(0, path.length() - 5));

            if (info != null && info.isMixin()) {
                final Iterator<IMixinInfo> iterator = info.getAppliedMixins().iterator();
                if (iterator.hasNext()) return iterator.next();
            }
        }
        return null;
    }

    private final String generatedFileName, firstStratum;
    private final Map<Integer, FileInfo> fileinfo = new HashMap<Integer, FileInfo>();
    private final Map<Integer, int[]> reverseLineMapping = new HashMap<Integer, int[]>();

    private static final Pattern LINE_INFO_PATTERN = Pattern.compile("([0-9]+)(?:#([0-9]+))?(?:,([0-9]+))?:([0-9]+)(?:,([0-9]+))?");

    private SMAPSourceDebugExtension(String value) {
        String[] lines = value.split("\n");
        if (!lines[0].equals("SMAP") || !lines[3].startsWith("*S ") || !lines[4].equals("*F"))
            throw new IllegalArgumentException(value);
        generatedFileName = lines[1];
        firstStratum = lines[3].substring(3);
        int idx = 5;
        while (!lines[idx].startsWith("*")) {
            String infoline = lines[idx++], path = null;
            if (infoline.startsWith("+ ")) {
                path = lines[idx++];
                infoline = infoline.substring(2);
            }
            int pos = infoline.indexOf(" ");
            int filenum = Integer.parseInt(infoline.substring(0, pos));
            String name = infoline.substring(pos + 1);
            fileinfo.put(filenum, new FileInfo(name, path == null ? name : path));
        }
        if (lines[idx].equals("*L")) {
            idx++;
            int lastLFI = 0;
            while (!lines[idx].startsWith("*")) {
                Matcher m = LINE_INFO_PATTERN.matcher(lines[idx++]);
                if (!m.matches())
                    throw new IllegalArgumentException(lines[idx - 1]);
                int inputStartLine = Integer.parseInt(m.group(1));
                int lineFileID = m.group(2) == null ? lastLFI : Integer.parseInt(m.group(2));
                int repeatCount = m.group(3) == null ? 1 : Integer.parseInt(m.group(3));
                int outputStartLine = Integer.parseInt(m.group(4));
                int outputLineIncrement = m.group(5) == null ? 1 : Integer.parseInt(m.group(5));
                for (int i = 0; i < repeatCount; i++) {
                    int[] inputMapping = new int[] { lineFileID, inputStartLine + i };
                    int baseOL = outputStartLine + i * outputLineIncrement;
                    for (int ol = baseOL; ol < baseOL + outputLineIncrement; ol++) {
                        if (!reverseLineMapping.containsKey(ol))
                            reverseLineMapping.put(ol, inputMapping);
                    }
                }
                lastLFI = lineFileID;
            }
        }
    }

    private static class FileInfo {
        public final String name, path;

        public FileInfo(String name, String path) {
            this.name = name;
            this.path = path;
        }
    }
}
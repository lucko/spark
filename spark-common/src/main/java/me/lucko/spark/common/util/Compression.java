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

package me.lucko.spark.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.LongConsumer;
import java.util.zip.GZIPOutputStream;

public enum Compression {
    GZIP {
        @Override
        public Path compress(Path file, LongConsumer progressHandler) throws IOException {
            Path compressedFile = file.getParent().resolve(file.getFileName().toString() + ".gz");
            try (InputStream in = Files.newInputStream(file);
                 OutputStream out = Files.newOutputStream(compressedFile);
                 GZIPOutputStream gzipOut = new GZIPOutputStream(out, BUFFER_SIZE)
            ) {
                copy(in, gzipOut, progressHandler);
            }
            return compressedFile;
        }
    };

    /**
     * Compresses the given file and returns the path to the compressed file.
     *
     * @param file the file to compress
     * @param progressHandler a handler to report progress, called with the number of bytes copied so far
     * @return the path to the compressed file
     * @throws IOException if an I/O error occurs
     */
    public abstract Path compress(Path file, LongConsumer progressHandler) throws IOException;

    /** Size of the buffer used to read/write data while copying. */
    private static final int BUFFER_SIZE = 64 * 1024; // 64KB

    /** How often (in bytes) the progress handler should be called. */
    private static final long PROGRESS_REPORT_INTERVAL = 5 * 1024 * 1024; // 5MB

    private static long copy(InputStream from, OutputStream to, LongConsumer progress) throws IOException {
        long totalBytesCopied = 0;
        long bytesCopiedSinceLastReport = 0;

        byte[] buf = new byte[BUFFER_SIZE];
        while (true) {
            int read = from.read(buf);
            if (read == -1) {
                break;
            }
            to.write(buf, 0, read);
            totalBytesCopied += read;

            bytesCopiedSinceLastReport += read;
            if (bytesCopiedSinceLastReport >= PROGRESS_REPORT_INTERVAL) {
                progress.accept(totalBytesCopied);
                bytesCopiedSinceLastReport = 0;
            }
        }

        return totalBytesCopied;
    }
}

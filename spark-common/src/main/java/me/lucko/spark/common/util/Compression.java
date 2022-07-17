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
            try (InputStream in = Files.newInputStream(file)) {
                try (OutputStream out = Files.newOutputStream(compressedFile)) {
                    try (GZIPOutputStream compressionOut = new GZIPOutputStream(out, 1024 * 64)) {
                        copy(in, compressionOut, progressHandler);
                    }
                }
            }
            return compressedFile;
        }
    };
    // XZ {
    //     @Override
    //     public Path compress(Path file, LongConsumer progressHandler) throws IOException {
    //         Path compressedFile = file.getParent().resolve(file.getFileName().toString() + ".xz");
    //         try (InputStream in = Files.newInputStream(file)) {
    //             try (OutputStream out = Files.newOutputStream(compressedFile)) {
    //                 try (XZOutputStream compressionOut = new XZOutputStream(out, new LZMA2Options())) {
    //                     copy(in, compressionOut, progressHandler);
    //                 }
    //             }
    //         }
    //         return compressedFile;
    //     }
    // },
    // LZMA {
    //     @Override
    //     public Path compress(Path file, LongConsumer progressHandler) throws IOException {
    //         Path compressedFile = file.getParent().resolve(file.getFileName().toString() + ".lzma");
    //         try (InputStream in = Files.newInputStream(file)) {
    //             try (OutputStream out = Files.newOutputStream(compressedFile)) {
    //                 try (LZMAOutputStream compressionOut = new LZMAOutputStream(out, new LZMA2Options(), true)) {
    //                     copy(in, compressionOut, progressHandler);
    //                 }
    //             }
    //         }
    //         return compressedFile;
    //     }
    // };

    public abstract Path compress(Path file, LongConsumer progressHandler) throws IOException;

    private static long copy(InputStream from, OutputStream to, LongConsumer progress) throws IOException {
        byte[] buf = new byte[1024 * 64];
        long total = 0;
        long iterations = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;

            // report progress every 5MB
            if (iterations++ % ((1024 / 64) * 5) == 0) {
                progress.accept(total);
            }
        }
        return total;
    }
}

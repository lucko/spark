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

import com.google.protobuf.AbstractMessageLite;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

/**
 * Utility for posting content to bytebin.
 *
 * @see <a href="https://github.com/lucko/bytebin">https://github.com/lucko/bytebin</a>
 */
public class BytebinClient {

    /** The bytebin URL */
    private final String url;
    /** The client user agent */
    private final String userAgent;

    public BytebinClient(String url, String userAgent) {
        this.url = url + (url.endsWith("/") ? "" : "/");
        this.userAgent = userAgent;
    }

    private Content postContent(String contentType, Consumer<OutputStream> consumer, String userAgentExtra) throws IOException {
        String userAgent = userAgentExtra != null
                ? this.userAgent + "/" + userAgentExtra
                : this.userAgent;

        URL url = new URL(this.url + "post");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        try {
            connection.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(10));
            connection.setReadTimeout((int) TimeUnit.SECONDS.toMillis(10));

            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", contentType);
            connection.setRequestProperty("User-Agent", userAgent);
            connection.setRequestProperty("Content-Encoding", "gzip");

            connection.connect();
            try (OutputStream output = connection.getOutputStream()) {
                consumer.accept(output);
            }

            String key = connection.getHeaderField("Location");
            if (key == null) {
                throw new IllegalStateException("Key not returned");
            }
            return new Content(key);
        } finally {
            connection.getInputStream().close();
            connection.disconnect();
        }
    }

    public Content postContent(AbstractMessageLite<?, ?> proto, String contentType, String userAgentExtra) throws IOException {
        return postContent(contentType, outputStream -> {
            try (OutputStream out = new GZIPOutputStream(outputStream)) {
                proto.writeTo(out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, userAgentExtra);
    }

    public Content postContent(AbstractMessageLite<?, ?> proto, String contentType) throws IOException {
        return postContent(proto, contentType, null);
    }

    public static final class Content {
        private final String key;

        Content(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }

}

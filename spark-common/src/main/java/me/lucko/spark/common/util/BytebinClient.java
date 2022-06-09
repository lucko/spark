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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility for posting content to bytebin.
 */
public class BytebinClient extends AbstractHttpClient {

    /** The bytebin URL */
    private final String url;
    /** The client user agent */
    private final String userAgent;

    /**
     * Creates a new bytebin instance
     *
     * @param url the bytebin url
     * @param userAgent the client user agent string
     */
    public BytebinClient(OkHttpClient okHttpClient, String url, String userAgent) {
        super(okHttpClient);
        this.url = url + (url.endsWith("/") ? "" : "/");
        this.userAgent = userAgent;
    }

    /**
     * POSTs GZIP compressed content to bytebin.
     *
     * @param buf the compressed content
     * @param contentType the type of the content
     * @return the key of the resultant content
     * @throws IOException if an error occurs
     */
    public Content postContent(byte[] buf, MediaType contentType) throws IOException {
        RequestBody body = RequestBody.create(contentType, buf);

        Request.Builder requestBuilder = new Request.Builder()
                .url(this.url + "post")
                .header("User-Agent", this.userAgent)
                .header("Content-Encoding", "gzip");

        Request request = requestBuilder.post(body).build();
        try (Response response = makeHttpRequest(request)) {
            String key = response.header("Location");
            if (key == null) {
                throw new IllegalStateException("Key not returned");
            }
            return new Content(key);
        }
    }

    public Content postContent(AbstractMessageLite<?, ?> proto, MediaType contentType) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        try (OutputStream out = new GZIPOutputStream(byteOut)) {
            proto.writeTo(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return postContent(byteOut.toByteArray(), contentType);
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

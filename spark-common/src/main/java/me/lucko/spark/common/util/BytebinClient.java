/*
 * This file is part of bytebin, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.spark.common.util;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;

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

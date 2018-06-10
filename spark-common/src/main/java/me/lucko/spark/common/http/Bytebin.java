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

package me.lucko.spark.common.http;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Utility for uploading JSON data to bytebin.
 */
public final class Bytebin {

    /** Media type for JSON data */
    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    /** The URL used to upload sampling data */
    private static final String UPLOAD_ENDPOINT = "https://bytebin.lucko.me/post";

    public static String postCompressedContent(byte[] buf) throws IOException {
        RequestBody body = RequestBody.create(JSON_TYPE, buf);

        Request.Builder requestBuilder = new Request.Builder()
                .url(UPLOAD_ENDPOINT)
                .header("Content-Encoding", "gzip")
                .post(body);

        Request request = requestBuilder.build();
        try (Response response = HttpClient.makeCall(request)) {
            try (ResponseBody responseBody = response.body()) {
                if (responseBody == null) {
                    throw new RuntimeException("No response");
                }

                try (InputStream inputStream = responseBody.byteStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                        JsonObject object = new Gson().fromJson(reader, JsonObject.class);
                        return object.get("key").getAsString();
                    }
                }
            }
        }
    }

    private Bytebin() {}
}

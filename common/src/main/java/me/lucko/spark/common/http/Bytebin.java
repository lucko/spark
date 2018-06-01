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

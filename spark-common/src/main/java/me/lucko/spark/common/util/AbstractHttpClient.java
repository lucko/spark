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

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AbstractHttpClient {

    /** The http client */
    protected final OkHttpClient okHttp;

    public AbstractHttpClient(OkHttpClient okHttp) {
        this.okHttp = okHttp;
    }

    protected Response makeHttpRequest(Request request) throws IOException {
        Response response = this.okHttp.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new RuntimeException("Request was unsuccessful: " + response.code() + " - " + response.message());
        }
        return response;
    }
}

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

package me.lucko.spark.common.util.ws;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Java 11 implementation of {@link BytesocksClient}.
 */
class BytesocksClientImpl implements BytesocksClient {

    private final HttpClient client;

    /* The bytesocks urls */
    private final String httpUrl;
    private final String wsUrl;

    /** The client user agent */
    private final String userAgent;

    BytesocksClientImpl(String host, String userAgent) {
        this.client = HttpClient.newHttpClient();

        this.httpUrl = "https://" + host + "/";
        this.wsUrl = "wss://" + host + "/";
        this.userAgent = userAgent;
    }

    @Override
    public Socket createAndConnect(Listener listener) throws Exception {
        HttpRequest createRequest = HttpRequest.newBuilder()
                .uri(URI.create(this.httpUrl + "create"))
                .header("User-Agent", this.userAgent)
                .build();

        HttpResponse<Void> resp = this.client.send(createRequest, HttpResponse.BodyHandlers.discarding());
        if (resp.statusCode() != 201) {
            throw new RuntimeException("Request failed: " + resp);
        }

        String channelId = resp.headers().firstValue("Location").orElse(null);
        if (channelId == null) {
            throw new RuntimeException("Location header not returned: " + resp);
        }

        return connect(channelId, listener);
    }

    @Override
    public Socket connect(String channelId, Listener listener) throws Exception {
        WebSocket socket = this.client.newWebSocketBuilder()
                .header("User-Agent", this.userAgent)
                .connectTimeout(Duration.ofSeconds(5))
                .buildAsync(URI.create(this.wsUrl + channelId), new ListenerImpl(listener))
                .join();

        return new SocketImpl(channelId, socket);
    }

    private static final class SocketImpl implements Socket {
        private final String id;
        private final WebSocket ws;

        private SocketImpl(String id, WebSocket ws) {
            this.id = id;
            this.ws = ws;
        }

        @Override
        public String getChannelId() {
            return this.id;
        }

        @Override
        public boolean isOpen() {
            return !this.ws.isOutputClosed() && !this.ws.isInputClosed();
        }

        @Override
        public CompletableFuture<?> send(CharSequence msg) {
            return this.ws.sendText(msg, true);
        }

        @Override
        public void close(int statusCode, String reason) {
            this.ws.sendClose(statusCode, reason);
        }
    }

    private static final class ListenerImpl implements WebSocket.Listener {
        private final Listener listener;

        private ListenerImpl(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            this.listener.onOpen();
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            return CompletableFuture.runAsync(() -> this.listener.onClose(statusCode, reason));
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            this.listener.onError(error);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            webSocket.request(1);
            return CompletableFuture.runAsync(() -> this.listener.onText(data));
        }
    }


}

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

package me.lucko.spark.common.legacy;

import com.neovisionaries.ws.client.*;
import me.lucko.bytesocks.client.BytesocksClient;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Implementation of BytesocksClient that works on Java 8.
 */
public class LegacyBytesocksClient implements BytesocksClient {

    /* The bytesocks urls */
    private final String httpUrl;
    private final String wsUrl;

    /** The client user agent */
    private final String userAgent;

    LegacyBytesocksClient(String host, String userAgent) {

        this.httpUrl = "https://" + host + "/";
        this.wsUrl = "wss://" + host + "/";
        this.userAgent = userAgent;
    }

    @Override
    public BytesocksClient.Socket createAndConnect(BytesocksClient.Listener listener) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(this.httpUrl + "create").openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", this.userAgent);
        if (con.getResponseCode() != 201) {
            throw new RuntimeException("Request failed");
        }

        String channelId = null;

        for(Map.Entry<String, List<String>> entry : con.getHeaderFields().entrySet()) {
            String key = entry.getKey();
            List<String> value = entry.getValue();
            if(key != null && key.equalsIgnoreCase("Location") && value != null && value.size() > 0) {
                channelId = value.get(0);
                if(channelId != null)
                    break;
            }
        }

        if(channelId == null) {
            throw new RuntimeException("Location header not returned");
        }

        return connect(channelId, listener);
    }

    @Override
    public BytesocksClient.Socket connect(String channelId, BytesocksClient.Listener listener) throws Exception {
        WebSocketFactory factory = new WebSocketFactory()
                .setConnectionTimeout(5000);
        WebSocket socket = factory.createSocket(URI.create(this.wsUrl + channelId))
                .addHeader("User-Agent", this.userAgent)
                .addListener(new ListenerImpl(listener))
                .connect();

        return new SocketImpl(channelId, socket);
    }

    private static final class SocketImpl implements BytesocksClient.Socket {
        private final String id;
        private final WebSocket ws;

        private SocketImpl(String id, WebSocket ws) {
            this.id = id;
            this.ws = ws;
        }

        @Override
        public String channelId() {
            return this.id;
        }

        @Override
        public boolean isOpen() {
            return this.ws.isOpen();
        }

        @Override
        public void send(String msg) {
            this.ws.sendText(msg);
        }

        @Override
        public void close(int statusCode, String reason) {
            this.ws.disconnect(statusCode, reason, 0);
        }

        @Override
        public void closeGracefully(int statusCode, String reason) {
            this.ws.disconnect(statusCode, reason);
        }
    }

    private static final class ListenerImpl extends WebSocketAdapter {
        private final Listener listener;

        private ListenerImpl(Listener listener) {
            this.listener = listener;
        }

        @Override
        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
            this.listener.onOpen();
        }

        @Override
        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
            if (serverCloseFrame != null) {
                this.listener.onClose(serverCloseFrame.getCloseCode(), serverCloseFrame.getCloseReason());
            } else {
                this.listener.onClose(WebSocketCloseCode.ABNORMAL, "connection reset");
            }
        }

        @Override
        public void onError(WebSocket websocket, WebSocketException cause) throws Exception {
            this.listener.onError(cause);
        }

        @Override
        public void onTextMessage(WebSocket websocket, String text) throws Exception {
            this.listener.onText(text);
        }
    }
}

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

package me.lucko.spark.common.ws;

import com.google.protobuf.ByteString;
import me.lucko.bytesocks.client.BytesocksClient;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.window.ProfilingWindowUtils;
import me.lucko.spark.common.util.TimeUtil;
import me.lucko.spark.proto.SparkProtos;
import me.lucko.spark.proto.SparkWebSocketProtos.ClientConnect;
import me.lucko.spark.proto.SparkWebSocketProtos.ClientPing;
import me.lucko.spark.proto.SparkWebSocketProtos.PacketWrapper;
import me.lucko.spark.proto.SparkWebSocketProtos.ServerConnectResponse;
import me.lucko.spark.proto.SparkWebSocketProtos.ServerPong;
import me.lucko.spark.proto.SparkWebSocketProtos.ServerUpdateStatistics;

import java.security.PublicKey;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Represents a connection with the spark viewer.
 */
public abstract class ViewerSocket implements ViewerSocketConnection.Listener, AutoCloseable {

    /** Allow 60 seconds for the first client to connect */
    private static final long SOCKET_INITIAL_TIMEOUT = TimeUnit.SECONDS.toMillis(60);

    /** Once established, expect a ping at least once every 30 seconds */
    private static final long SOCKET_ESTABLISHED_TIMEOUT = TimeUnit.SECONDS.toMillis(30);

    /** The spark platform */
    protected final SparkPlatform platform;
    /** The underlying connection */
    protected final ViewerSocketConnection socket;

    private boolean closed = false;
    private final long socketOpenTime = TimeUtil.monotonicCurrentTimeMillis();
    private long lastPing = 0;
    private String lastPayloadId = null;

    protected ViewerSocket(SparkPlatform platform, BytesocksClient client) throws Exception {
        this.platform = platform;
        this.socket = new ViewerSocketConnection(platform, client, this);
    }

    protected void log(String message) {
        this.platform.getPlugin().log(Level.INFO, "[Viewer - " + this.socket.getChannelId() + "] " + message);
    }

    /**
     * Gets the initial payload to send to the viewer.
     *
     * @return the payload
     */
    public SparkProtos.SocketChannelInfo getPayload() {
        return SparkProtos.SocketChannelInfo.newBuilder()
                .setChannelId(this.socket.getChannelId())
                .setPublicKey(ByteString.copyFrom(this.platform.getTrustedKeyStore().getLocalPublicKey().getEncoded()))
                .build();
    }

    public boolean isOpen() {
        return !this.closed && this.socket.isOpen();
    }

    protected boolean isClosed() {
        return this.closed;
    }

    public boolean checkShouldClose() {
        if (this.closed) {
            return true;
        }

        long time = TimeUtil.monotonicCurrentTimeMillis();
        if ((time - this.socketOpenTime) > SOCKET_INITIAL_TIMEOUT && (time - this.lastPing) > SOCKET_ESTABLISHED_TIMEOUT) {
            log("No clients have pinged for 30s, closing socket");
            close();
            return true;
        }

        // no clients connected yet!
        if (this.lastPing == 0) {
            return true;
        }

        return false;
    }

    @Override
    public void close() {
        this.socket.sendPacket(builder -> builder.setServerPong(ServerPong.newBuilder()
                .setOk(false)
                .build()
        ));
        this.socket.close();
        this.closed = true;
    }

    public String getLastPayloadId() {
        return this.lastPayloadId;
    }

    public void setLastPayloadId(String lastPayloadId) {
        this.lastPayloadId = lastPayloadId;
    }

    @Override
    public boolean isKeyTrusted(PublicKey publicKey) {
        return this.platform.getTrustedKeyStore().isKeyTrusted(publicKey);
    }

    /**
     * Sends a message to the socket to say that the given client is now trusted.
     *
     * @param clientId the client id
     */
    public void sendClientTrustedMessage(String clientId) {
        this.socket.sendPacket(builder -> builder.setServerConnectResponse(ServerConnectResponse.newBuilder()
                .setClientId(clientId)
                .setState(ServerConnectResponse.State.ACCEPTED)
                .build()
        ));
    }

    /**
     * Sends a message to the socket with updated statistics
     *
     * @param platform the platform statistics
     * @param system the system statistics
     * @param metrics the metrics
     */
    public void sendUpdatedStatistics(SparkProtos.PlatformStatistics platform, SparkProtos.SystemStatistics system, SparkProtos.Metrics metrics) {
        this.socket.sendPacket(builder -> builder.setServerUpdateStatistics(ServerUpdateStatistics.newBuilder()
                .setPlatform(platform)
                .setSystem(system)
                .setMetrics(metrics)
                .build()
        ));
    }

    @Override
    public void onPacket(PacketWrapper packet, boolean verified, PublicKey publicKey) throws Exception {
        switch (packet.getPacketCase()) {
            case CLIENT_PING:
                onClientPing(packet.getClientPing(), publicKey);
                break;
            case CLIENT_CONNECT:
                onClientConnect(packet.getClientConnect(), verified, publicKey);
                break;
            default:
                throw new IllegalArgumentException("Unexpected packet: " + packet.getPacketCase());
        }
    }

    private void onClientPing(ClientPing packet, PublicKey publicKey) {
        this.lastPing = TimeUtil.monotonicCurrentTimeMillis();
        this.socket.sendPacket(builder -> builder.setServerPong(ServerPong.newBuilder()
                .setOk(!this.closed)
                .setData(packet.getData())
                .build()
        ));
    }

    private void onClientConnect(ClientConnect packet, boolean verified, PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalStateException("Missing public key");
        }

        this.lastPing = TimeUtil.monotonicCurrentTimeMillis();

        String clientId = packet.getClientId();
        log("Client connected: clientId=" + clientId + ", keyhash=" + hashPublicKey(publicKey) + ", desc=" + packet.getDescription());

        ServerConnectResponse.Builder resp = ServerConnectResponse.newBuilder()
                .setClientId(clientId)
                .setSettings(ServerConnectResponse.Settings.newBuilder()
                        .setSamplerInterval(ProfilingWindowUtils.WINDOW_SIZE_SECONDS)
                        .setStatisticsInterval(10)
                        .build()
                );

        if (this.lastPayloadId != null) {
            resp.setLastPayloadId(this.lastPayloadId);
        }

        if (this.closed) {
            resp.setState(ServerConnectResponse.State.REJECTED);
        } else if (verified) {
            resp.setState(ServerConnectResponse.State.ACCEPTED);
        } else {
            resp.setState(ServerConnectResponse.State.UNTRUSTED);
            this.platform.getTrustedKeyStore().addPendingKey(clientId, publicKey);
        }

        this.socket.sendPacket(builder -> builder.setServerConnectResponse(resp.build()));
    }

    private static String hashPublicKey(PublicKey publicKey) {
        return publicKey == null ? "null" : Integer.toHexString(publicKey.hashCode());
    }

}

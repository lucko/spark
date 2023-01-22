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

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.AbstractSampler;
import me.lucko.spark.common.sampler.Sampler;
import me.lucko.spark.common.util.MediaTypes;
import me.lucko.spark.common.util.ws.BytesocksClient;
import me.lucko.spark.proto.SparkProtos;
import me.lucko.spark.proto.SparkSamplerProtos;
import me.lucko.spark.proto.SparkWebSocketProtos;
import me.lucko.spark.proto.SparkWebSocketProtos.ClientConnect;
import me.lucko.spark.proto.SparkWebSocketProtos.ClientPing;
import me.lucko.spark.proto.SparkWebSocketProtos.PacketWrapper;
import me.lucko.spark.proto.SparkWebSocketProtos.ServerConnectResponse;
import me.lucko.spark.proto.SparkWebSocketProtos.ServerPong;
import me.lucko.spark.proto.SparkWebSocketProtos.ServerUpdateSamplerData;
import me.lucko.spark.proto.SparkWebSocketProtos.ServerUpdateStatistics;

import java.security.PublicKey;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Represents a connection with the spark viewer.
 */
public class ViewerSocket implements ViewerSocketConnection.Listener, AutoCloseable {

    /** The spark platform */
    private final SparkPlatform platform;
    /** The export props to use when exporting the sampler data */
    private final Sampler.ExportProps exportProps;
    /** The underlying connection */
    private final ViewerSocketConnection socket;

    private boolean closed = false;
    private long lastPing = 0;

    public ViewerSocket(SparkPlatform platform, BytesocksClient client, Sampler.ExportProps exportProps) throws Exception {
        this.platform = platform;
        this.exportProps = exportProps;
        this.socket = new ViewerSocketConnection(platform, client, this);
    }

    /**
     * Gets the initial payload to send to the viewer.
     *
     * @param samplerData the sampler data to include in the initial payload
     * @return the payload
     */
    public SparkWebSocketProtos.LiveSamplerData getPayload(SparkSamplerProtos.SamplerData samplerData) {
        return SparkWebSocketProtos.LiveSamplerData.newBuilder()
                .setChannelId(this.socket.getChannelId())
                .setPublicKey(ByteString.copyFrom(this.platform.getTrustedKeyStore().getLocalPublicKey().getEncoded()))
                .setInitialData(samplerData)
                .build();
    }

    public boolean isOpen() {
        return !this.closed && this.socket.isOpen();
    }

    /**
     * Called each time the sampler rotates to a new window.
     *
     * @param sampler the sampler
     */
    public void processWindowRotate(AbstractSampler sampler) {
        if (this.closed) {
            return;
        }

        if ((System.currentTimeMillis() - this.lastPing) > TimeUnit.SECONDS.toMillis(60)) {
            close();
            return;
        }

        try {
            SparkSamplerProtos.SamplerData samplerData = sampler.toProto(this.platform, this.exportProps);
            String key = this.platform.getBytebinClient().postContent(samplerData, MediaTypes.SPARK_SAMPLER_MEDIA_TYPE_LIVE, "live").key();
            sendUpdatedSamplerData(key);
        } catch (Exception e) {
            this.platform.getPlugin().log(Level.WARNING, "Error whilst sending updated sampler data to the socket");
            e.printStackTrace();
        }
    }

    /**
     * Called when the sampler stops.
     *
     * @param sampler the sampler
     */
    public void processSamplerStopped(AbstractSampler sampler) {
        if (this.closed) {
            return;
        }

        close();
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
     * Sends a message to the socket to indicate that updated sampler data is available
     *
     * @param payloadId the payload id of the updated data
     */
    public void sendUpdatedSamplerData(String payloadId) {
        this.socket.sendPacket(builder -> builder.setServerUpdateSampler(ServerUpdateSamplerData.newBuilder()
                .setPayloadId(payloadId)
                .build()
        ));
    }

    /**
     * Sends a message to the socket with updated statistics
     *
     * @param platform the platform statistics
     * @param system the system statistics
     */
    public void sendUpdatedStatistics(SparkProtos.PlatformStatistics platform, SparkProtos.SystemStatistics system) {
        this.socket.sendPacket(builder -> builder.setServerUpdateStatistics(ServerUpdateStatistics.newBuilder()
                .setPlatform(platform)
                .setSystem(system)
                .build()
        ));
    }

    @Override
    public void onPacket(PacketWrapper packet, boolean verified, PublicKey publicKey) throws Exception {
        switch (packet.getPacketCase()) {
            case CLIENTPING:
                onPing(packet.getClientPing());
                break;
            case CLIENTCONNECT:
                onConnect(packet.getClientConnect(), verified, publicKey);
                break;
            default:
                throw new IllegalArgumentException("Unexpected packet: " + packet.getPacketCase());
        }
    }

    private void onPing(ClientPing packet) {
        this.lastPing = System.currentTimeMillis();
        this.socket.sendPacket(builder -> builder.setServerPong(ServerPong.newBuilder()
                .setOk(!this.closed)
                .build()
        ));
    }

    private void onConnect(ClientConnect packet, boolean verified, PublicKey publicKey) {
        if (publicKey == null) {
            throw new IllegalStateException("Missing public key");
        }

        String clientId = packet.getClientId();

        ServerConnectResponse.Builder resp = ServerConnectResponse.newBuilder().setClientId(clientId);
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

}

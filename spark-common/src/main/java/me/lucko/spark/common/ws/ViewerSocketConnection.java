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
import me.lucko.spark.proto.SparkWebSocketProtos.PacketWrapper;
import me.lucko.spark.proto.SparkWebSocketProtos.RawPacket;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Controls a websocket connection between a spark server (the plugin/mod) and a spark client (the web viewer).
 */
public class ViewerSocketConnection implements BytesocksClient.Listener, AutoCloseable {

    /** The protocol version */
    public static final int VERSION_1 = 1;
    /** The crypto algorithm used to sign/verify messages sent between the server and client */
    public static final CryptoAlgorithm CRYPTO = CryptoAlgorithm.RSA2048;

    /** The platform */
    private final SparkPlatform platform;
    /** The underlying listener */
    private final Listener listener;
    /** The private key used to sign messages sent from this connection */
    private final PrivateKey privateKey;
    /** The bytesocks socket */
    private final BytesocksClient.Socket socket;

    public ViewerSocketConnection(SparkPlatform platform, BytesocksClient client, Listener listener) throws Exception {
        this.platform = platform;
        this.listener = listener;
        this.privateKey = platform.getTrustedKeyStore().getLocalPrivateKey();
        this.socket = client.createAndConnect(this);
    }

    public interface Listener {

        /**
         * Checks if the given public key is trusted
         *
         * @param publicKey the public key
         * @return true if trusted
         */
        boolean isKeyTrusted(PublicKey publicKey);

        /**
         * Handles a packet sent to the socket
         *
         * @param packet the packet that was sent
         * @param verified if the packet was signed by a trusted key
         * @param publicKey the public key the packet was signed with
         */
        void onPacket(PacketWrapper packet, boolean verified, PublicKey publicKey) throws Exception;
    }

    /**
     * Gets the bytesocks channel id
     *
     * @return the channel id
     */
    public String getChannelId() {
        return this.socket.getChannelId();
    }

    /**
     * Gets if the underlying socket is open
     *
     * @return true if the socket is open
     */
    public boolean isOpen() {
        return this.socket.isOpen();
    }

    @Override
    public void onText(CharSequence data) {
        try {
            RawPacket packet = decodeRawPacket(data);
            handleRawPacket(packet);
        } catch (Exception e) {
            this.platform.getPlugin().log(Level.WARNING, "Exception occurred while reading data from the socket");
            e.printStackTrace();
        }
    }

    @Override
    public void onError(Throwable error) {
        this.platform.getPlugin().log(Level.INFO, "Socket error: " + error.getClass().getName() + " " + error.getMessage());
        error.printStackTrace();
    }

    @Override
    public void onClose(int statusCode, String reason) {
        //this.platform.getPlugin().log(Level.INFO, "Socket closed with status " + statusCode + " and reason " + reason);
    }

    /**
     * Sends a packet to the socket.
     *
     * @param packetBuilder the builder to construct the wrapper packet
     */
    public void sendPacket(Consumer<PacketWrapper.Builder> packetBuilder) {
        PacketWrapper.Builder builder = PacketWrapper.newBuilder();
        packetBuilder.accept(builder);
        PacketWrapper wrapper = builder.build();

        try {
            sendPacket(wrapper);
        } catch (Exception e) {
            this.platform.getPlugin().log(Level.WARNING, "Exception occurred while sending data to the socket");
            e.printStackTrace();
        }
    }

    /**
     * Sends a packet to the socket.
     *
     * @param packet the packet to send
     */
    private void sendPacket(PacketWrapper packet) throws Exception {
        ByteString msg = packet.toByteString();

        // sign the message using the server private key
        Signature sign = CRYPTO.createSignature();
        sign.initSign(this.privateKey);
        sign.update(msg.asReadOnlyByteBuffer());
        byte[] signature = sign.sign();

        sendRawPacket(RawPacket.newBuilder()
                .setVersion(VERSION_1)
                .setSignature(ByteString.copyFrom(signature))
                .setMessage(msg)
                .build()
        );
    }

    /**
     * Sends a raw packet to the socket.
     *
     * @param packet the packet to send
     */
    private void sendRawPacket(RawPacket packet) throws IOException {
        byte[] buf = packet.toByteArray();
        String encoded = Base64.getEncoder().encodeToString(buf);
        this.socket.send(encoded);
    }

    /**
     * Decodes a raw packet sent to the socket.
     *
     * @param data the encoded data
     * @return the decoded packet
     */
    private RawPacket decodeRawPacket(CharSequence data) throws IOException {
        byte[] buf = Base64.getDecoder().decode(data.toString());
        return RawPacket.parseFrom(buf);
    }

    /**
     * Handles a raw packet sent to the socket
     *
     * @param packet the packet
     */
    private void handleRawPacket(RawPacket packet) throws Exception {
        int version = packet.getVersion();
        if (version != VERSION_1) {
            throw new IllegalArgumentException("Unsupported packet version " + version);
        }

        ByteString message = packet.getMessage();
        PublicKey publicKey = CRYPTO.decodePublicKey(packet.getPublicKey());
        ByteString signature = packet.getSignature();

        boolean verified = false;
        if (signature != null && publicKey != null && this.listener.isKeyTrusted(publicKey)) {
            Signature sign = CRYPTO.createSignature();
            sign.initVerify(publicKey);
            sign.update(message.asReadOnlyByteBuffer());

            verified = sign.verify(signature.toByteArray());
        }

        PacketWrapper wrapper = PacketWrapper.parseFrom(message);
        this.listener.onPacket(wrapper, verified, publicKey);
    }

    @Override
    public void close() {
        this.socket.close(1001 /* going away */, "spark plugin disconnected");
    }
}

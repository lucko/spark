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

import java.util.concurrent.CompletableFuture;

/**
 * A client that can interact with bytesocks.
 *
 * @see <a href="https://github.com/lucko/bytesocks">https://github.com/lucko/bytesocks</a>
 */
public interface BytesocksClient {

    /**
     * Creates a new {@link BytesocksClient}.
     *
     * <p>Returns {@code null} on Java versions before 11.</p>
     *
     * @param host the host
     * @param userAgent the user agent
     * @return the client
     */
    static BytesocksClient create(String host, String userAgent) {
        try {
            return new BytesocksClientImpl(host, userAgent);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    /**
     * Creates a new bytesocks channel and returns a socket connected to it.
     *
     * @param listener the listener
     * @return the socket
     * @throws Exception if something goes wrong
     */
    Socket createAndConnect(Listener listener) throws Exception;

    /**
     * Connects to an existing bytesocks channel.
     *
     * @param channelId the channel id
     * @param listener the listener
     * @return the socket
     * @throws Exception if something goes wrong
     */
    Socket connect(String channelId, Listener listener) throws Exception;

    /**
     * A socket connected to a bytesocks channel.
     */
    interface Socket {

        /**
         * Gets the id of the connected channel.
         *
         * @return the id of the channel
         */
        String getChannelId();

        /**
         * Gets if the socket is open.
         *
         * @return true if the socket is open
         */
        boolean isOpen();

        /**
         * Sends a message to the channel using the socket.
         *
         * @param msg the message to send
         * @return a future to encapsulate the progress of sending the message
         */
        CompletableFuture<?> send(CharSequence msg);

        /**
         * Closes the socket.
         *
         * @param statusCode the status code
         * @param reason the reason
         */
        void close(int statusCode, String reason);
    }

    /**
     * Socket listener
     */
    interface Listener {

        default void onOpen() {}

        default void onError(Throwable error) {}

        default void onText(CharSequence data) {}

        default void onClose(int statusCode, String reason) {}
    }

}

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

import me.lucko.bytesocks.client.BytesocksClient;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.sampler.AbstractSampler;
import me.lucko.spark.common.sampler.Sampler;
import me.lucko.spark.common.util.MediaTypes;
import me.lucko.spark.proto.SparkSamplerProtos;
import me.lucko.spark.proto.SparkWebSocketProtos;

import java.util.logging.Level;

/**
 * Represents a 'sampler' connection with the spark viewer.
 */
public class SamplerViewerSocket extends ViewerSocket {

    /** The export props to use when exporting the sampler data */
    private final Sampler.ExportProps exportProps;

    public SamplerViewerSocket(SparkPlatform platform, BytesocksClient client, Sampler.ExportProps exportProps) throws Exception {
        super(platform, client);
        this.exportProps = exportProps;
    }

    /**
     * Called each time the sampler rotates to a new window.
     *
     * @param sampler the sampler
     */
    public void processWindowRotate(AbstractSampler sampler) {
        if (checkShouldClose()) {
            return;
        }

        try {
            SparkSamplerProtos.SamplerData samplerData = sampler.toProto(this.platform, this.exportProps);
            String key = this.platform.getBytebinClient().postContent(samplerData, MediaTypes.SPARK_SAMPLER_MEDIA_TYPE, "live").key();
            sendUpdatedSamplerData(key);
        } catch (Exception e) {
            this.platform.getPlugin().log(Level.WARNING, "Error whilst sending updated sampler data to the socket", e);
        }
    }

    /**
     * Called when the sampler stops.
     *
     * @param sampler the sampler
     */
    public void processSamplerStopped(AbstractSampler sampler) {
        if (isClosed()) {
            return;
        }

        close();
    }

    /**
     * Sends a message to the socket to indicate that updated sampler data is available
     *
     * @param payloadId the payload id of the updated data
     */
    public void sendUpdatedSamplerData(String payloadId) {
        this.socket.sendPacket(builder -> builder.setServerUpdateSampler(SparkWebSocketProtos.ServerUpdateSamplerData.newBuilder()
                .setPayloadId(payloadId)
                .build()
        ));
        setLastPayloadId(payloadId);
    }
}

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

package me.lucko.spark.common.sampler;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.command.sender.CommandSender;
import me.lucko.spark.common.sampler.java.MergeStrategy;
import me.lucko.spark.common.sampler.source.ClassSourceLookup;
import me.lucko.spark.common.ws.ViewerSocket;
import me.lucko.spark.proto.SparkProtos;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerData;
import me.lucko.spark.proto.SparkSamplerProtos.SocketChannelInfo;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Abstract superinterface for all sampler implementations.
 */
public interface Sampler {

    /**
     * Starts the sampler.
     */
    void start();

    /**
     * Stops the sampler.
     */
    void stop(boolean cancelled);

    /**
     * Attaches a viewer socket to this sampler.
     *
     * @param socket the socket
     */
    void attachSocket(ViewerSocket socket);

    /**
     * Gets the sockets attached to this sampler.
     *
     * @return the attached sockets
     */
    Collection<ViewerSocket> getAttachedSockets();

    /**
     * Gets the time when the sampler started (unix timestamp in millis)
     *
     * @return the start time
     */
    long getStartTime();

    /**
     * Gets the time when the sampler should automatically stop (unix timestamp in millis)
     *
     * @return the end time, or -1 if undefined
     */
    long getAutoEndTime();

    /**
     * If this sampler is running in the background. (wasn't started by a specific user)
     *
     * @return true if the sampler is running in the background
     */
    boolean isRunningInBackground();

    /**
     * Gets the sampler type.
     *
     * @return the sampler type
     */
    SamplerType getType();

    /**
     * Gets the version of the sampler.
     *
     * @return the library version if known, else null
     */
    String getLibraryVersion();

    /**
     * Gets the sampler mode.
     *
     * @return the sampler mode
     */
    SamplerMode getMode();

    /**
     * Gets a future to encapsulate the completion of the sampler
     *
     * @return a future
     */
    CompletableFuture<Sampler> getFuture();

    /**
     * Exports the current set of window statistics.
     *
     * @return the window statistics
     */
    Map<Integer, SparkProtos.WindowStatistics> exportWindowStatistics();

    // Methods used to export the sampler data to the web viewer.
    SamplerData toProto(SparkPlatform platform, ExportProps exportProps);

    final class ExportProps {
        private CommandSender.Data creator;
        private String comment;
        private MergeStrategy mergeStrategy;
        private Supplier<ClassSourceLookup> classSourceLookup;
        private SocketChannelInfo channelInfo;

        public ExportProps() {
        }

        public CommandSender.Data creator() {
            return this.creator;
        }

        public String comment() {
            return this.comment;
        }

        public MergeStrategy mergeStrategy() {
            return this.mergeStrategy;
        }

        public Supplier<ClassSourceLookup> classSourceLookup() {
            return this.classSourceLookup;
        }

        public SocketChannelInfo channelInfo() {
            return this.channelInfo;
        }

        public ExportProps creator(CommandSender.Data creator) {
            this.creator = creator;
            return this;
        }

        public ExportProps comment(String comment) {
            this.comment = comment;
            return this;
        }

        public ExportProps mergeStrategy(MergeStrategy mergeStrategy) {
            this.mergeStrategy = mergeStrategy;
            return this;
        }

        public ExportProps classSourceLookup(Supplier<ClassSourceLookup> classSourceLookup) {
            this.classSourceLookup = classSourceLookup;
            return this;
        }

        public ExportProps channelInfo(SocketChannelInfo channelInfo) {
            this.channelInfo = channelInfo;
            return this;
        }
    }

}

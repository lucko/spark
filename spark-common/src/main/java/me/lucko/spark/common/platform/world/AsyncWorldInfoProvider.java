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

package me.lucko.spark.common.platform.world;

import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.SparkPlugin;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Async-friendly wrapper around {@link WorldInfoProvider}.
 */
public class AsyncWorldInfoProvider {
    private static final int TIMEOUT_SECONDS = 5;

    private final SparkPlatform platform;
    private final WorldInfoProvider provider;

    public AsyncWorldInfoProvider(SparkPlatform platform, WorldInfoProvider provider) {
        this.platform = platform;
        this.provider = provider == WorldInfoProvider.NO_OP ? null : provider;
    }

    private <T> CompletableFuture<T> async(Function<WorldInfoProvider, T> function) {
        if (this.provider == null) {
            return null;
        }

        if (this.provider.mustCallSync()) {
            SparkPlugin plugin = this.platform.getPlugin();
            return CompletableFuture.supplyAsync(() -> function.apply(this.provider), plugin::executeSync);
        } else {
            return CompletableFuture.completedFuture(function.apply(this.provider));
        }
    }

    private <T> T get(CompletableFuture<T> future) {
        if (future == null) {
            return null;
        }

        try {
            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            this.platform.getPlugin().log(Level.WARNING, "Timed out waiting for world statistics");
            return null;
        }
    }

    public CompletableFuture<WorldInfoProvider.CountsResult> pollCounts() {
        return async(WorldInfoProvider::pollCounts);
    }

    public CompletableFuture<WorldInfoProvider.ChunksResult<? extends ChunkInfo<?>>> pollChunks() {
        return async(WorldInfoProvider::pollChunks);
    }

    public WorldInfoProvider.CountsResult getCounts() {
        return get(pollCounts());
    }

    public WorldInfoProvider.ChunksResult<? extends ChunkInfo<?>> getChunks() {
        return get(pollChunks());
    }
}

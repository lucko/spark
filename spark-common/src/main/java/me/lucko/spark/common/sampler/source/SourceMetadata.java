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

package me.lucko.spark.common.sampler.source;

import com.google.common.collect.ImmutableList;

import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * A "source" is a plugin or mod on the platform that may be identified
 * as a source of a method call in a profile.
 */
public class SourceMetadata {

    public static <T> List<SourceMetadata> gather(Collection<T> sources, Function<? super T, String> nameFunction, Function<? super T, String> versionFunction, Function<? super T, String> authorFunction) {
        ImmutableList.Builder<SourceMetadata> builder = ImmutableList.builder();

        for (T source : sources) {
            String name = nameFunction.apply(source);
            String version = versionFunction.apply(source);
            String author = authorFunction.apply(source);

            SourceMetadata metadata = new SourceMetadata(name, version, author);
            builder.add(metadata);
        }

        return builder.build();
    }

    private final String name;
    private final String version;
    private final String author;

    public SourceMetadata(String name, String version, String author) {
        this.name = name;
        this.version = version;
        this.author = author;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getAuthor() {
        return this.author;
    }

    public SamplerMetadata.SourceMetadata toProto() {
        return SamplerMetadata.SourceMetadata.newBuilder()
                .setName(this.name)
                .setVersion(this.version)
                .build();
    }

}

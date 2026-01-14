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
import me.lucko.spark.proto.SparkProtos.PluginOrModMetadata;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A "source" is a plugin or mod on the platform that may be identified
 * as a source of a method call in a profile.
 */
public class SourceMetadata {

    public static <T> List<SourceMetadata> gather(Collection<T> sources, Function<? super T, String> name, Function<? super T, String> version, Function<? super T, String> author, Function<? super T, String> description) {
        return gather(sources, name, version, author, description, t -> false);
    }

    public static <T> List<SourceMetadata> gather(Collection<T> sources, Function<? super T, String> name, Function<? super T, String> version, Function<? super T, String> author, Function<? super T, String> description, Predicate<? super T> builtIn) {
        ImmutableList.Builder<SourceMetadata> builder = ImmutableList.builder();

        for (T source : sources) {
            SourceMetadata metadata = new SourceMetadata(
                    name.apply(source),
                    version.apply(source),
                    author.apply(source),
                    description.apply(source),
                    builtIn.test(source)
            );
            builder.add(metadata);
        }

        return builder.build();
    }

    private final String name;
    private final String version;
    private final String author;
    private final String description;
    private final boolean builtIn;

    public SourceMetadata(String name, String version, String author, String description, boolean builtIn) {
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.builtIn = builtIn;
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

    public String getDescription() {
        return this.description;
    }

    public boolean isBuiltIn() {
        return this.builtIn;
    }

    public PluginOrModMetadata toProto() {
        PluginOrModMetadata.Builder builder = PluginOrModMetadata.newBuilder().setName(this.name);
        if (this.version != null) {
            builder.setVersion(this.version);
        }
        if (this.author != null) {
            builder.setAuthor(this.author);
        }
        if (this.description != null) {
            builder.setDescription(this.description);
        }
        builder.setBuiltin(this.builtIn);

        return builder.build();
    }

}

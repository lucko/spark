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

import me.lucko.spark.common.sampler.async.AsyncSampler;
import me.lucko.spark.common.sampler.java.JavaSampler;
import me.lucko.spark.proto.SparkSamplerProtos.SamplerMetadata;

public enum SamplerType {
    JAVA(JavaSampler.class, SamplerMetadata.SamplerEngine.JAVA),
    ASYNC(AsyncSampler.class, SamplerMetadata.SamplerEngine.ASYNC);

    private final Class<? extends Sampler> expectedClass;
    private final SamplerMetadata.SamplerEngine proto;

    SamplerType(Class<? extends Sampler> expectedClass, SamplerMetadata.SamplerEngine proto) {
        this.expectedClass = expectedClass;
        this.proto = proto;
    }

    public Class<? extends Sampler> implClass() {
        return this.expectedClass;
    }

    public SamplerMetadata.SamplerEngine asProto() {
        return this.proto;
    }

}

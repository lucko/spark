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

package me.lucko.spark.fabric.placeholder;

import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.PlaceholderHandler;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import me.lucko.spark.common.SparkPlatform;
import me.lucko.spark.common.util.SparkPlaceholder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public enum SparkFabricPlaceholderApi {
    ;

    public static void register(SparkPlatform platform) {
        for (SparkPlaceholder placeholder : SparkPlaceholder.values()) {
            Placeholders.register(
                    Identifier.of("spark", placeholder.getName()),
                    new Handler(platform, placeholder)
            );
        }
    }

    private record Handler(SparkPlatform platform, SparkPlaceholder placeholder) implements PlaceholderHandler {
        @Override
        public PlaceholderResult onPlaceholderRequest(PlaceholderContext context, @Nullable String argument) {
            return toResult(this.placeholder.resolve(this.platform, argument));
        }

        private static PlaceholderResult toResult(Component component) {
            return component == null
                    ? PlaceholderResult.invalid()
                    : PlaceholderResult.value(toText(component));
        }

        private static Text toText(Component component) {
            return TextCodecs.CODEC.decode(
                    DynamicRegistryManager.EMPTY.getOps(JsonOps.INSTANCE),
                    GsonComponentSerializer.gson().serializeToTree(component)
            ).getOrThrow(JsonParseException::new).getFirst();
        }
    }

}

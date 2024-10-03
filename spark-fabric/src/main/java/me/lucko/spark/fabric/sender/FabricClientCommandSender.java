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

package me.lucko.spark.fabric.sender;

import me.lucko.spark.common.command.sender.AbstractCommandSender;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Text;

import java.util.UUID;

public class FabricClientCommandSender extends AbstractCommandSender<FabricClientCommandSource> {

    public FabricClientCommandSender(FabricClientCommandSource commandSource) {
        super(commandSource);
    }

    @Override
    public String getName() {
        return this.delegate.getPlayer().getNameForScoreboard();
    }

    @Override
    public UUID getUniqueId() {
        if (this.delegate.getPlayer() instanceof PlayerEntity player) {
            return player.getUuid();
        }
        return null;
    }

    @Override
    public void sendMessage(Component message) {
        Text component = Text.Serialization.fromJsonTree(GsonComponentSerializer.gson().serializeToTree(message), DynamicRegistryManager.EMPTY);
        super.delegate.sendFeedback(component);
    }

    @Override
    public boolean hasPermission(String permission) {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FabricClientCommandSender that = (FabricClientCommandSender) o;
        return this.getName().equals(that.getName());
    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }
}

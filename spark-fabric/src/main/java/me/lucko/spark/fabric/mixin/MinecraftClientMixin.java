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

package me.lucko.spark.fabric.mixin;

import me.lucko.spark.fabric.FabricSparkGameHooks;
import me.lucko.spark.fabric.FabricSparkMod;
import me.lucko.spark.fabric.plugin.FabricClientSparkPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.NonBlockingThreadExecutor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin extends NonBlockingThreadExecutor<Runnable> {

    public MinecraftClientMixin(String string_1) {
        super(string_1);
    }

    // Inject at when menu pops up
    @Inject(method = "init()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/SplashScreen;method_18819(Lnet/minecraft/client/MinecraftClient;)V"))
    public void onInit(CallbackInfo ci) {
        FabricClientSparkPlugin.register(FabricSparkMod.getMod(), (MinecraftClient) (Object) this);
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    public void onTick(CallbackInfo ci) {
        FabricSparkGameHooks.INSTANCE.tickClientCounters();
    }

}

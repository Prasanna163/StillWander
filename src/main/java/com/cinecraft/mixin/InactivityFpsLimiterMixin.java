/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_9919
 *  net.minecraft.class_9919$class_10601
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_9919;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value=EnvType.CLIENT)
@Mixin(value={class_9919.class})
abstract class InactivityFpsLimiterMixin {
    InactivityFpsLimiterMixin() {
    }

    @Inject(method={"method_66514"}, at={@At(value="HEAD")}, cancellable=true)
    private void cinecraft$keepCinematicAtFullRate(CallbackInfoReturnable<class_9919.class_10601> cir) {
        if (CinecraftClient.DIRECTOR.isActive()) {
            cir.setReturnValue((Object)class_9919.class_10601.field_55843);
        }
    }
}


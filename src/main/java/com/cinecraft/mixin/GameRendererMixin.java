/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_757
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value=EnvType.CLIENT)
@Mixin(value={class_757.class})
abstract class GameRendererMixin {
    GameRendererMixin() {
    }

    @Inject(method={"method_3198"}, at={@At(value="HEAD")}, cancellable=true)
    private void cinecraft$disableHurtTilt(class_4587 matrices, float tickDelta, CallbackInfo ci) {
        if (CinecraftClient.DIRECTOR.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method={"method_3186"}, at={@At(value="HEAD")}, cancellable=true)
    private void cinecraft$disableViewBob(class_4587 matrices, float tickDelta, CallbackInfo ci) {
        if (CinecraftClient.DIRECTOR.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method={"method_3196"}, at={@At(value="RETURN")}, cancellable=true)
    private void cinecraft$applyShotFov(class_4184 camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        float fov = CinecraftClient.DIRECTOR.currentFov();
        if (Float.isFinite(fov)) {
            cir.setReturnValue((Object)Float.valueOf(fov));
        }
    }
}


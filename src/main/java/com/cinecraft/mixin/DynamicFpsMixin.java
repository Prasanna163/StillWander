/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_310
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Pseudo
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value=EnvType.CLIENT)
@Pseudo
@Mixin(targets={"dynamic_fps/impl/DynamicFPSMod"}, remap=false)
abstract class DynamicFpsMixin {
    DynamicFpsMixin() {
    }

    @Inject(method={"isDisabled()Z"}, at={@At(value="HEAD")}, cancellable=true, require=0, remap=false)
    private static void cinecraft$disableDynamicFps(CallbackInfoReturnable<Boolean> cir) {
        if (CinecraftClient.requiresUnthrottledRendering()) {
            cir.setReturnValue((Object)true);
        }
    }

    @Inject(method={"checkForRender()Z"}, at={@At(value="HEAD")}, cancellable=true, require=0, remap=false)
    private static void cinecraft$renderEveryFrame(CallbackInfoReturnable<Boolean> cir) {
        if (CinecraftClient.requiresUnthrottledRendering()) {
            cir.setReturnValue((Object)true);
        }
    }

    @Inject(method={"targetFrameRate()I"}, at={@At(value="HEAD")}, cancellable=true, require=0, remap=false)
    private static void cinecraft$restoreFrameRate(CallbackInfoReturnable<Integer> cir) {
        if (!CinecraftClient.requiresUnthrottledRendering()) {
            return;
        }
        class_310 client = class_310.method_1551();
        int configuredLimit = client == null ? 260 : (Integer)client.field_1690.method_42524().method_41753();
        cir.setReturnValue((Object)Math.max(15, configuredLimit));
    }
}


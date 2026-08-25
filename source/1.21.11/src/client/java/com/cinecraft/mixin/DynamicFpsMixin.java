package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Soft fail-safe for Dynamic FPS 3.x. FREX remains the primary integration,
 * while this guard covers both its frame limiter and skipped-render path.
 */
@Pseudo
@Mixin(targets = "dynamic_fps.impl.DynamicFPSMod", remap = false)
abstract class DynamicFpsMixin {
    @Inject(method = "isDisabled()Z", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void cinecraft$disableDynamicFps(CallbackInfoReturnable<Boolean> cir) {
        if (CinecraftClient.requiresUnthrottledRendering()) cir.setReturnValue(true);
    }

    @Inject(method = "checkForRender()Z", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void cinecraft$renderEveryFrame(CallbackInfoReturnable<Boolean> cir) {
        if (CinecraftClient.requiresUnthrottledRendering()) cir.setReturnValue(true);
    }

    @Inject(method = "targetFrameRate()I", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void cinecraft$restoreFrameRate(CallbackInfoReturnable<Integer> cir) {
        if (!CinecraftClient.requiresUnthrottledRendering()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int configuredLimit = client == null ? 260 : client.options.getMaxFps().getValue();
        cir.setReturnValue(Math.max(15, configuredLimit));
    }
}

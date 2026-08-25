package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import net.minecraft.client.option.InactivityFpsLimiter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps vanilla's AFK limiter from reducing an active cinematic's frame rate. */
@Mixin(InactivityFpsLimiter.class)
abstract class InactivityFpsLimiterMixin {
    @Inject(method = "getLimitReason", at = @At("HEAD"), cancellable = true)
    private void cinecraft$keepCinematicAtFullRate(
            CallbackInfoReturnable<InactivityFpsLimiter.LimitReason> cir
    ) {
        if (CinecraftClient.DIRECTOR.isActive()) {
            cir.setReturnValue(InactivityFpsLimiter.LimitReason.NONE);
        }
    }
}

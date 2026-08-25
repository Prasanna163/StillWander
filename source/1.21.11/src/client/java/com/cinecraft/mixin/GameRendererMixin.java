package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Prevents vanilla head bob and hurt tilt from being layered over a directed camera pose. */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void cinecraft$disableHurtTilt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (CinecraftClient.DIRECTOR.isActive()) ci.cancel();
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void cinecraft$disableViewBob(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (CinecraftClient.DIRECTOR.isActive()) ci.cancel();
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void cinecraft$applyShotFov(
            Camera camera,
            float tickDelta,
            boolean changingFov,
            CallbackInfoReturnable<Float> cir
    ) {
        float fov = CinecraftClient.DIRECTOR.currentFov();
        if (Float.isFinite(fov)) cir.setReturnValue(fov);
    }
}

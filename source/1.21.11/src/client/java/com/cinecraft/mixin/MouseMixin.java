package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
abstract class MouseMixin {
    @Inject(method = "onCursorPos", at = @At("HEAD"))
    private void cinecraft$move(long window, double x, double y, CallbackInfo ci) { CinecraftClient.activity(); }

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void cinecraft$click(long window, MouseInput input, int action, CallbackInfo ci) { CinecraftClient.activity(); }

    @Inject(method = "onMouseScroll", at = @At("HEAD"))
    private void cinecraft$scroll(long window, double horizontal, double vertical, CallbackInfo ci) { CinecraftClient.activity(); }
}

package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import net.minecraft.client.Keyboard;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
abstract class KeyboardMixin {
    @Inject(method = "onKey", at = @At("HEAD"))
    private void cinecraft$input(long window, int action, KeyInput input, CallbackInfo ci) {
        if (!CinecraftClient.isControlKey(input)) CinecraftClient.activity();
    }
}

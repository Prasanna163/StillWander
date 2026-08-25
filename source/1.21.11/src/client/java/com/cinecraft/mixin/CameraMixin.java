package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import com.cinecraft.camera.CameraPose;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
abstract class CameraMixin {
    @Shadow private boolean thirdPerson;
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void cinecraft$applyPose(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CameraPose pose = CinecraftClient.DIRECTOR.pose(tickDelta);
        if (pose == null) return;
        // WorldRenderer hides the focused entity for a first-person camera.
        // Mark only this rendered frame as third person so the player is visible.
        this.thirdPerson = true;
        setPos(pose.position().x, pose.position().y, pose.position().z);
        setRotation(pose.yaw(), pose.pitch());
    }
}

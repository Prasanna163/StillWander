/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1297
 *  net.minecraft.class_1937
 *  net.minecraft.class_4184
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.cinecraft.mixin;

import com.cinecraft.CinecraftClient;
import com.cinecraft.camera.CameraPose;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1937;
import net.minecraft.class_4184;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value={class_4184.class})
abstract class CameraMixin {
    @Shadow
    private boolean field_18719;

    CameraMixin() {
    }

    @Shadow
    protected abstract void method_19327(double var1, double var3, double var5);

    @Shadow
    protected abstract void method_19325(float var1, float var2);

    @Inject(method={"method_19321"}, at={@At(value="TAIL")})
    private void cinecraft$applyPose(class_1937 area, class_1297 focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CameraPose pose = CinecraftClient.DIRECTOR.pose(tickDelta);
        if (pose == null) {
            return;
        }
        this.field_18719 = true;
        this.method_19327(pose.position().field_1352, pose.position().field_1351, pose.position().field_1350);
        this.method_19325(pose.yaw(), pose.pitch());
    }
}


/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_243
 */
package com.cinecraft.camera;

import com.cinecraft.camera.CameraPose;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(value=EnvType.CLIENT)
public final class LookAt {
    private LookAt() {
    }

    public static CameraPose pose(class_243 camera, class_243 target) {
        class_243 delta = target.method_1020(camera);
        double horizontal = Math.sqrt(delta.field_1352 * delta.field_1352 + delta.field_1350 * delta.field_1350);
        float yaw = (float)Math.toDegrees(Math.atan2(-delta.field_1352, delta.field_1350));
        float pitch = (float)(-Math.toDegrees(Math.atan2(delta.field_1351, horizontal)));
        return new CameraPose(camera, yaw, pitch).withFocusDistance((float)delta.method_1033());
    }
}


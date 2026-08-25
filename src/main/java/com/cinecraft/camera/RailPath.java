/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_243
 */
package com.cinecraft.camera;

import com.cinecraft.camera.CameraPath;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(value=EnvType.CLIENT)
public record RailPath(class_243 start, class_243 end) implements CameraPath
{
    @Override
    public class_243 sample(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        return this.start.method_35590(this.end, clamped);
    }
}


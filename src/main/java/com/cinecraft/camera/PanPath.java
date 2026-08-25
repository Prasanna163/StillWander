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
public record PanPath(class_243 center, double startingAngle, double sweepRadians, double startingRadius, double endingRadius, double startingY, double endingY) implements CameraPath
{
    @Override
    public class_243 sample(double progress) {
        double clamped = Math.max(0.0, Math.min(1.0, progress));
        double angle = this.startingAngle + this.sweepRadians * clamped;
        double radius = PanPath.lerp(this.startingRadius, this.endingRadius, clamped);
        return new class_243(this.center.field_1352 + Math.cos(angle) * radius, PanPath.lerp(this.startingY, this.endingY, clamped), this.center.field_1350 + Math.sin(angle) * radius);
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }
}


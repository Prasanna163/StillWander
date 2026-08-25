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
import com.cinecraft.camera.CameraPose;
import com.cinecraft.camera.CinematicShot;
import com.cinecraft.camera.FovPath;
import com.cinecraft.camera.LookAt;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@Environment(value=EnvType.CLIENT)
public final class MovingShot
implements CinematicShot {
    private final CameraPath path;
    private final CameraPath focusPath;
    private final FovPath fovPath;
    private final long startedNanos = System.nanoTime();
    private final long durationNanos;

    public MovingShot(CameraPath path, class_243 target, long durationMillis) {
        this(path, progress -> target, FovPath.none(), durationMillis);
    }

    public MovingShot(CameraPath path, CameraPath focusPath, long durationMillis) {
        this(path, focusPath, FovPath.none(), durationMillis);
    }

    public MovingShot(CameraPath path, CameraPath focusPath, FovPath fovPath, long durationMillis) {
        this.path = path;
        this.focusPath = focusPath;
        this.fovPath = fovPath;
        this.durationNanos = durationMillis * 1000000L;
    }

    @Override
    public CameraPose sample(float tickDelta) {
        double progress = Math.min(1.0, (double)(System.nanoTime() - this.startedNanos) / (double)this.durationNanos);
        return LookAt.pose(this.path.sample(progress), this.focusPath.sample(progress)).withFov(this.fovPath.sample(progress));
    }

    @Override
    public boolean finished() {
        return System.nanoTime() - this.startedNanos >= this.durationNanos;
    }
}


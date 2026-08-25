/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.camera;

import com.cinecraft.camera.CameraPose;
import com.cinecraft.camera.CinematicShot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class StaticShot
implements CinematicShot {
    private final CameraPose pose;
    private final long endMillis;

    public StaticShot(CameraPose pose, long durationMillis) {
        this.pose = pose;
        this.endMillis = System.currentTimeMillis() + durationMillis;
    }

    @Override
    public CameraPose sample(float tickDelta) {
        return this.pose;
    }

    @Override
    public boolean finished() {
        return System.currentTimeMillis() >= this.endMillis;
    }
}


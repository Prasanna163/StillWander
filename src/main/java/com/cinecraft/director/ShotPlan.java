/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.director;

import com.cinecraft.camera.CameraPath;
import com.cinecraft.camera.FovPath;
import com.cinecraft.director.ShotComposition;
import com.cinecraft.director.ShotType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record ShotPlan(ShotType type, CameraPath path, CameraPath focusPath, CameraPath subjectPath, FovPath fovPath, long durationMillis, ShotComposition composition, String source) {
}


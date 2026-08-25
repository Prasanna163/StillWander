/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_243
 */
package com.cinecraft.camera;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;

@FunctionalInterface
@Environment(value=EnvType.CLIENT)
public interface CameraPath {
    public class_243 sample(double var1);
}


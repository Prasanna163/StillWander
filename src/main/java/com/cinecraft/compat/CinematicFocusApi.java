/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.compat;

import com.cinecraft.CinecraftClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class CinematicFocusApi {
    private CinematicFocusApi() {
    }

    public static boolean active() {
        return CinecraftClient.DIRECTOR.isActive();
    }

    public static float focusDistance() {
        return CinecraftClient.DIRECTOR.currentFocusDistance();
    }

    public static float fieldOfView() {
        return CinecraftClient.DIRECTOR.currentFov();
    }
}


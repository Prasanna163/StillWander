package com.cinecraft.compat;

import com.cinecraft.CinecraftClient;

/** Stable zero-dependency hook for shader packs or companion render integrations. */
public final class CinematicFocusApi {
    private CinematicFocusApi() { }

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

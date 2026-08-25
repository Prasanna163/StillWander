package com.cinecraft.compat;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Optional FREX/Dynamic FPS bridge. Dynamic FPS discovers this entrypoint and
 * supplies a per-mod switch; no compile-time dependency on Dynamic FPS is needed.
 */
public final class CinecraftFlawlessFrames implements Consumer<Function<String, Consumer<Boolean>>> {
    private static Consumer<Boolean> switcher;
    private static boolean requested;

    @Override
    public void accept(Function<String, Consumer<Boolean>> provider) {
        switcher = provider.apply("Still Wander cinematic camera");
        switcher.accept(requested);
    }

    /** Requests normal rendering only for the lifetime of an active cinematic. */
    public static void setActive(boolean active) {
        if (requested == active) return;
        requested = active;
        if (switcher != null) switcher.accept(active);
    }

    public static boolean isRegistered() {
        return switcher != null;
    }

    public static boolean isRequested() {
        return requested;
    }
}

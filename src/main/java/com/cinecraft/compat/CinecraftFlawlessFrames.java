/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.compat;

import java.util.function.Consumer;
import java.util.function.Function;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public final class CinecraftFlawlessFrames
implements Consumer<Function<String, Consumer<Boolean>>> {
    private static Consumer<Boolean> switcher;
    private static boolean requested;

    @Override
    public void accept(Function<String, Consumer<Boolean>> provider) {
        switcher = provider.apply("Cinecraft cinematic camera");
        switcher.accept(requested);
    }

    public static void setActive(boolean active) {
        if (requested == active) {
            return;
        }
        requested = active;
        if (switcher != null) {
            switcher.accept(active);
        }
    }

    public static boolean isRegistered() {
        return switcher != null;
    }

    public static boolean isRequested() {
        return requested;
    }
}


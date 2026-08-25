/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_310
 *  net.minecraft.class_327
 *  net.minecraft.class_332
 */
package com.cinecraft.debug;

import com.cinecraft.CinecraftClient;
import com.cinecraft.compat.CinecraftFlawlessFrames;
import com.cinecraft.compat.RendererCompatibility;
import com.cinecraft.config.CinecraftConfig;
import com.cinecraft.director.CinematicDirector;
import com.cinecraft.director.EnvironmentProfile;
import java.util.ArrayList;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;

@Environment(value=EnvType.CLIENT)
public final class CinecraftDebugHud {
    private CinecraftDebugHud() {
    }

    public static void register() {
        HudElementRegistry.addLast((class_2960)class_2960.method_60655((String)"cinecraft", (String)"director_debug"), (context, tickCounter) -> CinecraftDebugHud.render(context));
    }

    private static void render(class_332 context) {
        if (!CinecraftConfig.INSTANCE.debugOverlay() || !CinecraftClient.DIRECTOR.isActive()) {
            return;
        }
        class_310 client = class_310.method_1551();
        CinematicDirector director = CinecraftClient.DIRECTOR;
        EnvironmentProfile profile = director.currentProfile();
        ArrayList<Object> lines = new ArrayList<Object>();
        lines.add("CINECRAFT DIRECTOR");
        lines.add("mode  " + (CinecraftClient.isRecordingMode() ? "capture" : "cinematic") + " / shaders " + (RendererCompatibility.shadersActive() ? "on" : "off") + " / DH " + (RendererCompatibility.distantHorizonsAvailable() ? "yes" : "no"));
        if (RendererCompatibility.dynamicFpsAvailable()) {
            lines.add("fps   Dynamic FPS bypass " + (CinecraftClient.requiresUnthrottledRendering() ? "active" : "waiting") + " / FREX " + (CinecraftFlawlessFrames.isRegistered() ? "linked" : "missing"));
        }
        lines.add("shot  " + CinecraftDebugHud.value((Object)director.currentShotType()) + " / " + CinecraftDebugHud.value((Object)director.currentSubjectType()));
        lines.add("frame " + CinecraftDebugHud.value(director.currentComposition()) + " / action " + CinecraftDebugHud.value((Object)director.currentAction()));
        lines.add(String.format(Locale.ROOT, "lens  %.1f\u00b0 / focus %.1fm", Float.valueOf(director.currentFov()), Float.valueOf(director.currentFocusDistance())));
        if (profile != null) {
            lines.add("scene " + String.valueOf((Object)profile.spaceType()) + " / " + String.valueOf((Object)profile.biomeMood()) + " / " + String.valueOf((Object)profile.weather()) + " / " + String.valueOf((Object)profile.sceneTime()));
        }
        lines.add("path  " + director.plannerSource());
        lines.add("score " + director.plannerDecision() + " / rejected " + director.plannerRejected());
        int width = lines.stream().mapToInt(arg_0 -> ((class_327)client.field_1772).method_1727(arg_0)).max().orElse(150) + 12;
        int height = lines.size() * 11 + 9;
        context.method_25294(6, 6, 6 + width, 6 + height, -1341123301);
        for (int index = 0; index < lines.size(); ++index) {
            int color = index == 0 ? -8922625 : -1511694;
            context.method_27535(client.field_1772, (class_2561)class_2561.method_43470((String)((String)lines.get(index))), 12, 12 + index * 11, color);
        }
    }

    private static String value(Object value) {
        return value == null ? "none" : value.toString();
    }
}


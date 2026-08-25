package com.cinecraft.debug;

import com.cinecraft.CinecraftClient;
import com.cinecraft.compat.CinecraftFlawlessFrames;
import com.cinecraft.compat.RendererCompatibility;
import com.cinecraft.config.CinecraftConfig;
import com.cinecraft.director.CinematicDirector;
import com.cinecraft.director.EnvironmentProfile;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Small opt-in director readout for path tuning and compatibility reports. */
public final class CinecraftDebugHud {
    private CinecraftDebugHud() { }

    public static void register() {
        HudElementRegistry.addLast(
                Identifier.of("stillwander", "director_debug"),
                (context, tickCounter) -> render(context)
        );
    }

    private static void render(DrawContext context) {
        if (!CinecraftConfig.INSTANCE.debugOverlay() || !CinecraftClient.DIRECTOR.isActive()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        CinematicDirector director = CinecraftClient.DIRECTOR;
        EnvironmentProfile profile = director.currentProfile();
        List<String> lines = new ArrayList<>();
        lines.add("STILL WANDER DIRECTOR");
        lines.add("mode  " + (CinecraftClient.isRecordingMode() ? "capture" : "cinematic")
                + " / shaders " + (RendererCompatibility.shadersActive() ? "on" : "off")
                + " / DH " + (RendererCompatibility.distantHorizonsAvailable() ? "yes" : "no"));
        if (RendererCompatibility.dynamicFpsAvailable()) {
            lines.add("fps   Dynamic FPS bypass "
                    + (CinecraftClient.requiresUnthrottledRendering() ? "active" : "waiting")
                    + " / FREX " + (CinecraftFlawlessFrames.isRegistered() ? "linked" : "missing"));
        }
        lines.add("shot  " + value(director.currentShotType()) + " / " + value(director.currentSubjectType()));
        lines.add("frame " + value(director.currentComposition()) + " / action " + value(director.currentAction()));
        lines.add(String.format(
                Locale.ROOT,
                "lens  %.1f° / focus %.1fm",
                director.currentFov(),
                director.currentFocusDistance()
        ));
        if (profile != null) {
            lines.add("scene " + profile.spaceType() + " / " + profile.biomeMood()
                    + " / " + profile.weather() + " / " + profile.sceneTime());
        }
        lines.add("path  " + director.plannerSource());
        lines.add("score " + director.plannerDecision() + " / rejected " + director.plannerRejected());

        int width = lines.stream().mapToInt(client.textRenderer::getWidth).max().orElse(150) + 12;
        int height = lines.size() * 11 + 9;
        context.fill(6, 6, 6 + width, 6 + height, 0xB010151B);
        for (int index = 0; index < lines.size(); index++) {
            int color = index == 0 ? 0xFF77D9FF : 0xFFE8EEF2;
            context.drawTextWithShadow(client.textRenderer, Text.literal(lines.get(index)), 12, 12 + index * 11, color);
        }
    }

    private static String value(Object value) {
        return value == null ? "none" : value.toString();
    }
}

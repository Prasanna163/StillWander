package com.cinecraft.config;

import com.cinecraft.CinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Dependency-free vanilla settings screen, opened by the configurable F7 key. */
public final class CinecraftSettingsScreen extends Screen {
    private static final int BUTTON_WIDTH = 154;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 24;

    private final Screen parent;
    private final CinecraftConfig config = CinecraftConfig.INSTANCE;
    private int page;

    public CinecraftSettingsScreen(Screen parent) {
        super(Text.literal("Still Wander Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = width / 2 - BUTTON_WIDTH - 4;
        int right = width / 2 + 4;
        int top = 48;
        if (page == 0) {
            addCycle(left, top, "Idle delay", () -> config.idleSeconds() + "s", () -> {
                config.idleSeconds(next(config.idleSeconds(), new int[]{10, 15, 25, 45, 60, 120}));
                CinecraftClient.applyConfiguration();
            });
            addCycle(right, top, "Path quality", () -> pretty(config.quality()), () ->
                    config.quality(next(config.quality(), QualityPreset.values())));
            addCycle(left, top + ROW_GAP, "Shot length", () -> format(config.shotLengthMultiplier()), () ->
                    config.shotLengthMultiplier(next(config.shotLengthMultiplier(), new double[]{0.75, 1.0, 1.25, 1.5})));
            addCycle(right, top + ROW_GAP, "Camera speed", () -> format(config.cameraSpeed()), () ->
                    config.cameraSpeed(next(config.cameraSpeed(), new double[]{0.65, 0.85, 1.0, 1.2, 1.4})));
            addCycle(left, top + ROW_GAP * 2, "Zoom strength", () -> format(config.zoomStrength()), () ->
                    config.zoomStrength(next(config.zoomStrength(), new double[]{0.0, 0.5, 1.0, 1.35, 1.7})));
            addCycle(right, top + ROW_GAP * 2, "Screenshots", () -> config.screenshotIntervalSeconds() == 0
                    ? "Off"
                    : config.screenshotIntervalSeconds() + "s", () -> config.screenshotIntervalSeconds(next(
                    config.screenshotIntervalSeconds(),
                    new int[]{0, 10, 20, 30, 60}
            )));
            addToggle(left, top + ROW_GAP * 3, "Hide HUD", config::hideHud, config::hideHud);
            addToggle(right, top + ROW_GAP * 3, "Exit on damage", config::exitOnDamage, config::exitOnDamage);
            addToggle(left, top + ROW_GAP * 4, "Focus effects", config::focusEffects, config::focusEffects);
            addToggle(right, top + ROW_GAP * 4, "Debug overlay", config::debugOverlay, config::debugOverlay);
        } else {
            addToggle(left, top, "Player shots", config::playerShots, config::playerShots);
            addToggle(right, top, "Armor/item details", config::playerDetailShots, config::playerDetailShots);
            addToggle(left, top + ROW_GAP, "Entity shots", config::entityShots, config::entityShots);
            addToggle(right, top + ROW_GAP, "Multi-subject shots", config::groupShots, config::groupShots);
            addToggle(left, top + ROW_GAP * 2, "Feature shots", config::featureShots, config::featureShots);
            addToggle(right, top + ROW_GAP * 2, "Landscape shots", config::landscapeShots, config::landscapeShots);
            addToggle(left, top + ROW_GAP * 3, "Aerial shots", config::aerialShots, config::aerialShots);
            addToggle(right, top + ROW_GAP * 3, "Cave/interior shots", config::interiorShots, config::interiorShots);
        }

        int bottom = height - 28;
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> {
            config.resetDefaults();
            CinecraftClient.applyConfiguration();
            clearAndInit();
        }).dimensions(width / 2 - 154, bottom, 98, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(
                Text.literal(page == 0 ? "Shot Types →" : "← Camera"),
                button -> {
                    page = 1 - page;
                    clearAndInit();
                }
        ).dimensions(width / 2 - 50, bottom, 100, BUTTON_HEIGHT).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close())
                .dimensions(width / 2 + 56, bottom, 98, BUTTON_HEIGHT)
                .build());
    }

    private void addCycle(int x, int y, String name, Supplier<String> value, Runnable cycle) {
        addDrawableChild(ButtonWidget.builder(label(name, value.get()), button -> {
            cycle.run();
            config.save();
            button.setMessage(label(name, value.get()));
        }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private void addToggle(
            int x,
            int y,
            String name,
            BooleanSupplier getter,
            Consumer<Boolean> setter
    ) {
        addDrawableChild(ButtonWidget.builder(label(name, onOff(getter.getAsBoolean())), button -> {
            setter.accept(!getter.getAsBoolean());
            config.save();
            button.setMessage(label(name, onOff(getter.getAsBoolean())));
        }).dimensions(x, y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 16, 0xFFFFFF);
        context.drawCenteredTextWithShadow(
                textRenderer,
                page == 0 ? "Camera and capture" : "Enabled shot categories",
                width / 2,
                31,
                0xA8B8C8
        );
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        config.save();
        CinecraftClient.applyConfiguration();
        if (client != null) client.setScreen(parent);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static Text label(String name, String value) {
        return Text.literal(name + ": " + value);
    }

    private static String onOff(boolean value) { return value ? "On" : "Off"; }
    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.2fx", value); }
    private static String pretty(Enum<?> value) {
        String text = value.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static int next(int current, int[] values) {
        for (int index = 0; index < values.length; index++) {
            if (values[index] == current) return values[(index + 1) % values.length];
        }
        return values[0];
    }

    private static double next(double current, double[] values) {
        for (int index = 0; index < values.length; index++) {
            if (Math.abs(values[index] - current) < 0.001) return values[(index + 1) % values.length];
        }
        return values[0];
    }

    private static <T> T next(T current, T[] values) {
        for (int index = 0; index < values.length; index++) {
            if (values[index].equals(current)) return values[(index + 1) % values.length];
        }
        return values[0];
    }
}

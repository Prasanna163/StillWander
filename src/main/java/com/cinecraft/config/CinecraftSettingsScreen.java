/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_2561
 *  net.minecraft.class_332
 *  net.minecraft.class_364
 *  net.minecraft.class_4185
 *  net.minecraft.class_437
 */
package com.cinecraft.config;

import com.cinecraft.CinecraftClient;
import com.cinecraft.config.CinecraftConfig;
import com.cinecraft.config.QualityPreset;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;

@Environment(value=EnvType.CLIENT)
public final class CinecraftSettingsScreen
extends class_437 {
    private static final int BUTTON_WIDTH = 154;
    private static final int BUTTON_HEIGHT = 20;
    private static final int ROW_GAP = 24;
    private final class_437 parent;
    private final CinecraftConfig config = CinecraftConfig.INSTANCE;
    private int page;

    public CinecraftSettingsScreen(class_437 parent) {
        super((class_2561)class_2561.method_43470((String)"Cinecraft Settings"));
        this.parent = parent;
    }

    protected void method_25426() {
        int left = this.field_22789 / 2 - 154 - 4;
        int right = this.field_22789 / 2 + 4;
        int top = 48;
        if (this.page == 0) {
            this.addCycle(left, top, "Idle delay", () -> this.config.idleSeconds() + "s", () -> {
                this.config.idleSeconds(CinecraftSettingsScreen.next(this.config.idleSeconds(), new int[]{10, 15, 25, 45, 60, 120}));
                CinecraftClient.applyConfiguration();
            });
            this.addCycle(right, top, "Path quality", () -> CinecraftSettingsScreen.pretty(this.config.quality()), () -> this.config.quality(CinecraftSettingsScreen.next(this.config.quality(), QualityPreset.values())));
            this.addCycle(left, top + 24, "Shot length", () -> CinecraftSettingsScreen.format(this.config.shotLengthMultiplier()), () -> this.config.shotLengthMultiplier(CinecraftSettingsScreen.next(this.config.shotLengthMultiplier(), new double[]{0.75, 1.0, 1.25, 1.5})));
            this.addCycle(right, top + 24, "Camera speed", () -> CinecraftSettingsScreen.format(this.config.cameraSpeed()), () -> this.config.cameraSpeed(CinecraftSettingsScreen.next(this.config.cameraSpeed(), new double[]{0.65, 0.85, 1.0, 1.2, 1.4})));
            this.addCycle(left, top + 48, "Zoom strength", () -> CinecraftSettingsScreen.format(this.config.zoomStrength()), () -> this.config.zoomStrength(CinecraftSettingsScreen.next(this.config.zoomStrength(), new double[]{0.0, 0.5, 1.0, 1.35, 1.7})));
            this.addCycle(right, top + 48, "Screenshots", () -> this.config.screenshotIntervalSeconds() == 0 ? "Off" : this.config.screenshotIntervalSeconds() + "s", () -> this.config.screenshotIntervalSeconds(CinecraftSettingsScreen.next(this.config.screenshotIntervalSeconds(), new int[]{0, 10, 20, 30, 60})));
            this.addToggle(left, top + 72, "Hide HUD", this.config::hideHud, this.config::hideHud);
            this.addToggle(right, top + 72, "Exit on damage", this.config::exitOnDamage, this.config::exitOnDamage);
            this.addToggle(left, top + 96, "Focus effects", this.config::focusEffects, this.config::focusEffects);
            this.addToggle(right, top + 96, "Debug overlay", this.config::debugOverlay, this.config::debugOverlay);
        } else {
            this.addToggle(left, top, "Player shots", this.config::playerShots, this.config::playerShots);
            this.addToggle(right, top, "Armor/item details", this.config::playerDetailShots, this.config::playerDetailShots);
            this.addToggle(left, top + 24, "Entity shots", this.config::entityShots, this.config::entityShots);
            this.addToggle(right, top + 24, "Multi-subject shots", this.config::groupShots, this.config::groupShots);
            this.addToggle(left, top + 48, "Feature shots", this.config::featureShots, this.config::featureShots);
            this.addToggle(right, top + 48, "Landscape shots", this.config::landscapeShots, this.config::landscapeShots);
            this.addToggle(left, top + 72, "Aerial shots", this.config::aerialShots, this.config::aerialShots);
            this.addToggle(right, top + 72, "Cave/interior shots", this.config::interiorShots, this.config::interiorShots);
        }
        int bottom = this.field_22790 - 28;
        this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43470((String)"Reset"), button -> {
            this.config.resetDefaults();
            CinecraftClient.applyConfiguration();
            this.method_41843();
        }).method_46434(this.field_22789 / 2 - 154, bottom, 98, 20).method_46431());
        this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43470((String)(this.page == 0 ? "Shot Types \u2192" : "\u2190 Camera")), button -> {
            this.page = 1 - this.page;
            this.method_41843();
        }).method_46434(this.field_22789 / 2 - 50, bottom, 100, 20).method_46431());
        this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43470((String)"Done"), button -> this.method_25419()).method_46434(this.field_22789 / 2 + 56, bottom, 98, 20).method_46431());
    }

    private void addCycle(int x, int y, String name, Supplier<String> value, Runnable cycle) {
        this.method_37063((class_364)class_4185.method_46430((class_2561)CinecraftSettingsScreen.label(name, value.get()), button -> {
            cycle.run();
            this.config.save();
            button.method_25355(CinecraftSettingsScreen.label(name, (String)value.get()));
        }).method_46434(x, y, 154, 20).method_46431());
    }

    private void addToggle(int x, int y, String name, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.method_37063((class_364)class_4185.method_46430((class_2561)CinecraftSettingsScreen.label(name, CinecraftSettingsScreen.onOff(getter.getAsBoolean())), button -> {
            setter.accept(!getter.getAsBoolean());
            this.config.save();
            button.method_25355(CinecraftSettingsScreen.label(name, CinecraftSettingsScreen.onOff(getter.getAsBoolean())));
        }).method_46434(x, y, 154, 20).method_46431());
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.method_25420(context, mouseX, mouseY, delta);
        context.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 16, 0xFFFFFF);
        context.method_25300(this.field_22793, this.page == 0 ? "Camera and capture" : "Enabled shot categories", this.field_22789 / 2, 31, 11057352);
        super.method_25394(context, mouseX, mouseY, delta);
    }

    public void method_25419() {
        this.config.save();
        CinecraftClient.applyConfiguration();
        if (this.field_22787 != null) {
            this.field_22787.method_1507(this.parent);
        }
    }

    public boolean method_25421() {
        return false;
    }

    private static class_2561 label(String name, String value) {
        return class_2561.method_43470((String)(name + ": " + value));
    }

    private static String onOff(boolean value) {
        return value ? "On" : "Off";
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2fx", value);
    }

    private static String pretty(Enum<?> value) {
        String text = value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    private static int next(int current, int[] values) {
        for (int index = 0; index < values.length; ++index) {
            if (values[index] != current) continue;
            return values[(index + 1) % values.length];
        }
        return values[0];
    }

    private static double next(double current, double[] values) {
        for (int index = 0; index < values.length; ++index) {
            if (!(Math.abs(values[index] - current) < 0.001)) continue;
            return values[(index + 1) % values.length];
        }
        return values[0];
    }

    private static <T> T next(T current, T[] values) {
        for (int index = 0; index < values.length; ++index) {
            if (!values[index].equals(current)) continue;
            return values[(index + 1) % values.length];
        }
        return values[0];
    }
}


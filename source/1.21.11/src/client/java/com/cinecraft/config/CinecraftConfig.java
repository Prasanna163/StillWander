package com.cinecraft.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent user-facing settings with conservative bounds for camera safety. */
public final class CinecraftConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("stillwander.json");
    public static final CinecraftConfig INSTANCE = load();

    private int idleSeconds = 25;
    private QualityPreset quality = QualityPreset.BALANCED;
    private double shotLengthMultiplier = 1.0;
    private double cameraSpeed = 1.0;
    private double zoomStrength = 1.0;
    private boolean playerShots = true;
    private boolean playerDetailShots = true;
    private boolean entityShots = true;
    private boolean groupShots = true;
    private boolean featureShots = true;
    private boolean landscapeShots = true;
    private boolean aerialShots = true;
    private boolean interiorShots = true;
    private boolean hideHud = true;
    private boolean exitOnDamage = true;
    private boolean debugOverlay = false;
    private boolean focusEffects = true;
    private int screenshotIntervalSeconds = 0;

    private static CinecraftConfig load() {
        if (Files.isRegularFile(PATH)) {
            try (Reader reader = Files.newBufferedReader(PATH)) {
                CinecraftConfig loaded = GSON.fromJson(reader, CinecraftConfig.class);
                if (loaded != null) {
                    loaded.sanitize();
                    return loaded;
                }
            } catch (IOException | RuntimeException ignored) {
                // A malformed user file falls back to safe defaults and is replaced on save.
            }
        }
        return new CinecraftConfig();
    }

    public synchronized void save() {
        sanitize();
        try {
            Files.createDirectories(PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
            // Configuration failure must never take down the render thread.
        }
    }

    public void resetDefaults() {
        idleSeconds = 25;
        quality = QualityPreset.BALANCED;
        shotLengthMultiplier = 1.0;
        cameraSpeed = 1.0;
        zoomStrength = 1.0;
        playerShots = true;
        playerDetailShots = true;
        entityShots = true;
        groupShots = true;
        featureShots = true;
        landscapeShots = true;
        aerialShots = true;
        interiorShots = true;
        hideHud = true;
        exitOnDamage = true;
        debugOverlay = false;
        focusEffects = true;
        screenshotIntervalSeconds = 0;
        save();
    }

    private void sanitize() {
        idleSeconds = clamp(idleSeconds, 5, 600);
        if (quality == null) quality = QualityPreset.BALANCED;
        shotLengthMultiplier = clamp(shotLengthMultiplier, 0.5, 2.0);
        cameraSpeed = clamp(cameraSpeed, 0.5, 1.6);
        zoomStrength = clamp(zoomStrength, 0.0, 1.8);
        screenshotIntervalSeconds = screenshotIntervalSeconds <= 0
                ? 0
                : clamp(screenshotIntervalSeconds, 5, 600);
    }

    public long effectiveDuration(long baseMillis) {
        return Math.round(baseMillis * shotLengthMultiplier / cameraSpeed);
    }

    public int idleSeconds() { return idleSeconds; }
    public void idleSeconds(int value) { idleSeconds = value; }
    public QualityPreset quality() { return quality; }
    public void quality(QualityPreset value) { quality = value; }
    public double shotLengthMultiplier() { return shotLengthMultiplier; }
    public void shotLengthMultiplier(double value) { shotLengthMultiplier = value; }
    public double cameraSpeed() { return cameraSpeed; }
    public void cameraSpeed(double value) { cameraSpeed = value; }
    public double zoomStrength() { return zoomStrength; }
    public void zoomStrength(double value) { zoomStrength = value; }
    public boolean playerShots() { return playerShots; }
    public void playerShots(boolean value) { playerShots = value; }
    public boolean playerDetailShots() { return playerDetailShots; }
    public void playerDetailShots(boolean value) { playerDetailShots = value; }
    public boolean entityShots() { return entityShots; }
    public void entityShots(boolean value) { entityShots = value; }
    public boolean groupShots() { return groupShots; }
    public void groupShots(boolean value) { groupShots = value; }
    public boolean featureShots() { return featureShots; }
    public void featureShots(boolean value) { featureShots = value; }
    public boolean landscapeShots() { return landscapeShots; }
    public void landscapeShots(boolean value) { landscapeShots = value; }
    public boolean aerialShots() { return aerialShots; }
    public void aerialShots(boolean value) { aerialShots = value; }
    public boolean interiorShots() { return interiorShots; }
    public void interiorShots(boolean value) { interiorShots = value; }
    public boolean hideHud() { return hideHud; }
    public void hideHud(boolean value) { hideHud = value; }
    public boolean exitOnDamage() { return exitOnDamage; }
    public void exitOnDamage(boolean value) { exitOnDamage = value; }
    public boolean debugOverlay() { return debugOverlay; }
    public void debugOverlay(boolean value) { debugOverlay = value; }
    public boolean focusEffects() { return focusEffects; }
    public void focusEffects(boolean value) { focusEffects = value; }
    public int screenshotIntervalSeconds() { return screenshotIntervalSeconds; }
    public void screenshotIntervalSeconds(int value) { screenshotIntervalSeconds = value; }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}

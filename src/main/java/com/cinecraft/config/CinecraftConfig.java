/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.loader.api.FabricLoader
 */
package com.cinecraft.config;

import com.cinecraft.config.QualityPreset;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(value=EnvType.CLIENT)
public final class CinecraftConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("cinecraft.json");
    public static final CinecraftConfig INSTANCE = CinecraftConfig.load();
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

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static CinecraftConfig load() {
        if (!Files.isRegularFile(PATH, new LinkOption[0])) return new CinecraftConfig();
        try (BufferedReader reader = Files.newBufferedReader(PATH);){
            CinecraftConfig loaded = (CinecraftConfig)GSON.fromJson((Reader)reader, CinecraftConfig.class);
            if (loaded == null) return new CinecraftConfig();
            loaded.sanitize();
            CinecraftConfig cinecraftConfig = loaded;
            return cinecraftConfig;
        }
        catch (IOException | RuntimeException exception) {
            // empty catch block
        }
        return new CinecraftConfig();
    }

    public synchronized void save() {
        this.sanitize();
        try {
            Files.createDirectories(PATH.getParent(), new FileAttribute[0]);
            try (BufferedWriter writer = Files.newBufferedWriter(PATH, new OpenOption[0]);){
                GSON.toJson((Object)this, (Appendable)writer);
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public void resetDefaults() {
        this.idleSeconds = 25;
        this.quality = QualityPreset.BALANCED;
        this.shotLengthMultiplier = 1.0;
        this.cameraSpeed = 1.0;
        this.zoomStrength = 1.0;
        this.playerShots = true;
        this.playerDetailShots = true;
        this.entityShots = true;
        this.groupShots = true;
        this.featureShots = true;
        this.landscapeShots = true;
        this.aerialShots = true;
        this.interiorShots = true;
        this.hideHud = true;
        this.exitOnDamage = true;
        this.debugOverlay = false;
        this.focusEffects = true;
        this.screenshotIntervalSeconds = 0;
        this.save();
    }

    private void sanitize() {
        this.idleSeconds = CinecraftConfig.clamp(this.idleSeconds, 5, 600);
        if (this.quality == null) {
            this.quality = QualityPreset.BALANCED;
        }
        this.shotLengthMultiplier = CinecraftConfig.clamp(this.shotLengthMultiplier, 0.5, 2.0);
        this.cameraSpeed = CinecraftConfig.clamp(this.cameraSpeed, 0.5, 1.6);
        this.zoomStrength = CinecraftConfig.clamp(this.zoomStrength, 0.0, 1.8);
        this.screenshotIntervalSeconds = this.screenshotIntervalSeconds <= 0 ? 0 : CinecraftConfig.clamp(this.screenshotIntervalSeconds, 5, 600);
    }

    public long effectiveDuration(long baseMillis) {
        return Math.round((double)baseMillis * this.shotLengthMultiplier / this.cameraSpeed);
    }

    public int idleSeconds() {
        return this.idleSeconds;
    }

    public void idleSeconds(int value) {
        this.idleSeconds = value;
    }

    public QualityPreset quality() {
        return this.quality;
    }

    public void quality(QualityPreset value) {
        this.quality = value;
    }

    public double shotLengthMultiplier() {
        return this.shotLengthMultiplier;
    }

    public void shotLengthMultiplier(double value) {
        this.shotLengthMultiplier = value;
    }

    public double cameraSpeed() {
        return this.cameraSpeed;
    }

    public void cameraSpeed(double value) {
        this.cameraSpeed = value;
    }

    public double zoomStrength() {
        return this.zoomStrength;
    }

    public void zoomStrength(double value) {
        this.zoomStrength = value;
    }

    public boolean playerShots() {
        return this.playerShots;
    }

    public void playerShots(boolean value) {
        this.playerShots = value;
    }

    public boolean playerDetailShots() {
        return this.playerDetailShots;
    }

    public void playerDetailShots(boolean value) {
        this.playerDetailShots = value;
    }

    public boolean entityShots() {
        return this.entityShots;
    }

    public void entityShots(boolean value) {
        this.entityShots = value;
    }

    public boolean groupShots() {
        return this.groupShots;
    }

    public void groupShots(boolean value) {
        this.groupShots = value;
    }

    public boolean featureShots() {
        return this.featureShots;
    }

    public void featureShots(boolean value) {
        this.featureShots = value;
    }

    public boolean landscapeShots() {
        return this.landscapeShots;
    }

    public void landscapeShots(boolean value) {
        this.landscapeShots = value;
    }

    public boolean aerialShots() {
        return this.aerialShots;
    }

    public void aerialShots(boolean value) {
        this.aerialShots = value;
    }

    public boolean interiorShots() {
        return this.interiorShots;
    }

    public void interiorShots(boolean value) {
        this.interiorShots = value;
    }

    public boolean hideHud() {
        return this.hideHud;
    }

    public void hideHud(boolean value) {
        this.hideHud = value;
    }

    public boolean exitOnDamage() {
        return this.exitOnDamage;
    }

    public void exitOnDamage(boolean value) {
        this.exitOnDamage = value;
    }

    public boolean debugOverlay() {
        return this.debugOverlay;
    }

    public void debugOverlay(boolean value) {
        this.debugOverlay = value;
    }

    public boolean focusEffects() {
        return this.focusEffects;
    }

    public void focusEffects(boolean value) {
        this.focusEffects = value;
    }

    public int screenshotIntervalSeconds() {
        return this.screenshotIntervalSeconds;
    }

    public void screenshotIntervalSeconds(int value) {
        this.screenshotIntervalSeconds = value;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}


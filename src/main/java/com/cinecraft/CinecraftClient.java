/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
 *  net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
 *  net.minecraft.class_11908
 *  net.minecraft.class_1657
 *  net.minecraft.class_2960
 *  net.minecraft.class_304
 *  net.minecraft.class_304$class_11900
 *  net.minecraft.class_310
 *  net.minecraft.class_3675$class_307
 *  net.minecraft.class_437
 */
package com.cinecraft;

import com.cinecraft.capture.CinematicRecorder;
import com.cinecraft.compat.CinecraftFlawlessFrames;
import com.cinecraft.config.CinecraftConfig;
import com.cinecraft.config.CinecraftSettingsScreen;
import com.cinecraft.debug.CinecraftDebugHud;
import com.cinecraft.director.CinematicDirector;
import com.cinecraft.idle.DamageMonitor;
import com.cinecraft.idle.IdleDetector;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.class_11908;
import net.minecraft.class_1657;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;

@Environment(value=EnvType.CLIENT)
public final class CinecraftClient
implements ClientModInitializer {
    public static final IdleDetector IDLE = new IdleDetector(25000L);
    public static final CinematicDirector DIRECTOR = new CinematicDirector();
    private static final DamageMonitor DAMAGE = new DamageMonitor();
    private static final CinematicRecorder RECORDER = new CinematicRecorder();
    private static final class_304.class_11900 CONTROLS = class_304.class_11900.method_74698((class_2960)class_2960.method_60655((String)"cinecraft", (String)"controls"));
    private static class_304 toggleCinematic;
    private static class_304 openSettings;
    private static class_304 toggleRecording;
    private static class_304 toggleDebug;
    private static class_304 nextShot;
    private static boolean manualActivation;
    private static boolean recordingMode;

    public static void activity() {
        if (recordingMode) {
            return;
        }
        CinecraftClient.stopCinematic(true);
    }

    private static void stopCinematic(boolean resetIdle) {
        manualActivation = false;
        recordingMode = false;
        RECORDER.stop();
        if (resetIdle) {
            IDLE.activity();
        }
        DIRECTOR.stop();
    }

    public static boolean isControlKey(class_11908 input) {
        return CinecraftClient.matches(toggleCinematic, input) || CinecraftClient.matches(openSettings, input) || CinecraftClient.matches(toggleRecording, input) || CinecraftClient.matches(toggleDebug, input) || CinecraftClient.matches(nextShot, input);
    }

    private static boolean matches(class_304 binding, class_11908 input) {
        return binding != null && binding.method_1417(input);
    }

    public static void applyConfiguration() {
        IDLE.setTimeoutMillis((long)CinecraftConfig.INSTANCE.idleSeconds() * 1000L);
    }

    public static boolean isRecordingMode() {
        return recordingMode;
    }

    public static boolean requiresUnthrottledRendering() {
        return recordingMode || manualActivation || IDLE.isIdle() || DIRECTOR.isActive();
    }

    public void onInitializeClient() {
        toggleCinematic = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.cinecraft.toggle_cinematic", class_3675.class_307.field_1668, 66, CONTROLS));
        openSettings = CinecraftClient.register("key.cinecraft.open_settings", 296);
        toggleRecording = CinecraftClient.register("key.cinecraft.toggle_recording", 297);
        toggleDebug = CinecraftClient.register("key.cinecraft.toggle_debug", 298);
        nextShot = CinecraftClient.register("key.cinecraft.next_shot", 78);
        CinecraftClient.applyConfiguration();
        CinecraftDebugHud.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.field_1724 == null || client.field_1687 == null) {
                CinecraftClient.drainControlPresses();
                manualActivation = false;
                recordingMode = false;
                RECORDER.stop();
                DAMAGE.reset();
                DIRECTOR.stop();
                return;
            }
            CinecraftClient.handleControls(client);
            boolean damaged = DAMAGE.update((class_1657)client.field_1724);
            if (damaged && CinecraftConfig.INSTANCE.exitOnDamage()) {
                CinecraftClient.stopCinematic(true);
                return;
            }
            boolean cinematicRequested = recordingMode || manualActivation || IDLE.isIdle();
            CinecraftFlawlessFrames.setActive(cinematicRequested);
            if (cinematicRequested) {
                DIRECTOR.tick(client);
                RECORDER.tick(client, DIRECTOR);
                if (recordingMode && !RECORDER.isRecording()) {
                    CinecraftClient.stopCinematic(true);
                }
            } else {
                DIRECTOR.stop();
            }
        });
    }

    private static class_304 register(String id, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding((class_304)new class_304(id, class_3675.class_307.field_1668, defaultKey, CONTROLS));
    }

    private static void handleControls(class_310 client) {
        while (toggleCinematic.method_1436()) {
            if (recordingMode || manualActivation || DIRECTOR.isActive()) {
                CinecraftClient.stopCinematic(true);
                continue;
            }
            manualActivation = true;
            IDLE.activity();
        }
        while (openSettings.method_1436()) {
            CinecraftClient.stopCinematic(true);
            client.method_1507((class_437)new CinecraftSettingsScreen(client.field_1755));
        }
        while (toggleRecording.method_1436()) {
            if (recordingMode) {
                CinecraftClient.stopCinematic(true);
                continue;
            }
            if (!RECORDER.start(client)) continue;
            recordingMode = true;
            manualActivation = false;
            IDLE.activity();
        }
        while (toggleDebug.method_1436()) {
            CinecraftConfig config;
            config.debugOverlay(!(config = CinecraftConfig.INSTANCE).debugOverlay());
            config.save();
        }
        while (nextShot.method_1436()) {
            if (!DIRECTOR.isActive()) continue;
            DIRECTOR.nextShot();
        }
    }

    private static void drainControlPresses() {
        CinecraftClient.drain(toggleCinematic);
        CinecraftClient.drain(openSettings);
        CinecraftClient.drain(toggleRecording);
        CinecraftClient.drain(toggleDebug);
        CinecraftClient.drain(nextShot);
    }

    private static void drain(class_304 binding) {
        while (binding != null && binding.method_1436()) {
        }
    }
}


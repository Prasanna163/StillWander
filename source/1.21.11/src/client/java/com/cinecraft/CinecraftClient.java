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
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;

/** Client-only entry point and the intentionally small public bridge for mixins. */
public final class CinecraftClient implements ClientModInitializer {
    public static final IdleDetector IDLE = new IdleDetector(25_000L);
    public static final CinematicDirector DIRECTOR = new CinematicDirector();
    private static final DamageMonitor DAMAGE = new DamageMonitor();
    private static final CinematicRecorder RECORDER = new CinematicRecorder();
    private static final KeyBinding.Category CONTROLS = KeyBinding.Category.create(
            Identifier.of("stillwander", "controls")
    );
    private static KeyBinding toggleCinematic;
    private static KeyBinding openSettings;
    private static KeyBinding toggleRecording;
    private static KeyBinding toggleDebug;
    private static KeyBinding nextShot;
    private static boolean manualActivation;
    private static boolean recordingMode;

    public static void activity() {
        if (recordingMode) return;
        stopCinematic(true);
    }

    private static void stopCinematic(boolean resetIdle) {
        manualActivation = false;
        recordingMode = false;
        RECORDER.stop();
        if (resetIdle) IDLE.activity();
        DIRECTOR.stop();
    }

    /** Lets Cinecraft controls reach the client-tick handler before ordinary input exits. */
    public static boolean isControlKey(KeyInput input) {
        return matches(toggleCinematic, input)
                || matches(openSettings, input)
                || matches(toggleRecording, input)
                || matches(toggleDebug, input)
                || matches(nextShot, input);
    }

    private static boolean matches(KeyBinding binding, KeyInput input) {
        return binding != null && binding.matchesKey(input);
    }

    public static void applyConfiguration() {
        IDLE.setTimeoutMillis(CinecraftConfig.INSTANCE.idleSeconds() * 1_000L);
    }

    public static boolean isRecordingMode() {
        return recordingMode;
    }

    /** True slightly before, and for the full lifetime of, any cinematic session. */
    public static boolean requiresUnthrottledRendering() {
        return recordingMode || manualActivation || IDLE.isIdle() || DIRECTOR.isActive();
    }

    @Override
    public void onInitializeClient() {
        toggleCinematic = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.stillwander.toggle_cinematic",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_B,
                CONTROLS
        ));
        openSettings = register("key.stillwander.open_settings", InputUtil.GLFW_KEY_F7);
        toggleRecording = register("key.stillwander.toggle_recording", InputUtil.GLFW_KEY_F8);
        toggleDebug = register("key.stillwander.toggle_debug", InputUtil.GLFW_KEY_F9);
        nextShot = register("key.stillwander.next_shot", InputUtil.GLFW_KEY_N);
        applyConfiguration();
        CinecraftDebugHud.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                drainControlPresses();
                manualActivation = false;
                recordingMode = false;
                RECORDER.stop();
                DAMAGE.reset();
                DIRECTOR.stop();
                return;
            }
            handleControls(client);
            boolean damaged = DAMAGE.update(client.player);
            if (damaged && CinecraftConfig.INSTANCE.exitOnDamage()) {
                stopCinematic(true);
                return;
            }
            boolean cinematicRequested = recordingMode || manualActivation || IDLE.isIdle();
            CinecraftFlawlessFrames.setActive(cinematicRequested);
            if (cinematicRequested) {
                DIRECTOR.tick(client);
                RECORDER.tick(client, DIRECTOR);
                if (recordingMode && !RECORDER.isRecording()) stopCinematic(true);
            } else {
                DIRECTOR.stop();
            }
        });
    }

    private static KeyBinding register(String id, int defaultKey) {
        return KeyBindingHelper.registerKeyBinding(new KeyBinding(
                id,
                InputUtil.Type.KEYSYM,
                defaultKey,
                CONTROLS
        ));
    }

    private static void handleControls(net.minecraft.client.MinecraftClient client) {
        while (toggleCinematic.wasPressed()) {
            if (recordingMode || manualActivation || DIRECTOR.isActive()) {
                stopCinematic(true);
            } else {
                manualActivation = true;
                IDLE.activity();
            }
        }
        while (openSettings.wasPressed()) {
            stopCinematic(true);
            client.setScreen(new CinecraftSettingsScreen(client.currentScreen));
        }
        while (toggleRecording.wasPressed()) {
            if (recordingMode) {
                stopCinematic(true);
            } else if (RECORDER.start(client)) {
                recordingMode = true;
                manualActivation = false;
                IDLE.activity();
            }
        }
        while (toggleDebug.wasPressed()) {
            CinecraftConfig config = CinecraftConfig.INSTANCE;
            config.debugOverlay(!config.debugOverlay());
            config.save();
        }
        while (nextShot.wasPressed()) {
            if (DIRECTOR.isActive()) DIRECTOR.nextShot();
        }
    }

    private static void drainControlPresses() {
        drain(toggleCinematic);
        drain(openSettings);
        drain(toggleRecording);
        drain(toggleDebug);
        drain(nextShot);
    }

    private static void drain(KeyBinding binding) {
        while (binding != null && binding.wasPressed()) { }
    }
}

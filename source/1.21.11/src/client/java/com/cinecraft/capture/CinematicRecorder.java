package com.cinecraft.capture;

import com.cinecraft.camera.CameraPose;
import com.cinecraft.config.CinecraftConfig;
import com.cinecraft.director.CinematicDirector;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;

import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Samples generated camera poses and optionally mirrors keyframes into ReplayMod. */
public final class CinematicRecorder {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final long SAMPLE_INTERVAL_NANOS = 100_000_000L;
    private static final long REPLAY_KEYFRAME_INTERVAL_NANOS = 500_000_000L;

    private BufferedWriter writer;
    private Path currentPath;
    private long startedNanos;
    private long lastSampleNanos;
    private long lastScreenshotNanos;
    private long lastReplayKeyframeNanos;
    private ReplayModBridge replayMod;

    public boolean start(MinecraftClient client) {
        if (writer != null) return true;
        try {
            Path directory = client.runDirectory.toPath().resolve("stillwander").resolve("camera_paths");
            Files.createDirectories(directory);
            currentPath = directory.resolve("stillwander-" + FILE_TIME.format(LocalDateTime.now()) + ".csv");
            writer = Files.newBufferedWriter(currentPath);
            writer.write("time_ms,x,y,z,yaw,pitch,fov,focus_distance,shot_type,subject_type,source\n");
            startedNanos = System.nanoTime();
            lastSampleNanos = 0L;
            lastScreenshotNanos = startedNanos;
            lastReplayKeyframeNanos = 0L;
            replayMod = ReplayModBridge.tryCreate();
            return true;
        } catch (IOException exception) {
            writer = null;
            currentPath = null;
            return false;
        }
    }

    public void tick(MinecraftClient client, CinematicDirector director) {
        if (writer == null || !director.isActive()) return;
        long now = System.nanoTime();
        if (now - lastSampleNanos >= SAMPLE_INTERVAL_NANOS) {
            CameraPose pose = director.pose(0.0f);
            if (pose != null) writeSample(director, pose, now);
            lastSampleNanos = now;
        }

        int screenshotSeconds = CinecraftConfig.INSTANCE.screenshotIntervalSeconds();
        if (screenshotSeconds > 0
                && now - lastScreenshotNanos >= screenshotSeconds * 1_000_000_000L) {
            ScreenshotRecorder.saveScreenshot(client.runDirectory, client.getFramebuffer(), message -> { });
            lastScreenshotNanos = now;
        }
    }

    private void writeSample(CinematicDirector director, CameraPose pose, long now) {
        long elapsedMillis = (now - startedNanos) / 1_000_000L;
        try {
            writer.write(String.format(
                    Locale.ROOT,
                    "%d,%.6f,%.6f,%.6f,%.4f,%.4f,%.4f,%.4f,%s,%s,%s%n",
                    elapsedMillis,
                    pose.position().x,
                    pose.position().y,
                    pose.position().z,
                    pose.yaw(),
                    pose.pitch(),
                    pose.fov(),
                    pose.focusDistance(),
                    safe(director.currentShotType()),
                    safe(director.currentSubjectType()),
                    csv(director.plannerSource())
            ));
            writer.flush();
        } catch (IOException exception) {
            stop();
            return;
        }

        if (replayMod != null && now - lastReplayKeyframeNanos >= REPLAY_KEYFRAME_INTERVAL_NANOS) {
            if (!replayMod.add(elapsedMillis, pose)) replayMod = null;
            lastReplayKeyframeNanos = now;
        }
    }

    public void stop() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {
            }
        }
        writer = null;
        replayMod = null;
    }

    public boolean isRecording() { return writer != null; }
    public Path currentPath() { return currentPath; }

    private static String safe(Object value) { return value == null ? "none" : value.toString(); }
    private static String csv(String value) { return value == null ? "none" : value.replace(',', '_'); }

    /** Reflection keeps ReplayMod completely optional while using its real SPTimeline when present. */
    private static final class ReplayModBridge {
        private final Object timeline;
        private final Method addPosition;
        private final Method hasPosition;
        private final long offset;

        private ReplayModBridge(Object timeline, Method addPosition, Method hasPosition, long offset) {
            this.timeline = timeline;
            this.addPosition = addPosition;
            this.hasPosition = hasPosition;
            this.offset = offset;
        }

        private static ReplayModBridge tryCreate() {
            if (!FabricLoader.getInstance().isModLoaded("replaymod")) return null;
            try {
                Class<?> moduleClass = Class.forName("com.replaymod.simplepathing.ReplayModSimplePathing");
                Field instanceField = moduleClass.getField("instance");
                Object module = instanceField.get(null);
                if (module == null) return null;
                Object timeline = moduleClass.getMethod("getCurrentTimeline").invoke(module);
                if (timeline == null) return null;
                Method selectedTime = moduleClass.getMethod("getSelectedTime");
                long offset = ((Number) selectedTime.invoke(module)).longValue();
                Method add = timeline.getClass().getMethod(
                        "addPositionKeyframe",
                        long.class,
                        double.class,
                        double.class,
                        double.class,
                        float.class,
                        float.class,
                        float.class,
                        int.class
                );
                Method has = timeline.getClass().getMethod("isPositionKeyframe", long.class);
                return new ReplayModBridge(timeline, add, has, offset);
            } catch (ReflectiveOperationException | LinkageError exception) {
                return null;
            }
        }

        private boolean add(long elapsedMillis, CameraPose pose) {
            try {
                long time = offset + elapsedMillis;
                while ((Boolean) hasPosition.invoke(timeline, time)) time++;
                addPosition.invoke(
                        timeline,
                        time,
                        pose.position().x,
                        pose.position().y,
                        pose.position().z,
                        pose.yaw(),
                        pose.pitch(),
                        0.0f,
                        -1
                );
                return true;
            } catch (ReflectiveOperationException | RuntimeException exception) {
                return false;
            }
        }
    }
}

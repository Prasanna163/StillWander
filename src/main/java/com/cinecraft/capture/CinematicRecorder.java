/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.loader.api.FabricLoader
 *  net.minecraft.class_276
 *  net.minecraft.class_310
 *  net.minecraft.class_318
 */
package com.cinecraft.capture;

import com.cinecraft.camera.CameraPose;
import com.cinecraft.config.CinecraftConfig;
import com.cinecraft.director.CinematicDirector;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_276;
import net.minecraft.class_310;
import net.minecraft.class_318;

@Environment(value=EnvType.CLIENT)
public final class CinematicRecorder {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final long SAMPLE_INTERVAL_NANOS = 100000000L;
    private static final long REPLAY_KEYFRAME_INTERVAL_NANOS = 500000000L;
    private BufferedWriter writer;
    private Path currentPath;
    private long startedNanos;
    private long lastSampleNanos;
    private long lastScreenshotNanos;
    private long lastReplayKeyframeNanos;
    private ReplayModBridge replayMod;

    public boolean start(class_310 client) {
        if (this.writer != null) {
            return true;
        }
        try {
            Path directory = client.field_1697.toPath().resolve("cinecraft").resolve("camera_paths");
            Files.createDirectories(directory, new FileAttribute[0]);
            this.currentPath = directory.resolve("cinecraft-" + FILE_TIME.format(LocalDateTime.now()) + ".csv");
            this.writer = Files.newBufferedWriter(this.currentPath, new OpenOption[0]);
            this.writer.write("time_ms,x,y,z,yaw,pitch,fov,focus_distance,shot_type,subject_type,source\n");
            this.startedNanos = System.nanoTime();
            this.lastSampleNanos = 0L;
            this.lastScreenshotNanos = this.startedNanos;
            this.lastReplayKeyframeNanos = 0L;
            this.replayMod = ReplayModBridge.tryCreate();
            return true;
        }
        catch (IOException exception) {
            this.writer = null;
            this.currentPath = null;
            return false;
        }
    }

    public void tick(class_310 client, CinematicDirector director) {
        int screenshotSeconds;
        if (this.writer == null || !director.isActive()) {
            return;
        }
        long now = System.nanoTime();
        if (now - this.lastSampleNanos >= 100000000L) {
            CameraPose pose = director.pose(0.0f);
            if (pose != null) {
                this.writeSample(director, pose, now);
            }
            this.lastSampleNanos = now;
        }
        if ((screenshotSeconds = CinecraftConfig.INSTANCE.screenshotIntervalSeconds()) > 0 && now - this.lastScreenshotNanos >= (long)screenshotSeconds * 1000000000L) {
            class_318.method_1659((File)client.field_1697, (class_276)client.method_1522(), message -> {});
            this.lastScreenshotNanos = now;
        }
    }

    private void writeSample(CinematicDirector director, CameraPose pose, long now) {
        long elapsedMillis = (now - this.startedNanos) / 1000000L;
        try {
            this.writer.write(String.format(Locale.ROOT, "%d,%.6f,%.6f,%.6f,%.4f,%.4f,%.4f,%.4f,%s,%s,%s%n", elapsedMillis, pose.position().field_1352, pose.position().field_1351, pose.position().field_1350, Float.valueOf(pose.yaw()), Float.valueOf(pose.pitch()), Float.valueOf(pose.fov()), Float.valueOf(pose.focusDistance()), CinematicRecorder.safe((Object)director.currentShotType()), CinematicRecorder.safe((Object)director.currentSubjectType()), CinematicRecorder.csv(director.plannerSource())));
            this.writer.flush();
        }
        catch (IOException exception) {
            this.stop();
            return;
        }
        if (this.replayMod != null && now - this.lastReplayKeyframeNanos >= 500000000L) {
            if (!this.replayMod.add(elapsedMillis, pose)) {
                this.replayMod = null;
            }
            this.lastReplayKeyframeNanos = now;
        }
    }

    public void stop() {
        if (this.writer != null) {
            try {
                this.writer.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        this.writer = null;
        this.replayMod = null;
    }

    public boolean isRecording() {
        return this.writer != null;
    }

    public Path currentPath() {
        return this.currentPath;
    }

    private static String safe(Object value) {
        return value == null ? "none" : value.toString();
    }

    private static String csv(String value) {
        return value == null ? "none" : value.replace(',', '_');
    }

    @Environment(value=EnvType.CLIENT)
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
            if (!FabricLoader.getInstance().isModLoaded("replaymod")) {
                return null;
            }
            try {
                Class<?> moduleClass = Class.forName("com.replaymod.simplepathing.ReplayModSimplePathing");
                Field instanceField = moduleClass.getField("instance");
                Object module = instanceField.get(null);
                if (module == null) {
                    return null;
                }
                Object timeline = moduleClass.getMethod("getCurrentTimeline", new Class[0]).invoke(module, new Object[0]);
                if (timeline == null) {
                    return null;
                }
                Method selectedTime = moduleClass.getMethod("getSelectedTime", new Class[0]);
                long offset = ((Number)selectedTime.invoke(module, new Object[0])).longValue();
                Method add = timeline.getClass().getMethod("addPositionKeyframe", Long.TYPE, Double.TYPE, Double.TYPE, Double.TYPE, Float.TYPE, Float.TYPE, Float.TYPE, Integer.TYPE);
                Method has = timeline.getClass().getMethod("isPositionKeyframe", Long.TYPE);
                return new ReplayModBridge(timeline, add, has, offset);
            }
            catch (LinkageError | ReflectiveOperationException exception) {
                return null;
            }
        }

        private boolean add(long elapsedMillis, CameraPose pose) {
            try {
                long time = this.offset + elapsedMillis;
                while (((Boolean)this.hasPosition.invoke(this.timeline, time)).booleanValue()) {
                    ++time;
                }
                this.addPosition.invoke(this.timeline, time, pose.position().field_1352, pose.position().field_1351, pose.position().field_1350, Float.valueOf(pose.yaw()), Float.valueOf(pose.pitch()), Float.valueOf(0.0f), -1);
                return true;
            }
            catch (ReflectiveOperationException | RuntimeException exception) {
                return false;
            }
        }
    }
}


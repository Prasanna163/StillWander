/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_243
 *  net.minecraft.class_310
 */
package com.cinecraft.director;

import com.cinecraft.camera.CameraPath;
import com.cinecraft.camera.FovPath;
import com.cinecraft.camera.PanPath;
import com.cinecraft.camera.RailPath;
import com.cinecraft.config.CinecraftConfig;
import com.cinecraft.director.BiomeMood;
import com.cinecraft.director.EntityAction;
import com.cinecraft.director.EnvironmentProfile;
import com.cinecraft.director.Framing;
import com.cinecraft.director.MotionStyle;
import com.cinecraft.director.SceneScanner;
import com.cinecraft.director.SceneSubject;
import com.cinecraft.director.SceneTime;
import com.cinecraft.director.SceneWeather;
import com.cinecraft.director.ScreenPlacement;
import com.cinecraft.director.ShotComposition;
import com.cinecraft.director.ShotEnvironment;
import com.cinecraft.director.ShotLibrary;
import com.cinecraft.director.ShotPlan;
import com.cinecraft.director.ShotPreset;
import com.cinecraft.director.ShotType;
import com.cinecraft.director.SpaceType;
import com.cinecraft.director.SubjectType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_310;

@Environment(value=EnvType.CLIENT)
public final class ShotPlanner {
    private static final int RECENT_PATH_LIMIT = 12;
    private final ArrayDeque<PathSignature> recentPaths = new ArrayDeque();
    private final ArrayDeque<String> recentPresets = new ArrayDeque();
    private final Random random = new Random();
    private ShotComposition lastComposition;
    private ShotType lastShotType;
    private float lastFov = Float.NaN;
    private String diagnosticSource = "none";
    private String diagnosticDecision = "waiting";
    private int diagnosticRejected;

    public Optional<ShotPlan> plan(class_310 client, SceneScanner scanner, SceneSubject subject, EnvironmentProfile profile, boolean preferWide) {
        Optional<ShotPlan> libraryPlan;
        boolean wide = preferWide && profile.supportsWideShots();
        this.diagnosticRejected = 0;
        if (this.random.nextDouble() < 0.56 && (libraryPlan = this.planFromLibrary(client, scanner, subject, profile, wide)).isPresent()) {
            return libraryPlan;
        }
        ArrayList<ScoredPlan> survivors = new ArrayList<ScoredPlan>();
        int routedAttempts = 0;
        for (int attempt = 0; attempt < CinecraftConfig.INSTANCE.quality().candidateCount(); ++attempt) {
            GeneratedPlan raw;
            boolean allowRouting = !(raw = this.generate(subject, profile, wide)).passage() || routedAttempts++ < 4;
            GeneratedPlan generated = this.adaptToScene(client, scanner, raw, subject, profile, wide, allowRouting);
            if (!ShotPlanner.framingIsUsable(generated.plan(), generated.aerial(), wide)) {
                ++this.diagnosticRejected;
                continue;
            }
            if (!scanner.isPathUsable(client, generated.plan().path(), generated.plan().focusPath(), generated.plan().subjectPath(), profile)) {
                ++this.diagnosticRejected;
                continue;
            }
            survivors.add(new ScoredPlan(generated.plan(), generated.signature(), this.score(generated, profile, wide) + scanner.compositionScore(client, generated.plan(), profile)));
        }
        return survivors.stream().max(Comparator.comparingDouble(ScoredPlan::score)).map(selected -> {
            this.remember(selected.signature(), selected.plan(), selected.score());
            return selected.plan();
        });
    }

    private Optional<ShotPlan> planFromLibrary(class_310 client, SceneScanner scanner, SceneSubject subject, EnvironmentProfile profile, boolean wide) {
        ArrayList<ShotPreset> matching = new ArrayList<ShotPreset>(ShotLibrary.matching(subject.type(), wide, profile));
        Collections.shuffle(matching, this.random);
        matching.sort(Comparator.comparing(preset -> preset.environment() == ShotEnvironment.ANY));
        List<ShotPreset> fresh = matching.stream().filter(preset -> !this.recentPresets.contains(preset.name())).toList();
        if (!fresh.isEmpty()) {
            matching = new ArrayList<ShotPreset>(fresh);
        }
        for (ShotPreset preset2 : matching) {
            for (int attempt = 0; attempt < 10; ++attempt) {
                GeneratedPlan generated = this.instantiatePreset(subject, profile, preset2);
                if (generated == null || !ShotPlanner.framingIsUsable((generated = this.adaptToScene(client, scanner, generated, subject, profile, wide, true)).plan(), generated.aerial(), wide)) continue;
                if (!scanner.isPathUsable(client, generated.plan().path(), generated.plan().focusPath(), generated.plan().subjectPath(), profile)) {
                    ++this.diagnosticRejected;
                    continue;
                }
                double selectedScore = this.score(generated, profile, wide) + scanner.compositionScore(client, generated.plan(), profile);
                this.remember(generated.signature(), generated.plan(), selectedScore);
                this.rememberPreset(preset2.name());
                return Optional.of(generated.plan());
            }
        }
        return Optional.empty();
    }

    private GeneratedPlan adaptToScene(class_310 client, SceneScanner scanner, GeneratedPlan generated, SceneSubject subject, EnvironmentProfile profile, boolean wide, boolean allowRouting) {
        ShotPlan original = generated.plan();
        CameraPath cameraPath = original.path();
        CameraPath subjectPath = original.subjectPath();
        Object source = original.source();
        if (generated.passage() && allowRouting) {
            Optional<CameraPath> routed = scanner.findNavigablePath(client, cameraPath.sample(0.0), cameraPath.sample(1.0));
            if (routed.isPresent()) {
                CameraPath finalRoute = cameraPath = routed.get();
                subjectPath = progress -> finalRoute.sample(Math.min(1.0, progress + 0.16));
                source = (String)source + ":air-route";
            }
        } else if (!(profile.underground() || generated.aerial() || !wide && subject.type() != SubjectType.LANDSCAPE)) {
            double clearance = wide ? ShotPlanner.clamp(5.5 + profile.terrainRelief() * 0.08, 5.5, 10.0) : 3.2;
            cameraPath = scanner.terrainFollowingPath(client, cameraPath, clearance);
            source = (String)source + ":terrain";
        }
        CameraPath focusPath = this.composeFocus(cameraPath, subjectPath, original.composition(), original.fovPath());
        ShotPlan adapted = new ShotPlan(original.type(), cameraPath, focusPath, subjectPath, original.fovPath(), original.durationMillis(), original.composition(), (String)source);
        return new GeneratedPlan(adapted, generated.signature(), generated.radius(), generated.sweep(), ShotPlanner.approximateLength(cameraPath), generated.aerial(), generated.passage());
    }

    private GeneratedPlan instantiatePreset(SceneSubject subject, EnvironmentProfile profile, ShotPreset preset) {
        double sweep;
        double baseAngle;
        class_243 center = preset.wide() ? profile.landscapeCenter() : ShotPlanner.pathCenterForSubject(subject.target(), profile.playerFocus(), profile.maxCameraDistance());
        double centerDistance = ShotPlanner.horizontalDistance(center, profile.playerFocus());
        double localMaximum = Math.max(1.8, profile.maxCameraDistance() - centerDistance - 0.5);
        double radius = Math.min(preset.radius(), localMaximum);
        if (radius < preset.radius() * 0.68) {
            return null;
        }
        double height = Math.min(preset.height(), profile.maxVerticalRise() - 1.5);
        if (preset.aerial() && height < 18.0) {
            return null;
        }
        if (preset.wide()) {
            baseAngle = this.between(0.0, Math.PI * 2);
        } else {
            class_243 towardPlayer = profile.playerFocus().method_1020(center);
            baseAngle = Math.atan2(towardPlayer.field_1350, towardPlayer.field_1352) + this.between(-0.9, 0.9);
        }
        double d = sweep = preset.motionStyle() == MotionStyle.PAN ? ShotPlanner.radians(preset.sweepDegrees()) : 0.0;
        if (this.random.nextBoolean()) {
            sweep = -sweep;
        }
        double startingRadius = radius;
        double endingRadius = radius * (1.0 + preset.radialChange());
        PanPath cameraPath = new PanPath(center, baseAngle, sweep, startingRadius, endingRadius, center.field_1351 + height, center.field_1351 + height);
        ShotComposition composition = this.compositionFor(subject, preset.wide(), radius, sweep);
        CameraPath subjectPath = this.createSubjectPath(subject, profile, preset.wide(), preset.aerial(), preset.motionStyle() == MotionStyle.HOLD, composition.rackFocus());
        ShotType type = ShotPlanner.chooseIntent(subject, profile, preset.wide(), preset.aerial(), preset.radialChange());
        float endingFov = (float)((double)preset.startingFov() + (double)(preset.endingFov() - preset.startingFov()) * CinecraftConfig.INSTANCE.zoomStrength());
        FovPath fovPath = FovPath.linear(preset.startingFov(), endingFov);
        CameraPath focusPath = this.composeFocus(cameraPath, subjectPath, composition, fovPath);
        long duration = ShotPlanner.configuredDuration(preset.durationMillis(), subject.action());
        PathSignature signature = ShotPlanner.signature(baseAngle, radius, height, sweep);
        return new GeneratedPlan(new ShotPlan(type, cameraPath, focusPath, subjectPath, fovPath, duration, composition, "library:" + preset.name()), signature, radius, Math.abs(sweep), ShotPlanner.approximateLength(cameraPath), preset.aerial(), false);
    }

    private GeneratedPlan generate(SceneSubject subject, EnvironmentProfile profile, boolean wide) {
        ShotType type;
        long duration;
        CameraPath subjectPath;
        Record cameraPath;
        double heightDrift;
        double heightAboveFocus;
        double radialDrift;
        double sweep;
        double sweepMagnitude;
        double baseAngle;
        double radius;
        boolean aerial;
        double maximum = profile.maxCameraDistance();
        class_243 center = wide ? profile.landscapeCenter() : ShotPlanner.pathCenterForSubject(subject.target(), profile.playerFocus(), maximum);
        boolean enclosed = profile.underground();
        boolean transitional = profile.spaceType() == SpaceType.TRANSITIONAL;
        boolean bl = aerial = wide && !profile.underground() && CinecraftConfig.INSTANCE.aerialShots() && this.random.nextDouble() < ShotPlanner.aerialChance(profile);
        double holdChance = subject.action().moving() ? 0.05 : (wide ? 0.08 : 0.24);
        boolean hold = this.random.nextDouble() < holdChance;
        boolean passage = !wide && !hold && (subject.type() == SubjectType.FEATURE || subject.type() == SubjectType.LANDSCAPE) && this.random.nextDouble() < 0.44;
        double centerDistance = ShotPlanner.horizontalDistance(center, profile.playerFocus());
        double localMaximum = Math.max(1.8, maximum - centerDistance - 0.5);
        if (wide) {
            double upper = Math.max(14.0, maximum - 3.0);
            double sceneDriven = ShotPlanner.clamp(profile.sceneRadius() * 0.85, 14.0, upper);
            radius = this.between(Math.max(10.0, sceneDriven * 0.72), sceneDriven);
        } else if (subject.type() == SubjectType.PLAYER_DETAIL) {
            minimum = enclosed ? 2.6 : 3.4;
            radius = this.between(Math.min(minimum, localMaximum), Math.min(enclosed ? 4.8 : 5.8, localMaximum));
        } else if (subject.type() == SubjectType.PLAYER) {
            minimum = enclosed ? 3.5 : 9.5;
            radius = this.between(Math.min(minimum, localMaximum), Math.min(enclosed ? 7.0 : 18.0, localMaximum));
        } else {
            radius = subject.type() == SubjectType.GROUP ? this.between(Math.min(enclosed ? 4.0 : 8.0, localMaximum), Math.min(enclosed ? 7.0 : 18.0, localMaximum)) : (enclosed ? this.between(1.15, Math.min(4.2, localMaximum)) : (transitional ? this.between(3.2, Math.min(10.5, localMaximum)) : this.between(4.2, Math.min(13.0, localMaximum))));
        }
        class_243 movement = subject.movementVector();
        if (wide) {
            baseAngle = this.between(0.0, Math.PI * 2);
        } else if (subject.action().moving() && movement.method_37268() > 0.001) {
            double movementAngle = Math.atan2(movement.field_1350, movement.field_1352);
            baseAngle = movementAngle + (this.random.nextBoolean() ? 1.5707963267948966 : -1.5707963267948966) + this.between(-0.42, 0.42);
        } else {
            class_243 towardPlayer = profile.playerFocus().method_1020(center);
            baseAngle = Math.atan2(towardPlayer.field_1350, towardPlayer.field_1352) + this.between(-1.15, 1.15);
        }
        double d = enclosed ? this.between(ShotPlanner.radians(6.0), ShotPlanner.radians(20.0)) : (aerial ? this.between(ShotPlanner.radians(18.0), ShotPlanner.radians(48.0)) : (sweepMagnitude = wide ? this.between(ShotPlanner.radians(24.0), ShotPlanner.radians(66.0)) : this.between(ShotPlanner.radians(12.0), ShotPlanner.radians(42.0))));
        double d2 = hold ? 0.0 : (sweep = this.random.nextBoolean() ? sweepMagnitude : -sweepMagnitude);
        double d3 = aerial ? this.between(-0.12, 0.12) : (radialDrift = wide ? this.between(-0.22, 0.22) : this.between(-0.18, 0.18));
        if (hold) {
            radialDrift = 0.0;
        }
        if (enclosed) {
            heightAboveFocus = this.between(0.15, 1.25);
        } else if (subject.type() == SubjectType.PLAYER_DETAIL) {
            heightAboveFocus = this.between(-0.15, 0.65);
        } else if (aerial) {
            double maximumAerialHeight = Math.min(profile.maxVerticalRise() - 2.0, Math.max(30.0, radius * 1.2 + profile.terrainRelief() * 0.16));
            heightAboveFocus = this.between(20.0, maximumAerialHeight);
        } else {
            heightAboveFocus = wide ? ShotPlanner.clamp(radius * this.between(0.18, 0.36) + profile.terrainRelief() * 0.08, 4.5, 16.0) : this.between(0.45, Math.min(4.5, profile.maxVerticalRise() * 0.3));
        }
        double d4 = enclosed ? this.between(-0.45, 0.45) : (aerial ? this.between(-2.0, 2.0) : (heightDrift = wide ? this.between(-1.6, 1.6) : this.between(-1.2, 1.2)));
        if (hold) {
            heightDrift = 0.0;
        }
        ShotComposition composition = this.compositionFor(subject, wide, radius, sweep);
        if (passage) {
            double travel = enclosed ? this.between(6.0, 12.0) : this.between(10.0, 22.0);
            double travelAngle = baseAngle + this.between(-0.65, 0.65);
            class_243 forward = new class_243(Math.cos(travelAngle), 0.0, Math.sin(travelAngle));
            class_243 side = new class_243(-forward.field_1350, 0.0, forward.field_1352).method_1021(this.between(-2.2, 2.2));
            double railY = center.field_1351 + ShotPlanner.clamp(heightAboveFocus, 0.7, 2.4);
            class_243 start = new class_243(center.field_1352, railY, center.field_1350).method_1020(forward.method_1021(travel * 0.45)).method_1019(side);
            class_243 end = new class_243(center.field_1352, railY + heightDrift * 0.35, center.field_1350).method_1019(forward.method_1021(travel * 0.55)).method_1019(side.method_1021(0.55));
            cameraPath = new RailPath(start, end);
            double lookAheadProgress = ShotPlanner.clamp(this.between(5.0, 9.0) / Math.max(6.0, travel), 0.16, 0.42);
            subjectPath = arg_0 -> ShotPlanner.lambda$generate$4((CameraPath)((Object)cameraPath), lookAheadProgress, arg_0);
            duration = Math.round(ShotPlanner.clamp(travel * 2200.0, 30000.0, 52000.0));
            type = ShotType.PROCEDURAL_PASSAGE;
        } else {
            double startingRadius = radius * (1.0 - radialDrift * 0.5);
            double endingRadius = radius * (1.0 + radialDrift * 0.5);
            double startingY = center.field_1351 + heightAboveFocus - heightDrift * 0.5;
            double endingY = center.field_1351 + heightAboveFocus + heightDrift * 0.5;
            cameraPath = new PanPath(center, baseAngle, sweep, startingRadius, endingRadius, startingY, endingY);
            subjectPath = this.createSubjectPath(subject, profile, wide, aerial, hold, composition.rackFocus());
            double panLength = ShotPlanner.approximateLength(cameraPath);
            duration = hold ? Math.round(this.between(wide ? 24000.0 : 18000.0, wide ? 34000.0 : 28000.0)) : ShotPlanner.durationFor(panLength, Math.abs(sweep), enclosed, wide, aerial);
            type = ShotPlanner.chooseIntent(subject, profile, wide, aerial, radialDrift);
        }
        double pathLength = ShotPlanner.approximateLength(cameraPath);
        FovPath fovPath = this.generatedFov(subject, wide, aerial, passage, hold);
        CameraPath focusPath = this.composeFocus((CameraPath)((Object)cameraPath), subjectPath, composition, fovPath);
        duration = ShotPlanner.configuredDuration(duration, subject.action());
        PathSignature signature = ShotPlanner.signature(baseAngle, radius, heightAboveFocus, sweep);
        return new GeneratedPlan(new ShotPlan(type, (CameraPath)((Object)cameraPath), focusPath, subjectPath, fovPath, duration, composition, "procedural"), signature, radius, Math.abs(sweep), pathLength, aerial, passage);
    }

    private FovPath generatedFov(SceneSubject subject, boolean wide, boolean aerial, boolean passage, boolean hold) {
        boolean zoom;
        double maximum;
        double minimum;
        if (subject.type() == SubjectType.PLAYER_DETAIL) {
            minimum = 27.0;
            maximum = 40.0;
        } else if (subject.type() == SubjectType.ENTITY) {
            minimum = 34.0;
            maximum = 48.0;
        } else if (subject.type() == SubjectType.GROUP) {
            minimum = 42.0;
            maximum = 62.0;
        } else if (subject.type() == SubjectType.FEATURE) {
            minimum = 40.0;
            maximum = 56.0;
        } else if (subject.type() == SubjectType.PLAYER) {
            minimum = 48.0;
            maximum = 62.0;
        } else if (wide) {
            minimum = aerial ? 58.0 : 66.0;
            maximum = aerial ? 74.0 : 84.0;
        } else {
            minimum = 50.0;
            maximum = 68.0;
        }
        float start = (float)this.between(minimum, maximum);
        boolean bl = zoom = passage || subject.type() == SubjectType.PLAYER_DETAIL || !hold && this.random.nextDouble() < 0.58;
        if (!zoom) {
            return FovPath.fixed(start);
        }
        double magnitude = passage ? this.between(9.0, 17.0) : (subject.type() == SubjectType.PLAYER_DETAIL ? this.between(6.0, 12.0) : (wide ? this.between(5.0, 11.0) : this.between(4.0, 9.0)));
        double direction = this.random.nextBoolean() ? 1.0 : -1.0;
        float end = (float)ShotPlanner.clamp((double)start + direction * magnitude * CinecraftConfig.INSTANCE.zoomStrength(), 24.0, 90.0);
        return FovPath.linear(start, end);
    }

    private CameraPath createSubjectPath(SceneSubject subject, EnvironmentProfile profile, boolean wide, boolean aerial, boolean hold, boolean rackFocus) {
        if (!wide || profile.landscapeAnchors().size() < 3) {
            return rackFocus ? subject::rackFocusTarget : progress -> subject.currentTarget();
        }
        List<class_243> anchors = profile.landscapeAnchors();
        class_243 first = anchors.get(this.random.nextInt(anchors.size()));
        class_243 center = profile.landscapeCenter();
        double minimumBlend = aerial ? 0.62 : 0.32;
        double maximumBlend = aerial ? 0.96 : 0.62;
        double lift = aerial ? 3.0 + profile.terrainRelief() * 0.07 : 1.3 + profile.terrainRelief() * 0.04;
        class_243 openingFocus = center.method_35590(first, this.between(minimumBlend, maximumBlend)).method_1031(0.0, lift, 0.0);
        if (hold) {
            return progress -> openingFocus;
        }
        class_243 second = anchors.get(this.random.nextInt(anchors.size()));
        for (int retry = 0; retry < 5 && first.method_1025(second) < 144.0; ++retry) {
            second = anchors.get(this.random.nextInt(anchors.size()));
        }
        class_243 closingFocus = center.method_35590(second, this.between(minimumBlend, maximumBlend)).method_1031(0.0, lift, 0.0);
        return progress -> openingFocus.method_35590(closingFocus, progress);
    }

    private CameraPath composeFocus(CameraPath cameraPath, CameraPath subjectPath, ShotComposition composition, FovPath fovPath) {
        return progress -> {
            class_243 camera = cameraPath.sample(progress);
            class_243 subject = subjectPath.sample(progress);
            class_243 view = subject.method_1020(camera);
            double horizontal = Math.sqrt(view.field_1352 * view.field_1352 + view.field_1350 * view.field_1350);
            if (horizontal < 0.001) {
                return subject;
            }
            class_243 right = new class_243(-view.field_1350 / horizontal, 0.0, view.field_1352 / horizontal);
            double fovScale = Math.tan(Math.toRadians(ShotPlanner.clamp(fovPath.sample(progress), 24.0, 90.0) * 0.5));
            double placementOffset = (double)composition.placement().direction() * Math.min(3.5, horizontal * fovScale * 0.16);
            double headroom = switch (composition.framing()) {
                default -> throw new MatchException(null, null);
                case Framing.DETAIL -> -0.02;
                case Framing.CLOSE -> -0.035;
                case Framing.MEDIUM -> -0.055;
                case Framing.WIDE, Framing.EXTREME_WIDE -> -0.075;
            };
            return subject.method_1019(right.method_1021(placementOffset)).method_1031(0.0, horizontal * headroom, 0.0);
        };
    }

    private ShotComposition compositionFor(SceneSubject subject, boolean wide, double radius, double sweep) {
        ScreenPlacement placement;
        Framing framing = wide ? (radius > 34.0 ? Framing.EXTREME_WIDE : Framing.WIDE) : (subject.type() == SubjectType.PLAYER_DETAIL || radius < 4.2 ? Framing.DETAIL : (radius < 7.0 ? Framing.CLOSE : (radius < 15.0 ? Framing.MEDIUM : Framing.WIDE)));
        if (framing == Framing.DETAIL && this.random.nextDouble() < 0.3) {
            placement = ScreenPlacement.CENTER;
        } else {
            ScreenPlacement screenPlacement = placement = this.random.nextBoolean() ? ScreenPlacement.LEFT_THIRD : ScreenPlacement.RIGHT_THIRD;
        }
        int movementDirection = sweep > 0.001 ? 1 : (sweep < -0.001 ? -1 : 0);
        boolean rackFocus = subject.isGroup() && CinecraftConfig.INSTANCE.focusEffects() && this.random.nextDouble() < 0.68;
        return new ShotComposition(framing, placement, movementDirection, subject.action(), rackFocus);
    }

    private static long configuredDuration(long baseDuration, EntityAction action) {
        double actionFactor = switch (action) {
            case EntityAction.RUNNING, EntityAction.COMBAT -> 0.78;
            case EntityAction.FLYING, EntityAction.SWIMMING -> 0.86;
            case EntityAction.SLEEPING, EntityAction.STILL -> 1.08;
            default -> 1.0;
        };
        return CinecraftConfig.INSTANCE.effectiveDuration(Math.round((double)baseDuration * actionFactor));
    }

    private static double aerialChance(EnvironmentProfile profile) {
        double chance = 0.46;
        if (profile.biomeMood() == BiomeMood.MOUNTAIN || profile.biomeMood() == BiomeMood.OCEAN) {
            chance += 0.16;
        }
        if (profile.biomeMood() == BiomeMood.FOREST) {
            chance -= 0.1;
        }
        if (profile.sceneTime() == SceneTime.SUNRISE || profile.sceneTime() == SceneTime.SUNSET) {
            chance += 0.08;
        }
        if (profile.weather() == SceneWeather.THUNDER) {
            chance -= 0.16;
        }
        return ShotPlanner.clamp(chance, 0.18, 0.72);
    }

    private static boolean framingIsUsable(ShotPlan plan, boolean aerial, boolean wide) {
        for (int sample = 0; sample <= 8; ++sample) {
            double progress = (double)sample / 8.0;
            class_243 camera = plan.path().sample(progress);
            class_243 focus = plan.subjectPath().sample(progress);
            double horizontal = ShotPlanner.horizontalDistance(camera, focus);
            double downwardAngle = Math.toDegrees(Math.atan2(camera.field_1351 - focus.field_1351, Math.max(0.001, horizontal)));
            if (aerial && (horizontal < 14.0 || downwardAngle > 48.0)) {
                return false;
            }
            if (!wide || !(downwardAngle > 62.0)) continue;
            return false;
        }
        return true;
    }

    private static long durationFor(double pathLength, double sweep, boolean enclosed, boolean wide, boolean aerial) {
        double minimum;
        double calculated = pathLength * 500.0 + Math.toDegrees(sweep) * 650.0;
        double d = aerial ? 44000.0 : (wide ? 36000.0 : (minimum = enclosed ? 24000.0 : 30000.0));
        double maximum = aerial ? 66000.0 : (wide ? 58000.0 : (enclosed ? 36000.0 : 46000.0));
        return Math.round(ShotPlanner.clamp(calculated, minimum, maximum));
    }

    private static ShotType chooseIntent(SceneSubject subject, EnvironmentProfile profile, boolean wide, boolean aerial, double radialDrift) {
        if (profile.underground()) {
            return ShotType.PROCEDURAL_INTERIOR;
        }
        if (aerial) {
            return ShotType.PROCEDURAL_AERIAL;
        }
        if (wide) {
            return ShotType.PROCEDURAL_PANORAMA;
        }
        if (Math.abs(radialDrift) > 0.13) {
            return ShotType.PROCEDURAL_REVEAL;
        }
        if (subject.type() == SubjectType.LANDSCAPE) {
            return ShotType.PROCEDURAL_TRAVERSE;
        }
        return ShotType.PROCEDURAL_SUBJECT;
    }

    private double score(GeneratedPlan generated, EnvironmentProfile profile, boolean wide) {
        double lensJump;
        double score = this.random.nextDouble(0.0, 10.0);
        score += Math.min(25.0, generated.pathLength() * (wide ? 0.68 : 0.42));
        score += Math.toDegrees(generated.sweep()) * (wide ? 0.2 : 0.09);
        if (wide) {
            score += generated.radius() * 0.48;
            score += Math.min(9.0, profile.terrainRelief() * 0.3);
            score += profile.waterCoverage() * 10.0;
        }
        if (generated.aerial()) {
            score += 8.0;
        }
        if (generated.passage()) {
            score += 11.0;
        }
        if (wide && (profile.sceneTime() == SceneTime.SUNRISE || profile.sceneTime() == SceneTime.SUNSET)) {
            score += 8.0;
        }
        if (wide && (profile.biomeMood() == BiomeMood.MOUNTAIN || profile.biomeMood() == BiomeMood.OCEAN)) {
            score += 6.0;
        }
        if (generated.passage() && (profile.biomeMood() == BiomeMood.FOREST || profile.underground())) {
            score += 7.0;
        }
        if (profile.weather() == SceneWeather.THUNDER && generated.plan().composition().framing() == Framing.WIDE) {
            score += 5.0;
        }
        if (generated.plan().composition().placement() != ScreenPlacement.CENTER) {
            score += 3.0;
        }
        for (PathSignature recent : this.recentPaths) {
            if (recent.equals(generated.signature())) {
                score -= 48.0;
                continue;
            }
            if (recent.sector() != generated.signature().sector()) continue;
            score -= 4.5;
        }
        ShotComposition composition = generated.plan().composition();
        if (this.lastComposition != null) {
            int framingStep = Math.abs(composition.framing().ordinal() - this.lastComposition.framing().ordinal());
            if (framingStep == 0) {
                score -= 8.0;
            } else if (framingStep == 1) {
                score += 5.0;
            } else if (framingStep >= 3) {
                score -= 9.0;
            }
            if (composition.placement() == this.lastComposition.placement()) {
                score -= 2.5;
            }
            if (composition.action().moving() && composition.movementDirection() != 0 && this.lastComposition.movementDirection() != 0 && composition.movementDirection() != this.lastComposition.movementDirection()) {
                score -= 10.0;
            }
        }
        if (this.lastShotType == generated.plan().type()) {
            score -= 4.0;
        }
        float openingFov = generated.plan().fovPath().sample(0.0);
        if (Float.isFinite(this.lastFov) && (lensJump = (double)Math.abs(openingFov - this.lastFov)) > 22.0) {
            score -= (lensJump - 22.0) * 1.35;
        }
        return score;
    }

    private void remember(PathSignature signature, ShotPlan plan, double score) {
        this.recentPaths.remove(signature);
        this.recentPaths.addFirst(signature);
        while (this.recentPaths.size() > 12) {
            this.recentPaths.removeLast();
        }
        this.lastComposition = plan.composition();
        this.lastShotType = plan.type();
        this.lastFov = plan.fovPath().sample(1.0);
        this.diagnosticSource = plan.source();
        this.diagnosticDecision = String.format(Locale.ROOT, "%s %s %.1f", new Object[]{plan.composition().framing(), plan.composition().placement(), score});
    }

    public void reset() {
        this.recentPaths.clear();
        this.recentPresets.clear();
        this.lastComposition = null;
        this.lastShotType = null;
        this.lastFov = Float.NaN;
        this.diagnosticSource = "none";
        this.diagnosticDecision = "waiting";
        this.diagnosticRejected = 0;
    }

    public String diagnosticSource() {
        return this.diagnosticSource;
    }

    public String diagnosticDecision() {
        return this.diagnosticDecision;
    }

    public int diagnosticRejected() {
        return this.diagnosticRejected;
    }

    private void rememberPreset(String name) {
        this.recentPresets.remove(name);
        this.recentPresets.addFirst(name);
        while (this.recentPresets.size() > 12) {
            this.recentPresets.removeLast();
        }
    }

    private static PathSignature signature(double angle, double radius, double height, double sweep) {
        int direction = sweep > 0.0 ? 1 : (sweep < 0.0 ? -1 : 0);
        return new PathSignature((int)Math.floor(ShotPlanner.normalizeAngle(angle) / 0.5235987755982988), (int)Math.floor(radius / 6.0), (int)Math.floor(height / 6.0), direction);
    }

    private static class_243 pathCenterForSubject(class_243 subject, class_243 player, double maximumDistance) {
        double distance = ShotPlanner.horizontalDistance(subject, player);
        double allowedOffset = maximumDistance * 0.3;
        if (distance <= maximumDistance * 0.52 || distance < 0.001) {
            return subject;
        }
        return player.method_35590(subject, Math.min(1.0, allowedOffset / distance));
    }

    private double between(double minimum, double maximum) {
        if (maximum <= minimum) {
            return Math.max(0.5, maximum);
        }
        return this.random.nextDouble(minimum, maximum);
    }

    private static double approximateLength(CameraPath path) {
        class_243 previous = path.sample(0.0);
        double length = 0.0;
        for (int sample = 1; sample <= 24; ++sample) {
            class_243 current = path.sample((double)sample / 24.0);
            length += previous.method_1022(current);
            previous = current;
        }
        return length;
    }

    private static double horizontalDistance(class_243 first, class_243 second) {
        double dx = first.field_1352 - second.field_1352;
        double dz = first.field_1350 - second.field_1350;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double normalizeAngle(double angle) {
        double normalized = angle % (Math.PI * 2);
        return normalized < 0.0 ? normalized + Math.PI * 2 : normalized;
    }

    private static double radians(double degrees) {
        return Math.toRadians(degrees);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static /* synthetic */ class_243 lambda$generate$4(CameraPath cameraPath, double lookAheadProgress, double progress) {
        return cameraPath.sample(Math.min(1.0, progress + lookAheadProgress));
    }

    @Environment(value=EnvType.CLIENT)
    private record GeneratedPlan(ShotPlan plan, PathSignature signature, double radius, double sweep, double pathLength, boolean aerial, boolean passage) {
    }

    @Environment(value=EnvType.CLIENT)
    private record ScoredPlan(ShotPlan plan, PathSignature signature, double score) {
    }

    @Environment(value=EnvType.CLIENT)
    private record PathSignature(int sector, int radiusBand, int heightBand, int direction) {
    }
}


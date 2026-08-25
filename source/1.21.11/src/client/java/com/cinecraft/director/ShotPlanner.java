package com.cinecraft.director;

import com.cinecraft.camera.CameraPath;
import com.cinecraft.camera.FovPath;
import com.cinecraft.camera.PanPath;
import com.cinecraft.camera.RailPath;
import com.cinecraft.config.CinecraftConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/** Blends curated cinematic compositions with scored scene-specific coverage. */
public final class ShotPlanner {
    private static final int RECENT_PATH_LIMIT = 12;
    private final ArrayDeque<PathSignature> recentPaths = new ArrayDeque<>();
    private final ArrayDeque<String> recentPresets = new ArrayDeque<>();
    private final Random random = new Random();
    private ShotComposition lastComposition;
    private ShotType lastShotType;
    private float lastFov = Float.NaN;
    private String diagnosticSource = "none";
    private String diagnosticDecision = "waiting";
    private int diagnosticRejected;

    public Optional<ShotPlan> plan(
            MinecraftClient client,
            SceneScanner scanner,
            SceneSubject subject,
            EnvironmentProfile profile,
            boolean preferWide
    ) {
        boolean wide = preferWide && profile.supportsWideShots();
        diagnosticRejected = 0;
        if (random.nextDouble() < 0.56) {
            Optional<ShotPlan> libraryPlan = planFromLibrary(client, scanner, subject, profile, wide);
            if (libraryPlan.isPresent()) return libraryPlan;
        }

        List<ScoredPlan> survivors = new ArrayList<>();
        int routedAttempts = 0;
        for (int attempt = 0; attempt < CinecraftConfig.INSTANCE.quality().candidateCount(); attempt++) {
            GeneratedPlan raw = generate(subject, profile, wide);
            boolean allowRouting = !raw.passage() || routedAttempts++ < 4;
            GeneratedPlan generated = adaptToScene(
                    client,
                    scanner,
                    raw,
                    subject,
                    profile,
                    wide,
                    allowRouting
            );
            if (!framingIsUsable(generated.plan(), generated.aerial(), wide)) {
                diagnosticRejected++;
                continue;
            }
            if (!scanner.isPathUsable(
                    client,
                    generated.plan().path(),
                    generated.plan().focusPath(),
                    generated.plan().subjectPath(),
                    profile
            )) {
                diagnosticRejected++;
                continue;
            }
            survivors.add(new ScoredPlan(
                    generated.plan(),
                    generated.signature(),
                    score(generated, profile, wide)
                            + scanner.compositionScore(client, generated.plan(), profile)
            ));
        }

        return survivors.stream()
                .max(Comparator.comparingDouble(ScoredPlan::score))
                .map(selected -> {
                    remember(selected.signature(), selected.plan(), selected.score());
                    return selected.plan();
                });
    }

    private Optional<ShotPlan> planFromLibrary(
            MinecraftClient client,
            SceneScanner scanner,
            SceneSubject subject,
            EnvironmentProfile profile,
            boolean wide
    ) {
        List<ShotPreset> matching = new ArrayList<>(ShotLibrary.matching(
                subject.type(),
                wide,
                profile
        ));
        java.util.Collections.shuffle(matching, random);
        matching.sort(Comparator.comparing(preset -> preset.environment() == ShotEnvironment.ANY));
        List<ShotPreset> fresh = matching.stream()
                .filter(preset -> !recentPresets.contains(preset.name()))
                .toList();
        if (!fresh.isEmpty()) matching = new ArrayList<>(fresh);

        for (ShotPreset preset : matching) {
            for (int attempt = 0; attempt < 10; attempt++) {
                GeneratedPlan generated = instantiatePreset(subject, profile, preset);
                if (generated == null) continue;
                generated = adaptToScene(client, scanner, generated, subject, profile, wide, true);
                if (!framingIsUsable(generated.plan(), generated.aerial(), wide)) continue;
                if (!scanner.isPathUsable(
                        client,
                        generated.plan().path(),
                        generated.plan().focusPath(),
                        generated.plan().subjectPath(),
                        profile
                )) {
                    diagnosticRejected++;
                    continue;
                }
                double selectedScore = score(generated, profile, wide)
                        + scanner.compositionScore(client, generated.plan(), profile);
                remember(generated.signature(), generated.plan(), selectedScore);
                rememberPreset(preset.name());
                return Optional.of(generated.plan());
            }
        }
        return Optional.empty();
    }

    private GeneratedPlan adaptToScene(
            MinecraftClient client,
            SceneScanner scanner,
            GeneratedPlan generated,
            SceneSubject subject,
            EnvironmentProfile profile,
            boolean wide,
            boolean allowRouting
    ) {
        ShotPlan original = generated.plan();
        CameraPath cameraPath = original.path();
        CameraPath subjectPath = original.subjectPath();
        String source = original.source();

        if (generated.passage() && allowRouting) {
            Optional<CameraPath> routed = scanner.findNavigablePath(
                    client,
                    cameraPath.sample(0.0),
                    cameraPath.sample(1.0)
            );
            if (routed.isPresent()) {
                cameraPath = routed.get();
                CameraPath finalRoute = cameraPath;
                subjectPath = progress -> finalRoute.sample(Math.min(1.0, progress + 0.16));
                source += ":air-route";
            }
        } else if (!profile.underground()
                && !generated.aerial()
                && (wide || subject.type() == SubjectType.LANDSCAPE)) {
            double clearance = wide ? clamp(5.5 + profile.terrainRelief() * 0.08, 5.5, 10.0) : 3.2;
            cameraPath = scanner.terrainFollowingPath(client, cameraPath, clearance);
            source += ":terrain";
        }

        CameraPath focusPath = composeFocus(cameraPath, subjectPath, original.composition(), original.fovPath());
        ShotPlan adapted = new ShotPlan(
                original.type(),
                cameraPath,
                focusPath,
                subjectPath,
                original.fovPath(),
                original.durationMillis(),
                original.composition(),
                source
        );
        return new GeneratedPlan(
                adapted,
                generated.signature(),
                generated.radius(),
                generated.sweep(),
                approximateLength(cameraPath),
                generated.aerial(),
                generated.passage()
        );
    }

    private GeneratedPlan instantiatePreset(
            SceneSubject subject,
            EnvironmentProfile profile,
            ShotPreset preset
    ) {
        Vec3d center = preset.wide()
                ? profile.landscapeCenter()
                : pathCenterForSubject(subject.target(), profile.playerFocus(), profile.maxCameraDistance());
        double centerDistance = horizontalDistance(center, profile.playerFocus());
        double localMaximum = Math.max(1.8, profile.maxCameraDistance() - centerDistance - 0.5);
        double radius = Math.min(preset.radius(), localMaximum);
        if (radius < preset.radius() * 0.68) return null;

        double height = Math.min(preset.height(), profile.maxVerticalRise() - 1.5);
        if (preset.aerial() && height < 18.0) return null;
        double baseAngle;
        if (preset.wide()) {
            baseAngle = between(0.0, Math.PI * 2.0);
        } else {
            Vec3d towardPlayer = profile.playerFocus().subtract(center);
            baseAngle = Math.atan2(towardPlayer.z, towardPlayer.x) + between(-0.9, 0.9);
        }
        double sweep = preset.motionStyle() == MotionStyle.PAN ? radians(preset.sweepDegrees()) : 0.0;
        if (random.nextBoolean()) sweep = -sweep;
        double startingRadius = radius;
        double endingRadius = radius * (1.0 + preset.radialChange());
        CameraPath cameraPath = new PanPath(
                center,
                baseAngle,
                sweep,
                startingRadius,
                endingRadius,
                center.y + height,
                center.y + height
        );
        ShotComposition composition = compositionFor(subject, preset.wide(), radius, sweep);
        CameraPath subjectPath = createSubjectPath(
                subject,
                profile,
                preset.wide(),
                preset.aerial(),
                preset.motionStyle() == MotionStyle.HOLD,
                composition.rackFocus()
        );
        ShotType type = chooseIntent(
                subject,
                profile,
                preset.wide(),
                preset.aerial(),
                preset.radialChange()
        );
        float endingFov = (float) (preset.startingFov()
                + (preset.endingFov() - preset.startingFov()) * CinecraftConfig.INSTANCE.zoomStrength());
        FovPath fovPath = FovPath.linear(preset.startingFov(), endingFov);
        CameraPath focusPath = composeFocus(cameraPath, subjectPath, composition, fovPath);
        long duration = configuredDuration(preset.durationMillis(), subject.action());
        PathSignature signature = signature(baseAngle, radius, height, sweep);
        return new GeneratedPlan(
                new ShotPlan(
                        type,
                        cameraPath,
                        focusPath,
                        subjectPath,
                        fovPath,
                        duration,
                        composition,
                        "library:" + preset.name()
                ),
                signature,
                radius,
                Math.abs(sweep),
                approximateLength(cameraPath),
                preset.aerial(),
                false
        );
    }

    private GeneratedPlan generate(SceneSubject subject, EnvironmentProfile profile, boolean wide) {
        double maximum = profile.maxCameraDistance();
        Vec3d center = wide
                ? profile.landscapeCenter()
                : pathCenterForSubject(subject.target(), profile.playerFocus(), maximum);
        boolean enclosed = profile.underground();
        boolean transitional = profile.spaceType() == SpaceType.TRANSITIONAL;
        boolean aerial = wide
                && !profile.underground()
                && CinecraftConfig.INSTANCE.aerialShots()
                && random.nextDouble() < aerialChance(profile);
        double holdChance = subject.action().moving() ? 0.05 : wide ? 0.08 : 0.24;
        boolean hold = random.nextDouble() < holdChance;
        boolean passage = !wide
                && !hold
                && (subject.type() == SubjectType.FEATURE || subject.type() == SubjectType.LANDSCAPE)
                && random.nextDouble() < 0.44;
        double centerDistance = horizontalDistance(center, profile.playerFocus());
        double localMaximum = Math.max(1.8, maximum - centerDistance - 0.5);

        double radius;
        if (wide) {
            double upper = Math.max(14.0, maximum - 3.0);
            double sceneDriven = clamp(profile.sceneRadius() * 0.85, 14.0, upper);
            radius = between(Math.max(10.0, sceneDriven * 0.72), sceneDriven);
        } else if (subject.type() == SubjectType.PLAYER_DETAIL) {
            double minimum = enclosed ? 2.6 : 3.4;
            radius = between(Math.min(minimum, localMaximum), Math.min(enclosed ? 4.8 : 5.8, localMaximum));
        } else if (subject.type() == SubjectType.PLAYER) {
            double minimum = enclosed ? 3.5 : 9.5;
            radius = between(Math.min(minimum, localMaximum), Math.min(enclosed ? 7.0 : 18.0, localMaximum));
        } else if (subject.type() == SubjectType.GROUP) {
            radius = between(Math.min(enclosed ? 4.0 : 8.0, localMaximum), Math.min(enclosed ? 7.0 : 18.0, localMaximum));
        } else if (enclosed) {
            radius = between(1.15, Math.min(4.2, localMaximum));
        } else if (transitional) {
            radius = between(3.2, Math.min(10.5, localMaximum));
        } else {
            radius = between(4.2, Math.min(13.0, localMaximum));
        }

        double baseAngle;
        Vec3d movement = subject.movementVector();
        if (wide) {
            baseAngle = between(0.0, Math.PI * 2.0);
        } else if (subject.action().moving() && movement.horizontalLengthSquared() > 0.001) {
            double movementAngle = Math.atan2(movement.z, movement.x);
            baseAngle = movementAngle + (random.nextBoolean() ? Math.PI * 0.5 : -Math.PI * 0.5)
                    + between(-0.42, 0.42);
        } else {
            Vec3d towardPlayer = profile.playerFocus().subtract(center);
            baseAngle = Math.atan2(towardPlayer.z, towardPlayer.x) + between(-1.15, 1.15);
        }

        double sweepMagnitude = enclosed
                ? between(radians(6.0), radians(20.0))
                : aerial
                ? between(radians(18.0), radians(48.0))
                : wide
                ? between(radians(24.0), radians(66.0))
                : between(radians(12.0), radians(42.0));
        double sweep = hold ? 0.0 : random.nextBoolean() ? sweepMagnitude : -sweepMagnitude;
        double radialDrift = aerial
                ? between(-0.12, 0.12)
                : wide
                ? between(-0.22, 0.22)
                : between(-0.18, 0.18);
        if (hold) radialDrift = 0.0;

        double heightAboveFocus;
        if (enclosed) {
            heightAboveFocus = between(0.15, 1.25);
        } else if (subject.type() == SubjectType.PLAYER_DETAIL) {
            heightAboveFocus = between(-0.15, 0.65);
        } else if (aerial) {
            double maximumAerialHeight = Math.min(
                    profile.maxVerticalRise() - 2.0,
                    Math.max(30.0, radius * 1.20 + profile.terrainRelief() * 0.16)
            );
            heightAboveFocus = between(20.0, maximumAerialHeight);
        } else if (wide) {
            heightAboveFocus = clamp(
                    radius * between(0.18, 0.36) + profile.terrainRelief() * 0.08,
                    4.5,
                    16.0
            );
        } else {
            heightAboveFocus = between(0.45, Math.min(4.5, profile.maxVerticalRise() * 0.30));
        }
        double heightDrift = enclosed
                ? between(-0.45, 0.45)
                : aerial
                ? between(-2.0, 2.0)
                : wide
                ? between(-1.6, 1.6)
                : between(-1.2, 1.2);
        if (hold) heightDrift = 0.0;

        CameraPath cameraPath;
        CameraPath subjectPath;
        long duration;
        ShotType type;
        ShotComposition composition = compositionFor(subject, wide, radius, sweep);
        if (passage) {
            double travel = enclosed ? between(6.0, 12.0) : between(10.0, 22.0);
            double travelAngle = baseAngle + between(-0.65, 0.65);
            Vec3d forward = new Vec3d(Math.cos(travelAngle), 0.0, Math.sin(travelAngle));
            Vec3d side = new Vec3d(-forward.z, 0.0, forward.x).multiply(between(-2.2, 2.2));
            double railY = center.y + clamp(heightAboveFocus, 0.7, 2.4);
            Vec3d start = new Vec3d(center.x, railY, center.z)
                    .subtract(forward.multiply(travel * 0.45))
                    .add(side);
            Vec3d end = new Vec3d(center.x, railY + heightDrift * 0.35, center.z)
                    .add(forward.multiply(travel * 0.55))
                    .add(side.multiply(0.55));
            cameraPath = new RailPath(start, end);
            double lookAheadProgress = clamp(between(5.0, 9.0) / Math.max(6.0, travel), 0.16, 0.42);
            subjectPath = progress -> cameraPath.sample(Math.min(1.0, progress + lookAheadProgress));
            duration = Math.round(clamp(travel * 2_200.0, 30_000.0, 52_000.0));
            type = ShotType.PROCEDURAL_PASSAGE;
        } else {
            double startingRadius = radius * (1.0 - radialDrift * 0.5);
            double endingRadius = radius * (1.0 + radialDrift * 0.5);
            double startingY = center.y + heightAboveFocus - heightDrift * 0.5;
            double endingY = center.y + heightAboveFocus + heightDrift * 0.5;
            cameraPath = new PanPath(
                    center,
                    baseAngle,
                    sweep,
                    startingRadius,
                    endingRadius,
                    startingY,
                    endingY
            );
            subjectPath = createSubjectPath(subject, profile, wide, aerial, hold, composition.rackFocus());
            double panLength = approximateLength(cameraPath);
            duration = hold
                    ? Math.round(between(wide ? 24_000.0 : 18_000.0, wide ? 34_000.0 : 28_000.0))
                    : durationFor(panLength, Math.abs(sweep), enclosed, wide, aerial);
            type = chooseIntent(subject, profile, wide, aerial, radialDrift);
        }
        double pathLength = approximateLength(cameraPath);
        FovPath fovPath = generatedFov(subject, wide, aerial, passage, hold);
        CameraPath focusPath = composeFocus(cameraPath, subjectPath, composition, fovPath);
        duration = configuredDuration(duration, subject.action());
        PathSignature signature = signature(baseAngle, radius, heightAboveFocus, sweep);
        return new GeneratedPlan(
                new ShotPlan(
                        type,
                        cameraPath,
                        focusPath,
                        subjectPath,
                        fovPath,
                        duration,
                        composition,
                        "procedural"
                ),
                signature,
                radius,
                Math.abs(sweep),
                pathLength,
                aerial,
                passage
        );
    }

    private FovPath generatedFov(
            SceneSubject subject,
            boolean wide,
            boolean aerial,
            boolean passage,
            boolean hold
    ) {
        double minimum;
        double maximum;
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

        float start = (float) between(minimum, maximum);
        boolean zoom = passage
                || subject.type() == SubjectType.PLAYER_DETAIL
                || (!hold && random.nextDouble() < 0.58);
        if (!zoom) return FovPath.fixed(start);

        double magnitude = passage
                ? between(9.0, 17.0)
                : subject.type() == SubjectType.PLAYER_DETAIL
                ? between(6.0, 12.0)
                : wide
                ? between(5.0, 11.0)
                : between(4.0, 9.0);
        double direction = random.nextBoolean() ? 1.0 : -1.0;
        float end = (float) clamp(
                start + direction * magnitude * CinecraftConfig.INSTANCE.zoomStrength(),
                24.0,
                90.0
        );
        return FovPath.linear(start, end);
    }

    private CameraPath createSubjectPath(
            SceneSubject subject,
            EnvironmentProfile profile,
            boolean wide,
            boolean aerial,
            boolean hold,
            boolean rackFocus
    ) {
        if (!wide || profile.landscapeAnchors().size() < 3) {
            return rackFocus ? subject::rackFocusTarget : progress -> subject.currentTarget();
        }

        List<Vec3d> anchors = profile.landscapeAnchors();
        Vec3d first = anchors.get(random.nextInt(anchors.size()));
        Vec3d center = profile.landscapeCenter();
        double minimumBlend = aerial ? 0.62 : 0.32;
        double maximumBlend = aerial ? 0.96 : 0.62;
        double lift = aerial ? 3.0 + profile.terrainRelief() * 0.07 : 1.3 + profile.terrainRelief() * 0.04;
        Vec3d openingFocus = center.lerp(first, between(minimumBlend, maximumBlend)).add(0.0, lift, 0.0);
        if (hold) return progress -> openingFocus;

        Vec3d second = anchors.get(random.nextInt(anchors.size()));
        for (int retry = 0; retry < 5 && first.squaredDistanceTo(second) < 144.0; retry++) {
            second = anchors.get(random.nextInt(anchors.size()));
        }
        Vec3d closingFocus = center.lerp(second, between(minimumBlend, maximumBlend)).add(0.0, lift, 0.0);
        return progress -> openingFocus.lerp(closingFocus, progress);
    }

    private CameraPath composeFocus(
            CameraPath cameraPath,
            CameraPath subjectPath,
            ShotComposition composition,
            FovPath fovPath
    ) {
        return progress -> {
            Vec3d camera = cameraPath.sample(progress);
            Vec3d subject = subjectPath.sample(progress);
            Vec3d view = subject.subtract(camera);
            double horizontal = Math.sqrt(view.x * view.x + view.z * view.z);
            if (horizontal < 0.001) return subject;
            Vec3d right = new Vec3d(-view.z / horizontal, 0.0, view.x / horizontal);
            double fovScale = Math.tan(Math.toRadians(clamp(fovPath.sample(progress), 24.0, 90.0) * 0.5));
            double placementOffset = composition.placement().direction()
                    * Math.min(3.5, horizontal * fovScale * 0.16);
            double headroom = switch (composition.framing()) {
                case DETAIL -> -0.02;
                case CLOSE -> -0.035;
                case MEDIUM -> -0.055;
                case WIDE, EXTREME_WIDE -> -0.075;
            };
            return subject.add(right.multiply(placementOffset)).add(0.0, horizontal * headroom, 0.0);
        };
    }

    private ShotComposition compositionFor(
            SceneSubject subject,
            boolean wide,
            double radius,
            double sweep
    ) {
        Framing framing;
        if (wide) framing = radius > 34.0 ? Framing.EXTREME_WIDE : Framing.WIDE;
        else if (subject.type() == SubjectType.PLAYER_DETAIL || radius < 4.2) framing = Framing.DETAIL;
        else if (radius < 7.0) framing = Framing.CLOSE;
        else if (radius < 15.0) framing = Framing.MEDIUM;
        else framing = Framing.WIDE;

        ScreenPlacement placement;
        if (framing == Framing.DETAIL && random.nextDouble() < 0.30) {
            placement = ScreenPlacement.CENTER;
        } else {
            placement = random.nextBoolean() ? ScreenPlacement.LEFT_THIRD : ScreenPlacement.RIGHT_THIRD;
        }
        int movementDirection = sweep > 0.001 ? 1 : sweep < -0.001 ? -1 : 0;
        boolean rackFocus = subject.isGroup()
                && CinecraftConfig.INSTANCE.focusEffects()
                && random.nextDouble() < 0.68;
        return new ShotComposition(framing, placement, movementDirection, subject.action(), rackFocus);
    }

    private static long configuredDuration(long baseDuration, EntityAction action) {
        double actionFactor = switch (action) {
            case RUNNING, COMBAT -> 0.78;
            case FLYING, SWIMMING -> 0.86;
            case SLEEPING, STILL -> 1.08;
            default -> 1.0;
        };
        return CinecraftConfig.INSTANCE.effectiveDuration(Math.round(baseDuration * actionFactor));
    }

    private static double aerialChance(EnvironmentProfile profile) {
        double chance = 0.46;
        if (profile.biomeMood() == BiomeMood.MOUNTAIN || profile.biomeMood() == BiomeMood.OCEAN) chance += 0.16;
        if (profile.biomeMood() == BiomeMood.FOREST) chance -= 0.10;
        if (profile.sceneTime() == SceneTime.SUNRISE || profile.sceneTime() == SceneTime.SUNSET) chance += 0.08;
        if (profile.weather() == SceneWeather.THUNDER) chance -= 0.16;
        return clamp(chance, 0.18, 0.72);
    }

    private static boolean framingIsUsable(ShotPlan plan, boolean aerial, boolean wide) {
        for (int sample = 0; sample <= 8; sample++) {
            double progress = sample / 8.0;
            Vec3d camera = plan.path().sample(progress);
            Vec3d focus = plan.subjectPath().sample(progress);
            double horizontal = horizontalDistance(camera, focus);
            double downwardAngle = Math.toDegrees(Math.atan2(camera.y - focus.y, Math.max(0.001, horizontal)));
            if (aerial && (horizontal < 14.0 || downwardAngle > 48.0)) return false;
            if (wide && downwardAngle > 62.0) return false;
        }
        return true;
    }

    private static long durationFor(
            double pathLength,
            double sweep,
            boolean enclosed,
            boolean wide,
            boolean aerial
    ) {
        double calculated = pathLength * 500.0 + Math.toDegrees(sweep) * 650.0;
        double minimum = aerial ? 44_000.0 : wide ? 36_000.0 : enclosed ? 24_000.0 : 30_000.0;
        double maximum = aerial ? 66_000.0 : wide ? 58_000.0 : enclosed ? 36_000.0 : 46_000.0;
        return Math.round(clamp(calculated, minimum, maximum));
    }

    private static ShotType chooseIntent(
            SceneSubject subject,
            EnvironmentProfile profile,
            boolean wide,
            boolean aerial,
            double radialDrift
    ) {
        if (profile.underground()) return ShotType.PROCEDURAL_INTERIOR;
        if (aerial) return ShotType.PROCEDURAL_AERIAL;
        if (wide) return ShotType.PROCEDURAL_PANORAMA;
        if (Math.abs(radialDrift) > 0.13) return ShotType.PROCEDURAL_REVEAL;
        if (subject.type() == SubjectType.LANDSCAPE) return ShotType.PROCEDURAL_TRAVERSE;
        return ShotType.PROCEDURAL_SUBJECT;
    }

    private double score(GeneratedPlan generated, EnvironmentProfile profile, boolean wide) {
        double score = random.nextDouble(0.0, 10.0);
        score += Math.min(25.0, generated.pathLength() * (wide ? 0.68 : 0.42));
        score += Math.toDegrees(generated.sweep()) * (wide ? 0.20 : 0.09);
        if (wide) {
            score += generated.radius() * 0.48;
            score += Math.min(9.0, profile.terrainRelief() * 0.30);
            score += profile.waterCoverage() * 10.0;
        }
        if (generated.aerial()) score += 8.0;
        if (generated.passage()) score += 11.0;
        if (wide && (profile.sceneTime() == SceneTime.SUNRISE || profile.sceneTime() == SceneTime.SUNSET)) score += 8.0;
        if (wide && (profile.biomeMood() == BiomeMood.MOUNTAIN || profile.biomeMood() == BiomeMood.OCEAN)) score += 6.0;
        if (generated.passage() && (profile.biomeMood() == BiomeMood.FOREST || profile.underground())) score += 7.0;
        if (profile.weather() == SceneWeather.THUNDER && generated.plan().composition().framing() == Framing.WIDE) score += 5.0;
        if (generated.plan().composition().placement() != ScreenPlacement.CENTER) score += 3.0;
        for (PathSignature recent : recentPaths) {
            if (recent.equals(generated.signature())) score -= 48.0;
            else if (recent.sector() == generated.signature().sector()) score -= 4.5;
        }

        ShotComposition composition = generated.plan().composition();
        if (lastComposition != null) {
            int framingStep = Math.abs(composition.framing().ordinal() - lastComposition.framing().ordinal());
            if (framingStep == 0) score -= 8.0;
            else if (framingStep == 1) score += 5.0;
            else if (framingStep >= 3) score -= 9.0;
            if (composition.placement() == lastComposition.placement()) score -= 2.5;
            if (composition.action().moving()
                    && composition.movementDirection() != 0
                    && lastComposition.movementDirection() != 0
                    && composition.movementDirection() != lastComposition.movementDirection()) {
                score -= 10.0;
            }
        }
        if (lastShotType == generated.plan().type()) score -= 4.0;
        float openingFov = generated.plan().fovPath().sample(0.0);
        if (Float.isFinite(lastFov)) {
            double lensJump = Math.abs(openingFov - lastFov);
            if (lensJump > 22.0) score -= (lensJump - 22.0) * 1.35;
        }
        return score;
    }

    private void remember(PathSignature signature, ShotPlan plan, double score) {
        recentPaths.remove(signature);
        recentPaths.addFirst(signature);
        while (recentPaths.size() > RECENT_PATH_LIMIT) recentPaths.removeLast();
        lastComposition = plan.composition();
        lastShotType = plan.type();
        lastFov = plan.fovPath().sample(1.0);
        diagnosticSource = plan.source();
        diagnosticDecision = String.format(
                java.util.Locale.ROOT,
                "%s %s %.1f",
                plan.composition().framing(),
                plan.composition().placement(),
                score
        );
    }

    public void reset() {
        recentPaths.clear();
        recentPresets.clear();
        lastComposition = null;
        lastShotType = null;
        lastFov = Float.NaN;
        diagnosticSource = "none";
        diagnosticDecision = "waiting";
        diagnosticRejected = 0;
    }

    public String diagnosticSource() { return diagnosticSource; }
    public String diagnosticDecision() { return diagnosticDecision; }
    public int diagnosticRejected() { return diagnosticRejected; }

    private void rememberPreset(String name) {
        recentPresets.remove(name);
        recentPresets.addFirst(name);
        while (recentPresets.size() > 12) recentPresets.removeLast();
    }

    private static PathSignature signature(double angle, double radius, double height, double sweep) {
        int direction = sweep > 0.0 ? 1 : sweep < 0.0 ? -1 : 0;
        return new PathSignature(
                (int) Math.floor(normalizeAngle(angle) / (Math.PI * 2.0 / 12.0)),
                (int) Math.floor(radius / 6.0),
                (int) Math.floor(height / 6.0),
                direction
        );
    }

    private static Vec3d pathCenterForSubject(Vec3d subject, Vec3d player, double maximumDistance) {
        double distance = horizontalDistance(subject, player);
        double allowedOffset = maximumDistance * 0.30;
        if (distance <= maximumDistance * 0.52 || distance < 0.001) return subject;
        return player.lerp(subject, Math.min(1.0, allowedOffset / distance));
    }

    private double between(double minimum, double maximum) {
        if (maximum <= minimum) return Math.max(0.5, maximum);
        return random.nextDouble(minimum, maximum);
    }

    private static double approximateLength(CameraPath path) {
        Vec3d previous = path.sample(0.0);
        double length = 0.0;
        for (int sample = 1; sample <= 24; sample++) {
            Vec3d current = path.sample(sample / 24.0);
            length += previous.distanceTo(current);
            previous = current;
        }
        return length;
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double normalizeAngle(double angle) {
        double normalized = angle % (Math.PI * 2.0);
        return normalized < 0.0 ? normalized + Math.PI * 2.0 : normalized;
    }

    private static double radians(double degrees) {
        return Math.toRadians(degrees);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record GeneratedPlan(
            ShotPlan plan,
            PathSignature signature,
            double radius,
            double sweep,
            double pathLength,
            boolean aerial,
            boolean passage
    ) { }

    private record ScoredPlan(ShotPlan plan, PathSignature signature, double score) { }

    private record PathSignature(int sector, int radiusBand, int heightBand, int direction) { }
}

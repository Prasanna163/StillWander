/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1297
 *  net.minecraft.class_1304
 *  net.minecraft.class_1309
 *  net.minecraft.class_1657
 *  net.minecraft.class_1922
 *  net.minecraft.class_1937
 *  net.minecraft.class_1959$class_1963
 *  net.minecraft.class_2338
 *  net.minecraft.class_2374
 *  net.minecraft.class_2382
 *  net.minecraft.class_239
 *  net.minecraft.class_239$class_240
 *  net.minecraft.class_243
 *  net.minecraft.class_2680
 *  net.minecraft.class_2902$class_2903
 *  net.minecraft.class_310
 *  net.minecraft.class_3959
 *  net.minecraft.class_3959$class_242
 *  net.minecraft.class_3959$class_3960
 *  net.minecraft.class_746
 */
package com.cinecraft.director;

import com.cinecraft.camera.ArcLengthSplinePath;
import com.cinecraft.camera.CameraPath;
import com.cinecraft.camera.CameraPose;
import com.cinecraft.camera.LookAt;
import com.cinecraft.config.CinecraftConfig;
import com.cinecraft.config.QualityPreset;
import com.cinecraft.director.BiomeMood;
import com.cinecraft.director.DimensionMood;
import com.cinecraft.director.EnvironmentProfile;
import com.cinecraft.director.SceneSubject;
import com.cinecraft.director.SceneTime;
import com.cinecraft.director.SceneWeather;
import com.cinecraft.director.ShotPlan;
import com.cinecraft.director.SpaceType;
import com.cinecraft.director.SubjectFocus;
import com.cinecraft.director.SubjectType;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1657;
import net.minecraft.class_1922;
import net.minecraft.class_1937;
import net.minecraft.class_1959;
import net.minecraft.class_2338;
import net.minecraft.class_2374;
import net.minecraft.class_2382;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_2902;
import net.minecraft.class_310;
import net.minecraft.class_3959;
import net.minecraft.class_746;

@Environment(value=EnvType.CLIENT)
public final class SceneScanner {
    private static final int RECENT_SUBJECT_LIMIT = 8;
    private static final double SURVEY_RANGE = 48.0;
    private static final double MINIMUM_CAMERA_GROUND_CLEARANCE = 0.85;
    private static final double MAXIMUM_RUNTIME_CAMERA_LIFT = 2.4;
    private final ArrayDeque<String> recentSubjects = new ArrayDeque();
    private final ArrayDeque<SubjectType> environmentDeck = new ArrayDeque();
    private final Random random = new Random();
    private static final int[][] ROUTE_STEPS = new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}, {0, 1, 0}, {0, -1, 0}};

    public SceneSubject playerSubject(class_310 client) {
        class_746 player = client.field_1724;
        return new SceneSubject(SubjectType.PLAYER, player.method_73189().method_1031(0.0, (double)player.method_17682() * 0.55, 0.0), "player", (class_1297)player);
    }

    public SceneSubject landscapeSubject(EnvironmentProfile profile) {
        class_243 center = profile.landscapeCenter();
        String key = "wide:" + Math.round(center.field_1352 / 8.0) + ":" + Math.round(center.field_1350 / 8.0);
        return new SceneSubject(SubjectType.LANDSCAPE, center, key);
    }

    public SceneSubject playerDetailSubject(class_310 client) {
        class_1304[] armorSlots;
        class_746 player = client.field_1724;
        ArrayList<SubjectFocus> choices = new ArrayList<SubjectFocus>();
        if (!player.method_6047().method_7960()) {
            choices.add(SubjectFocus.MAIN_HAND);
        }
        if (!player.method_6079().method_7960()) {
            choices.add(SubjectFocus.OFF_HAND);
        }
        boolean hasArmor = false;
        for (class_1304 slot : armorSlots = new class_1304[]{class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166}) {
            if (player.method_6118(slot).method_7960()) continue;
            hasArmor = true;
            break;
        }
        if (hasArmor) {
            choices.add(SubjectFocus.HEAD);
            choices.add(SubjectFocus.CHEST);
        }
        if (choices.isEmpty()) {
            choices.add(SubjectFocus.HEAD);
            choices.add(SubjectFocus.CHEST);
        }
        SubjectFocus focus = (SubjectFocus)((Object)choices.get(this.random.nextInt(choices.size())));
        return new SceneSubject(SubjectType.PLAYER_DETAIL, SceneSubject.targetFor((class_1297)player, focus), "player_detail:" + String.valueOf((Object)focus), (class_1297)player, focus);
    }

    public EnvironmentProfile survey(class_310 client) {
        boolean underground;
        class_243 playerFocus = this.playerSubject(client).target();
        double clearanceTotal = 0.0;
        for (int direction = 0; direction < 16; ++direction) {
            double angle = Math.PI * 2 * (double)direction / 16.0;
            class_243 end = playerFocus.method_1031(Math.cos(angle) * 48.0, 0.0, Math.sin(angle) * 48.0);
            clearanceTotal += this.rayDistance(client, playerFocus, end, 48.0);
        }
        double averageClearance = clearanceTotal / 16.0;
        double skyVisibility = this.measureSkyVisibility(client, playerFocus);
        SurfaceSurvey surface = this.surveySurface(client, playerFocus);
        class_2338 playerBlock = client.field_1724.method_24515();
        boolean directlySkyVisible = client.field_1687.method_8311(playerBlock);
        double surfaceDepth = Math.max(0.0, (double)(client.field_1687.method_8624(class_2902.class_2903.field_13197, playerBlock.method_10263(), playerBlock.method_10260()) - playerBlock.method_10264()));
        boolean bl = underground = !directlySkyVisible && skyVisibility < 0.34 && surfaceDepth > 8.0;
        SpaceType spaceType = underground ? SpaceType.ENCLOSED : (skyVisibility < 0.58 || averageClearance < 14.0 ? SpaceType.TRANSITIONAL : (skyVisibility > 0.86 && averageClearance > 30.0 ? SpaceType.VAST : SpaceType.OPEN));
        int viewDistanceChunks = (Integer)client.field_1690.method_42503().method_41753();
        double renderBound = SceneScanner.clamp((double)viewDistanceChunks * 16.0 - 16.0, 16.0, 80.0);
        double spatialBound = switch (spaceType) {
            default -> throw new MatchException(null, null);
            case SpaceType.ENCLOSED -> 6.5;
            case SpaceType.TRANSITIONAL -> 32.0;
            case SpaceType.OPEN -> 48.0;
            case SpaceType.VAST -> 72.0;
        };
        double horizontalBound = Math.min(renderBound, spatialBound);
        double verticalBound = underground ? 5.5 : SceneScanner.clamp(renderBound * 0.95, 32.0, 72.0);
        DimensionMood dimensionMood = SceneScanner.dimensionMood(client);
        BiomeMood biomeMood = SceneScanner.biomeMood(client, playerBlock, underground, dimensionMood);
        return new EnvironmentProfile(spaceType, underground, surfaceDepth, playerFocus, surface.center(), surface.anchors(), averageClearance, skyVisibility, surface.relief(), surface.waterCoverage(), surface.radius(), horizontalBound, verticalBound, biomeMood, SceneScanner.weather(client, playerBlock), SceneScanner.sceneTime(client), dimensionMood);
    }

    private static DimensionMood dimensionMood(class_310 client) {
        if (client.field_1687.method_27983().equals(class_1937.field_25180)) {
            return DimensionMood.NETHER;
        }
        if (client.field_1687.method_27983().equals(class_1937.field_25181)) {
            return DimensionMood.END;
        }
        if (client.field_1687.method_27983().equals(class_1937.field_25179)) {
            return DimensionMood.OVERWORLD;
        }
        return DimensionMood.OTHER;
    }

    private static BiomeMood biomeMood(class_310 client, class_2338 playerBlock, boolean underground, DimensionMood dimension) {
        if (dimension == DimensionMood.NETHER) {
            return BiomeMood.NETHER;
        }
        if (dimension == DimensionMood.END) {
            return BiomeMood.END;
        }
        if (underground) {
            return BiomeMood.CAVE;
        }
        String path = client.field_1687.method_23753(playerBlock).method_40230().map(key -> key.method_29177().method_12832()).orElse("");
        if (SceneScanner.containsAny(path, "snow", "frozen", "ice", "grove")) {
            return BiomeMood.SNOW;
        }
        if (SceneScanner.containsAny(path, "ocean", "beach", "river")) {
            return BiomeMood.OCEAN;
        }
        if (SceneScanner.containsAny(path, "mountain", "peak", "slope", "windswept", "cliff")) {
            return BiomeMood.MOUNTAIN;
        }
        if (SceneScanner.containsAny(path, "forest", "taiga", "jungle", "woods", "cherry")) {
            return BiomeMood.FOREST;
        }
        if (SceneScanner.containsAny(path, "desert", "badlands", "savanna")) {
            return BiomeMood.DESERT;
        }
        if (SceneScanner.containsAny(path, "swamp", "mangrove")) {
            return BiomeMood.SWAMP;
        }
        if (SceneScanner.containsAny(path, "plains", "meadow", "field")) {
            return BiomeMood.PLAINS;
        }
        return BiomeMood.OTHER;
    }

    private static SceneWeather weather(class_310 client, class_2338 playerBlock) {
        if (client.field_1687.method_8546()) {
            return SceneWeather.THUNDER;
        }
        if (!client.field_1687.method_8419()) {
            return SceneWeather.CLEAR;
        }
        return client.field_1687.method_70745(playerBlock) == class_1959.class_1963.field_9383 ? SceneWeather.SNOW : SceneWeather.RAIN;
    }

    private static SceneTime sceneTime(class_310 client) {
        long time = Math.floorMod(client.field_1687.method_8532(), 24000L);
        if (time >= 22500L || time < 1000L) {
            return SceneTime.SUNRISE;
        }
        if (time >= 11500L && time < 13500L) {
            return SceneTime.SUNSET;
        }
        if (time >= 13500L && time < 22500L) {
            return SceneTime.NIGHT;
        }
        return SceneTime.DAY;
    }

    private static boolean containsAny(String value, String ... needles) {
        for (String needle : needles) {
            if (!value.contains(needle)) continue;
            return true;
        }
        return false;
    }

    public SceneSubject findEnvironmentSubject(class_310 client) {
        ArrayList<WeightedSubject> candidates = new ArrayList<WeightedSubject>();
        this.collectEntities(client, candidates);
        this.collectGroups(client, candidates);
        this.collectFeatures(client, candidates);
        this.collectLandscapes(client, candidates);
        candidates.sort(Comparator.comparingDouble(WeightedSubject::score).reversed());
        List<WeightedSubject> pool = this.chooseEnvironmentPool(candidates);
        if (pool.isEmpty()) {
            return this.playerSubject(client);
        }
        SceneSubject selected = pool.get(this.random.nextInt(pool.size())).subject();
        this.remember(selected.key());
        return selected;
    }

    public SceneSubject findSubject(class_310 client, SubjectType desired) {
        if (desired == SubjectType.PLAYER) {
            return this.playerSubject(client);
        }
        ArrayList<WeightedSubject> candidates = new ArrayList<WeightedSubject>();
        this.collectEntities(client, candidates);
        this.collectGroups(client, candidates);
        this.collectFeatures(client, candidates);
        this.collectLandscapes(client, candidates);
        candidates.sort(Comparator.comparingDouble(WeightedSubject::score).reversed());
        List<WeightedSubject> pool = candidates.stream().filter(candidate -> candidate.subject().type() == desired).filter(candidate -> !this.recentSubjects.contains(candidate.subject().key())).limit(5L).toList();
        if (pool.isEmpty()) {
            pool = candidates.stream().filter(candidate -> candidate.subject().type() == desired).limit(5L).toList();
        }
        if (pool.isEmpty()) {
            return this.findEnvironmentSubject(client);
        }
        SceneSubject selected = pool.get(this.random.nextInt(pool.size())).subject();
        this.remember(selected.key());
        return selected;
    }

    public void resetSubjects() {
        this.recentSubjects.clear();
        this.environmentDeck.clear();
    }

    private List<WeightedSubject> chooseEnvironmentPool(List<WeightedSubject> candidates) {
        for (int attempt = 0; attempt < 4; ++attempt) {
            if (this.environmentDeck.isEmpty()) {
                this.refillEnvironmentDeck();
            }
            SubjectType desired = this.environmentDeck.removeFirst();
            List<WeightedSubject> freshOfType = candidates.stream().filter(candidate -> candidate.subject().type() == desired).filter(candidate -> !this.recentSubjects.contains(candidate.subject().key())).limit(5L).toList();
            if (!freshOfType.isEmpty()) {
                return freshOfType;
            }
            List<WeightedSubject> anyOfType = candidates.stream().filter(candidate -> candidate.subject().type() == desired).limit(5L).toList();
            if (anyOfType.isEmpty()) continue;
            return anyOfType;
        }
        return candidates.stream().filter(candidate -> !this.recentSubjects.contains(candidate.subject().key())).limit(5L).toList();
    }

    private void refillEnvironmentDeck() {
        ArrayList<SubjectType> types = new ArrayList<SubjectType>(List.of(SubjectType.ENTITY, SubjectType.GROUP, SubjectType.FEATURE, SubjectType.LANDSCAPE));
        Collections.shuffle(types, this.random);
        this.environmentDeck.addAll(types);
    }

    public CameraPose findPlayerView(class_310 client) {
        double[] radii;
        class_243 target = this.playerSubject(client).target();
        double baseAngle = Math.toRadians((double)client.field_1724.method_36454() + 180.0);
        for (double radius : radii = new double[]{1.15, 1.7, 2.5, 3.5, 4.5, 6.5}) {
            for (int index = 0; index < 12; ++index) {
                double angle = baseAngle + Math.PI * 2 * (double)index / 12.0;
                class_243 camera = new class_243(target.field_1352 + Math.cos(angle) * radius, target.field_1351 + Math.min(1.3, 0.3 + radius * 0.22), target.field_1350 + Math.sin(angle) * radius);
                if (!this.isUsable(client, camera, target)) continue;
                return LookAt.pose(camera, target);
            }
        }
        return null;
    }

    public boolean isPathUsable(class_310 client, CameraPath cameraPath, CameraPath focusPath, CameraPath subjectPath, EnvironmentProfile profile) {
        class_243 playerPosition = client.field_1724.method_73189();
        for (int sample = 0; sample <= 24; ++sample) {
            double progress = (double)sample / 24.0;
            class_243 camera = cameraPath.sample(progress);
            class_243 focus = focusPath.sample(progress);
            class_243 subject = subjectPath.sample(progress);
            double dx = camera.field_1352 - playerPosition.field_1352;
            double dz = camera.field_1350 - playerPosition.field_1350;
            if (dx * dx + dz * dz > profile.maxCameraDistance() * profile.maxCameraDistance()) {
                return false;
            }
            if (Math.abs(camera.field_1351 - playerPosition.field_1351) > profile.maxVerticalRise()) {
                return false;
            }
            if (!this.isUsable(client, camera, focus)) {
                return false;
            }
            if (this.raycast(client, camera, subject).method_17783() == class_239.class_240.field_1333) continue;
            return false;
        }
        return true;
    }

    public double compositionScore(class_310 client, ShotPlan plan, EnvironmentProfile profile) {
        double score = 0.0;
        block0: for (double progress : new double[]{0.15, 0.5, 0.85}) {
            class_2338 subjectBlock;
            boolean sky;
            class_243 backgroundEnd;
            boolean openBackground;
            class_243 camera = plan.path().sample(progress);
            class_243 subject = plan.subjectPath().sample(progress);
            class_243 direction = subject.method_1020(camera);
            if (direction.method_1027() < 0.001) continue;
            class_243 backgroundStart = subject.method_1019((direction = direction.method_1029()).method_1021(0.45));
            boolean bl = openBackground = this.raycast(client, backgroundStart, backgroundEnd = subject.method_1019(direction.method_1021(14.0))).method_17783() == class_239.class_240.field_1333;
            if (openBackground) {
                score += 3.5;
            }
            if (sky = client.field_1687.method_8311(subjectBlock = class_2338.method_49638((class_2374)subject))) {
                score += 1.5;
            }
            if (sky && openBackground && (profile.sceneTime() == SceneTime.SUNRISE || profile.sceneTime() == SceneTime.SUNSET)) {
                score += 3.0;
            }
            for (int depth = 0; depth <= 3; ++depth) {
                if (client.field_1687.method_8316(subjectBlock.method_10087(depth)).method_15769()) continue;
                score += profile.sceneTime() == SceneTime.DAY ? 1.0 : 2.0;
                continue block0;
            }
        }
        return score;
    }

    public class_243 resolveRuntimeCamera(class_310 client, class_243 camera, class_243 focus) {
        for (double lift = 0.0; lift <= 2.4; lift += 0.15) {
            class_243 candidate = camera.method_1031(0.0, lift, 0.0);
            if (!this.isUsable(client, candidate, focus)) continue;
            return candidate;
        }
        return null;
    }

    public Optional<CameraPath> findNavigablePath(class_310 client, class_243 requestedStart, class_243 requestedEnd) {
        class_2338 start = this.nearestRoutable(client, class_2338.method_49638((class_2374)requestedStart));
        class_2338 goal = this.nearestRoutable(client, class_2338.method_49638((class_2374)requestedEnd));
        if (start == null || goal == null) {
            return Optional.empty();
        }
        int margin = 6;
        int minimumX = Math.min(start.method_10263(), goal.method_10263()) - margin;
        int maximumX = Math.max(start.method_10263(), goal.method_10263()) + margin;
        int minimumY = Math.min(start.method_10264(), goal.method_10264()) - 4;
        int maximumY = Math.max(start.method_10264(), goal.method_10264()) + 5;
        int minimumZ = Math.min(start.method_10260(), goal.method_10260()) - margin;
        int maximumZ = Math.max(start.method_10260(), goal.method_10260()) + margin;
        PriorityQueue<RouteNode> open = new PriorityQueue<RouteNode>(Comparator.comparingDouble(RouteNode::estimatedTotal));
        HashMap<class_2338, Double> cost = new HashMap<class_2338, Double>();
        HashMap<class_2338, class_2338> previous = new HashMap<class_2338, class_2338>();
        HashSet<class_2338> closed = new HashSet<class_2338>();
        cost.put(start, 0.0);
        open.add(new RouteNode(start, SceneScanner.heuristic(start, goal)));
        int maximumVisited = switch (CinecraftConfig.INSTANCE.quality()) {
            default -> throw new MatchException(null, null);
            case QualityPreset.PERFORMANCE -> 1500;
            case QualityPreset.BALANCED -> 3000;
            case QualityPreset.CINEMATIC -> 5500;
        };
        int visited = 0;
        while (!open.isEmpty() && visited++ < maximumVisited) {
            class_2338 current = ((RouteNode)open.remove()).position();
            if (!closed.add(current)) continue;
            if (current.equals((Object)goal)) {
                List<class_243> controls = this.simplifyRoute(client, this.reconstruct(previous, current));
                if (controls.size() < 2) {
                    return Optional.empty();
                }
                controls.set(0, requestedStart);
                controls.set(controls.size() - 1, requestedEnd);
                return Optional.of(new ArcLengthSplinePath(controls));
            }
            for (int[] step : ROUTE_STEPS) {
                class_2338 next = current.method_10069(step[0], step[1], step[2]);
                if (next.method_10263() < minimumX || next.method_10263() > maximumX || next.method_10264() < minimumY || next.method_10264() > maximumY || next.method_10260() < minimumZ || next.method_10260() > maximumZ || closed.contains(next) || !this.isCameraVolumeClear(client, SceneScanner.routePoint(next))) continue;
                double stepCost = step[1] == 0 ? (step[0] != 0 && step[2] != 0 ? 1.42 : 1.0) : 1.35;
                double tentative = (Double)cost.get(current) + stepCost;
                if (tentative >= cost.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;
                cost.put(next, tentative);
                previous.put(next, current);
                open.add(new RouteNode(next, tentative + SceneScanner.heuristic(next, goal)));
            }
        }
        return Optional.empty();
    }

    public CameraPath terrainFollowingPath(class_310 client, CameraPath original, double desiredClearance) {
        class_243 point;
        int index;
        ArrayList<class_243> points = new ArrayList<class_243>();
        double[] requiredY = new double[25];
        for (index = 0; index < requiredY.length; ++index) {
            point = original.sample((double)index / (double)(requiredY.length - 1));
            double surface = this.surfaceY(client, point);
            double terrainY = Double.isFinite(surface) ? surface + desiredClearance : point.field_1351;
            requiredY[index] = SceneScanner.clamp(terrainY, point.field_1351 - 4.0, point.field_1351 + 7.0);
        }
        for (int pass = 0; pass < 3; ++pass) {
            double[] smoothed = (double[])requiredY.clone();
            for (int index2 = 1; index2 < requiredY.length - 1; ++index2) {
                smoothed[index2] = requiredY[index2 - 1] * 0.25 + requiredY[index2] * 0.5 + requiredY[index2 + 1] * 0.25;
            }
            requiredY = smoothed;
        }
        for (index = 0; index < requiredY.length; ++index) {
            point = original.sample((double)index / (double)(requiredY.length - 1));
            class_243 adjusted = new class_243(point.field_1352, requiredY[index], point.field_1350);
            for (double lift = 0.0; lift <= 3.0 && !this.isCameraVolumeClear(client, adjusted); lift += 0.2) {
                adjusted = new class_243(point.field_1352, requiredY[index] + lift, point.field_1350);
            }
            points.add(adjusted);
        }
        return new ArcLengthSplinePath(points);
    }

    private class_2338 nearestRoutable(class_310 client, class_2338 origin) {
        for (int radius = 0; radius <= 2; ++radius) {
            for (int dy = -1; dy <= 2; ++dy) {
                for (int dx = -radius; dx <= radius; ++dx) {
                    for (int dz = -radius; dz <= radius; ++dz) {
                        class_2338 candidate = origin.method_10069(dx, dy, dz);
                        if (!this.isCameraVolumeClear(client, SceneScanner.routePoint(candidate))) continue;
                        return candidate;
                    }
                }
            }
        }
        return null;
    }

    private List<class_243> reconstruct(Map<class_2338, class_2338> previous, class_2338 end) {
        ArrayList<class_243> reversed = new ArrayList<class_243>();
        class_2338 current = end;
        reversed.add(SceneScanner.routePoint(current));
        while (previous.containsKey(current)) {
            current = previous.get(current);
            reversed.add(SceneScanner.routePoint(current));
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private List<class_243> simplifyRoute(class_310 client, List<class_243> points) {
        if (points.size() <= 2) {
            return new ArrayList<class_243>(points);
        }
        ArrayList<class_243> simplified = new ArrayList<class_243>();
        int anchor = 0;
        simplified.add(points.getFirst());
        while (anchor < points.size() - 1) {
            int farthest = anchor + 1;
            int candidate = anchor + 2;
            while (candidate < points.size() && this.volumeLineClear(client, points.get(anchor), points.get(candidate))) {
                farthest = candidate++;
            }
            simplified.add(points.get(farthest));
            anchor = farthest;
        }
        return simplified;
    }

    private boolean volumeLineClear(class_310 client, class_243 start, class_243 end) {
        int samples = Math.max(2, (int)Math.ceil(start.method_1022(end) / 0.35));
        for (int index = 0; index <= samples; ++index) {
            if (this.isCameraVolumeClear(client, start.method_35590(end, (double)index / (double)samples))) continue;
            return false;
        }
        return true;
    }

    private double surfaceY(class_310 client, class_243 point) {
        int x = (int)Math.floor(point.field_1352);
        int z = (int)Math.floor(point.field_1350);
        int top = (int)Math.floor(point.field_1351) + 12;
        int bottom = (int)Math.floor(point.field_1351) - 24;
        for (int y = top; y >= bottom; --y) {
            class_2338 pos = new class_2338(x, y, z);
            if (client.field_1687.method_8320(pos).method_26220((class_1922)client.field_1687, pos).method_1110()) continue;
            return (double)y + 1.0;
        }
        return Double.NaN;
    }

    private boolean isCameraVolumeClear(class_310 client, class_243 camera) {
        class_243[] offsets;
        for (class_243 offset : offsets = new class_243[]{class_243.field_1353, new class_243(0.24, 0.0, 0.0), new class_243(-0.24, 0.0, 0.0), new class_243(0.0, 0.0, 0.24), new class_243(0.0, 0.0, -0.24), new class_243(0.0, 0.18, 0.0), new class_243(0.0, -0.18, 0.0)}) {
            class_2338 pos = class_2338.method_49638((class_2374)camera.method_1019(offset));
            if (client.field_1687.method_8320(pos).method_26220((class_1922)client.field_1687, pos).method_1110()) continue;
            return false;
        }
        class_239 floor = this.raycast(client, camera, camera.method_1031(0.0, -0.85, 0.0));
        return floor.method_17783() == class_239.class_240.field_1333;
    }

    private static class_243 routePoint(class_2338 pos) {
        return new class_243((double)pos.method_10263() + 0.5, (double)pos.method_10264() + 0.9, (double)pos.method_10260() + 0.5);
    }

    private static double heuristic(class_2338 first, class_2338 second) {
        double dx = first.method_10263() - second.method_10263();
        double dy = (double)(first.method_10264() - second.method_10264()) * 1.25;
        double dz = first.method_10260() - second.method_10260();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void collectEntities(class_310 client, List<WeightedSubject> candidates) {
        class_746 player = client.field_1724;
        List entities = client.field_1687.method_8333((class_1297)player, player.method_5829().method_1014(24.0), entity -> entity.method_5805() && !entity.method_7325());
        for (class_1297 entity2 : entities) {
            double distance = entity2.method_5739((class_1297)player);
            double movement = Math.min(18.0, entity2.method_18798().method_1027() * 120.0);
            double livingBonus = entity2 instanceof class_1309 ? 18.0 : 0.0;
            double score = 58.0 + livingBonus + movement - distance * 1.1;
            class_243 target = entity2.method_73189().method_1031(0.0, (double)entity2.method_17682() * 0.55, 0.0);
            candidates.add(new WeightedSubject(new SceneSubject(SubjectType.ENTITY, target, "entity:" + String.valueOf(entity2.method_5667()), entity2), score));
        }
    }

    private void collectGroups(class_310 client, List<WeightedSubject> candidates) {
        class_746 player = client.field_1724;
        ArrayList<class_1297> entities = new ArrayList<class_1297>(client.field_1687.method_8333((class_1297)player, player.method_5829().method_1014(24.0), entity -> entity.method_5805() && !entity.method_7325()));
        entities.sort(Comparator.comparingDouble(arg_0 -> SceneScanner.lambda$collectGroups$10((class_1657)player, arg_0)));
        int limit = Math.min(10, entities.size());
        int added = 0;
        for (int firstIndex = 0; firstIndex < limit && added < 12; ++firstIndex) {
            class_1297 first = (class_1297)entities.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < limit && added < 12; ++secondIndex) {
                class_1297 second = (class_1297)entities.get(secondIndex);
                double separation = first.method_5739(second);
                if (separation < 1.2 || separation > 10.0) continue;
                double playerDistance = (double)(first.method_5739((class_1297)player) + second.method_5739((class_1297)player)) * 0.5;
                double motion = (first.method_18798().method_37268() + second.method_18798().method_37268()) * 65.0;
                double score = 88.0 + Math.min(14.0, motion) - separation * 1.7 - playerDistance * 0.65;
                candidates.add(new WeightedSubject(SceneSubject.entityGroup(first, second), score));
                ++added;
            }
        }
        if (added > 0) {
            return;
        }
        ArrayList<WeightedSubject> features = new ArrayList<WeightedSubject>();
        this.collectFeatures(client, features);
        features.stream().filter(candidate -> candidate.subject().type() == SubjectType.FEATURE).max(Comparator.comparingDouble(WeightedSubject::score)).ifPresent(arg_0 -> SceneScanner.lambda$collectGroups$12(candidates, (class_1657)player, arg_0));
    }

    private void collectFeatures(class_310 client, List<WeightedSubject> candidates) {
        class_746 player = client.field_1724;
        int centerX = player.method_31477();
        int centerY = player.method_31478();
        int centerZ = player.method_31479();
        for (int x = centerX - 10; x <= centerX + 10; ++x) {
            for (int y = centerY - 5; y <= centerY + 6; ++y) {
                for (int z = centerZ - 10; z <= centerZ + 10; ++z) {
                    double distance;
                    class_2338 pos = new class_2338(x, y, z);
                    class_2680 state = client.field_1687.method_8320(pos);
                    if (!state.method_31709() && state.method_26213() < 8 || (distance = class_243.method_24953((class_2382)pos).method_1022(player.method_73189())) < 2.5) continue;
                    double score = (state.method_31709() ? 72.0 : 58.0) + (double)state.method_26213() - distance;
                    class_243 target = class_243.method_24953((class_2382)pos).method_1031(0.0, 0.65, 0.0);
                    candidates.add(new WeightedSubject(new SceneSubject(SubjectType.FEATURE, target, "feature:" + pos.method_10063()), score));
                }
            }
        }
    }

    private void collectLandscapes(class_310 client, List<WeightedSubject> candidates) {
        class_746 player = client.field_1724;
        int playerY = player.method_31478();
        int[] distances = new int[]{10, 18, 26};
        for (int direction = 0; direction < 12; ++direction) {
            double angle = Math.PI * 2 * (double)direction / 12.0;
            for (int distance : distances) {
                int z;
                int x = (int)Math.floor(player.method_23317() + Math.cos(angle) * (double)distance);
                SurfacePoint surface = this.findTopSurface(client, x, z = (int)Math.floor(player.method_23321() + Math.sin(angle) * (double)distance), playerY);
                if (surface == null || surface.water()) continue;
                double heightInterest = Math.min(18.0, (double)Math.abs(surface.position().method_10264() - playerY) * 1.6);
                double score = 38.0 + (double)distance * 0.55 + heightInterest;
                class_243 target = surface.focus();
                candidates.add(new WeightedSubject(new SceneSubject(SubjectType.LANDSCAPE, target, "landscape:" + direction + ":" + distance), score));
            }
        }
    }

    private SurfaceSurvey surveySurface(class_310 client, class_243 playerFocus) {
        int playerY = client.field_1724.method_31478();
        ArrayList<SurfacePoint> land = new ArrayList<SurfacePoint>();
        int water = 0;
        int sampled = 0;
        for (int dx = -40; dx <= 40; dx += 8) {
            for (int dz = -40; dz <= 40; dz += 8) {
                int z;
                int x = (int)Math.floor(playerFocus.field_1352) + dx;
                SurfacePoint point2 = this.findTopSurface(client, x, z = (int)Math.floor(playerFocus.field_1350) + dz, playerY);
                if (point2 == null) continue;
                ++sampled;
                if (point2.water()) {
                    ++water;
                    continue;
                }
                land.add(point2);
            }
        }
        if (land.isEmpty()) {
            return new SurfaceSurvey(playerFocus, List.of(playerFocus), 8.0, 0.0, sampled == 0 ? 0.0 : (double)water / (double)sampled);
        }
        double centerX = land.stream().mapToDouble(point -> point.focus().field_1352).average().orElse(playerFocus.field_1352);
        double centerZ = land.stream().mapToDouble(point -> point.focus().field_1350).average().orElse(playerFocus.field_1350);
        class_243 approximateCenter = new class_243(centerX, playerFocus.field_1351, centerZ);
        class_243 center = land.stream().min(Comparator.comparingDouble(point -> SceneScanner.horizontalDistance(point.focus(), approximateCenter))).map(SurfacePoint::focus).orElse(playerFocus);
        double centerY = center.field_1351;
        double minimumY = land.stream().mapToDouble(point -> point.focus().field_1351).min().orElse(centerY);
        double maximumY = land.stream().mapToDouble(point -> point.focus().field_1351).max().orElse(centerY);
        double radius = land.stream().mapToDouble(point -> SceneScanner.horizontalDistance(point.focus(), center)).max().orElse(8.0);
        ArrayList<class_243> anchors = new ArrayList<class_243>();
        anchors.add(center);
        anchors.add(playerFocus);
        land.stream().sorted(Comparator.comparingDouble(point -> SceneScanner.horizontalDistance(point.focus(), center)).reversed()).limit(8L).map(SurfacePoint::focus).forEach(anchors::add);
        land.stream().max(Comparator.comparingInt(point -> point.position().method_10264())).map(SurfacePoint::focus).ifPresent(anchors::add);
        double coverage = sampled == 0 ? 0.0 : (double)water / (double)sampled;
        return new SurfaceSurvey(center, anchors, SceneScanner.clamp(radius, 8.0, 56.0), maximumY - minimumY, coverage);
    }

    private SurfacePoint findTopSurface(class_310 client, int x, int z, int playerY) {
        for (int y = playerY + 32; y >= playerY - 24; --y) {
            boolean water;
            class_2338 pos = new class_2338(x, y, z);
            class_2680 state = client.field_1687.method_8320(pos);
            if (state.method_26215()) continue;
            boolean bl = water = !state.method_26227().method_15769();
            if (!water && state.method_26220((class_1922)client.field_1687, pos).method_1110()) continue;
            double focusHeight = water ? 0.35 : 1.15;
            return new SurfacePoint(pos, class_243.method_24953((class_2382)pos).method_1031(0.0, focusHeight, 0.0), water);
        }
        return null;
    }

    private double measureSkyVisibility(class_310 client, class_243 playerFocus) {
        class_243[] offsets = new class_243[]{class_243.field_1353, new class_243(2.5, 0.0, 0.0), new class_243(-2.5, 0.0, 0.0), new class_243(0.0, 0.0, 2.5), new class_243(0.0, 0.0, -2.5), new class_243(2.5, 0.0, 2.5), new class_243(-2.5, 0.0, 2.5), new class_243(2.5, 0.0, -2.5), new class_243(-2.5, 0.0, -2.5)};
        int visible = 0;
        for (class_243 offset : offsets) {
            class_243 start = playerFocus.method_1019(offset);
            class_239 hit = this.raycast(client, start, start.method_1031(0.0, 48.0, 0.0));
            if (hit.method_17783() != class_239.class_240.field_1333) continue;
            ++visible;
        }
        return (double)visible / (double)offsets.length;
    }

    private double rayDistance(class_310 client, class_243 start, class_243 end, double missDistance) {
        class_239 hit = this.raycast(client, start, end);
        return hit.method_17783() == class_239.class_240.field_1333 ? missDistance : start.method_1022(hit.method_17784());
    }

    private class_239 raycast(class_310 client, class_243 start, class_243 end) {
        return client.field_1687.method_17742(new class_3959(start, end, class_3959.class_3960.field_17558, class_3959.class_242.field_1348, (class_1297)client.field_1724));
    }

    private void remember(String key) {
        this.recentSubjects.remove(key);
        this.recentSubjects.addFirst(key);
        while (this.recentSubjects.size() > 8) {
            this.recentSubjects.removeLast();
        }
    }

    private boolean isUsable(class_310 client, class_243 camera, class_243 target) {
        class_243[] clearanceOffsets;
        for (class_243 offset : clearanceOffsets = new class_243[]{class_243.field_1353, new class_243(0.24, 0.0, 0.0), new class_243(-0.24, 0.0, 0.0), new class_243(0.0, 0.0, 0.24), new class_243(0.0, 0.0, -0.24), new class_243(0.0, 0.18, 0.0), new class_243(0.0, -0.18, 0.0)}) {
            class_2338 pos = class_2338.method_49638((class_2374)camera.method_1019(offset));
            if (client.field_1687.method_8320(pos).method_26220((class_1922)client.field_1687, pos).method_1110()) continue;
            return false;
        }
        class_239 floor = this.raycast(client, camera, camera.method_1031(0.0, -0.85, 0.0));
        if (floor.method_17783() != class_239.class_240.field_1333) {
            return false;
        }
        return this.raycast(client, camera, target).method_17783() == class_239.class_240.field_1333;
    }

    private static double horizontalDistance(class_243 first, class_243 second) {
        double dx = first.field_1352 - second.field_1352;
        double dz = first.field_1350 - second.field_1350;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static /* synthetic */ void lambda$collectGroups$12(List candidates, class_1657 player, WeightedSubject feature) {
        candidates.add(new WeightedSubject(SceneSubject.playerAndFeature((class_1297)player, feature.subject().target(), "player_feature:" + feature.subject().key()), feature.score() + 8.0));
    }

    private static /* synthetic */ double lambda$collectGroups$10(class_1657 player, class_1297 entity) {
        return entity.method_5858((class_1297)player);
    }

    @Environment(value=EnvType.CLIENT)
    private record SurfaceSurvey(class_243 center, List<class_243> anchors, double radius, double relief, double waterCoverage) {
    }

    @Environment(value=EnvType.CLIENT)
    private record WeightedSubject(SceneSubject subject, double score) {
    }

    @Environment(value=EnvType.CLIENT)
    private record RouteNode(class_2338 position, double estimatedTotal) {
    }

    @Environment(value=EnvType.CLIENT)
    private record SurfacePoint(class_2338 position, class_243 focus, boolean water) {
    }
}


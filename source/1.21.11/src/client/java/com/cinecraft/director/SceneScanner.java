package com.cinecraft.director;

import com.cinecraft.camera.CameraPath;
import com.cinecraft.camera.CameraPose;
import com.cinecraft.camera.LookAt;
import com.cinecraft.camera.ArcLengthSplinePath;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;

/** Surveys the scene and provides collision and visibility tests to the procedural planner. */
public final class SceneScanner {
    private static final int RECENT_SUBJECT_LIMIT = 8;
    private static final double SURVEY_RANGE = 48.0;
    private static final double MINIMUM_CAMERA_GROUND_CLEARANCE = 0.85;
    private static final double MAXIMUM_RUNTIME_CAMERA_LIFT = 2.4;

    private final ArrayDeque<String> recentSubjects = new ArrayDeque<>();
    private final ArrayDeque<SubjectType> environmentDeck = new ArrayDeque<>();
    private final Random random = new Random();

    public SceneSubject playerSubject(MinecraftClient client) {
        PlayerEntity player = client.player;
        return new SceneSubject(
                SubjectType.PLAYER,
                player.getEntityPos().add(0.0, player.getHeight() * 0.55, 0.0),
                "player",
                player
        );
    }

    public SceneSubject landscapeSubject(EnvironmentProfile profile) {
        Vec3d center = profile.landscapeCenter();
        String key = "wide:" + Math.round(center.x / 8.0) + ":" + Math.round(center.z / 8.0);
        return new SceneSubject(SubjectType.LANDSCAPE, center, key);
    }

    /** Selects armor or an occupied hand as a live character-detail target. */
    public SceneSubject playerDetailSubject(MinecraftClient client) {
        PlayerEntity player = client.player;
        List<SubjectFocus> choices = new ArrayList<>();
        if (!player.getMainHandStack().isEmpty()) choices.add(SubjectFocus.MAIN_HAND);
        if (!player.getOffHandStack().isEmpty()) choices.add(SubjectFocus.OFF_HAND);
        boolean hasArmor = false;
        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };
        for (EquipmentSlot slot : armorSlots) {
            if (!player.getEquippedStack(slot).isEmpty()) {
                hasArmor = true;
                break;
            }
        }
        if (hasArmor) {
            choices.add(SubjectFocus.HEAD);
            choices.add(SubjectFocus.CHEST);
        }
        if (choices.isEmpty()) {
            choices.add(SubjectFocus.HEAD);
            choices.add(SubjectFocus.CHEST);
        }
        SubjectFocus focus = choices.get(random.nextInt(choices.size()));
        return new SceneSubject(
                SubjectType.PLAYER_DETAIL,
                SceneSubject.targetFor(player, focus),
                "player_detail:" + focus,
                player,
                focus
        );
    }

    /** Measures enclosure, visibility, terrain extent, relief, and surrounding water. */
    public EnvironmentProfile survey(MinecraftClient client) {
        Vec3d playerFocus = playerSubject(client).target();
        double clearanceTotal = 0.0;
        for (int direction = 0; direction < 16; direction++) {
            double angle = Math.PI * 2.0 * direction / 16.0;
            Vec3d end = playerFocus.add(Math.cos(angle) * SURVEY_RANGE, 0.0, Math.sin(angle) * SURVEY_RANGE);
            clearanceTotal += rayDistance(client, playerFocus, end, SURVEY_RANGE);
        }
        double averageClearance = clearanceTotal / 16.0;
        double skyVisibility = measureSkyVisibility(client, playerFocus);
        SurfaceSurvey surface = surveySurface(client, playerFocus);
        BlockPos playerBlock = client.player.getBlockPos();
        boolean directlySkyVisible = client.world.isSkyVisible(playerBlock);
        double surfaceDepth = Math.max(0.0,
                client.world.getTopY(Heightmap.Type.MOTION_BLOCKING, playerBlock.getX(), playerBlock.getZ())
                        - playerBlock.getY());
        boolean underground = !directlySkyVisible && skyVisibility < 0.34 && surfaceDepth > 8.0;

        SpaceType spaceType;
        if (underground) {
            spaceType = SpaceType.ENCLOSED;
        } else if (skyVisibility < 0.58 || averageClearance < 14.0) {
            spaceType = SpaceType.TRANSITIONAL;
        } else if (skyVisibility > 0.86 && averageClearance > 30.0) {
            spaceType = SpaceType.VAST;
        } else {
            spaceType = SpaceType.OPEN;
        }

        int viewDistanceChunks = client.options.getViewDistance().getValue();
        double renderBound = clamp(viewDistanceChunks * 16.0 - 16.0, 16.0, 80.0);
        double spatialBound = switch (spaceType) {
            case ENCLOSED -> 6.5;
            case TRANSITIONAL -> 32.0;
            case OPEN -> 48.0;
            case VAST -> 72.0;
        };
        double horizontalBound = Math.min(renderBound, spatialBound);
        double verticalBound = underground ? 5.5 : clamp(renderBound * 0.95, 32.0, 72.0);
        DimensionMood dimensionMood = dimensionMood(client);
        BiomeMood biomeMood = biomeMood(client, playerBlock, underground, dimensionMood);

        return new EnvironmentProfile(
                spaceType,
                underground,
                surfaceDepth,
                playerFocus,
                surface.center(),
                surface.anchors(),
                averageClearance,
                skyVisibility,
                surface.relief(),
                surface.waterCoverage(),
                surface.radius(),
                horizontalBound,
                verticalBound,
                biomeMood,
                weather(client, playerBlock),
                sceneTime(client),
                dimensionMood
        );
    }

    private static DimensionMood dimensionMood(MinecraftClient client) {
        if (client.world.getRegistryKey().equals(World.NETHER)) return DimensionMood.NETHER;
        if (client.world.getRegistryKey().equals(World.END)) return DimensionMood.END;
        if (client.world.getRegistryKey().equals(World.OVERWORLD)) return DimensionMood.OVERWORLD;
        return DimensionMood.OTHER;
    }

    private static BiomeMood biomeMood(
            MinecraftClient client,
            BlockPos playerBlock,
            boolean underground,
            DimensionMood dimension
    ) {
        if (dimension == DimensionMood.NETHER) return BiomeMood.NETHER;
        if (dimension == DimensionMood.END) return BiomeMood.END;
        if (underground) return BiomeMood.CAVE;
        String path = client.world.getBiome(playerBlock)
                .getKey()
                .map(key -> key.getValue().getPath())
                .orElse("");
        if (containsAny(path, "snow", "frozen", "ice", "grove")) return BiomeMood.SNOW;
        if (containsAny(path, "ocean", "beach", "river")) return BiomeMood.OCEAN;
        if (containsAny(path, "mountain", "peak", "slope", "windswept", "cliff")) return BiomeMood.MOUNTAIN;
        if (containsAny(path, "forest", "taiga", "jungle", "woods", "cherry")) return BiomeMood.FOREST;
        if (containsAny(path, "desert", "badlands", "savanna")) return BiomeMood.DESERT;
        if (containsAny(path, "swamp", "mangrove")) return BiomeMood.SWAMP;
        if (containsAny(path, "plains", "meadow", "field")) return BiomeMood.PLAINS;
        return BiomeMood.OTHER;
    }

    private static SceneWeather weather(MinecraftClient client, BlockPos playerBlock) {
        if (client.world.isThundering()) return SceneWeather.THUNDER;
        if (!client.world.isRaining()) return SceneWeather.CLEAR;
        return client.world.getPrecipitation(playerBlock) == Biome.Precipitation.SNOW
                ? SceneWeather.SNOW
                : SceneWeather.RAIN;
    }

    private static SceneTime sceneTime(MinecraftClient client) {
        long time = Math.floorMod(client.world.getTimeOfDay(), 24_000L);
        if (time >= 22_500L || time < 1_000L) return SceneTime.SUNRISE;
        if (time >= 11_500L && time < 13_500L) return SceneTime.SUNSET;
        if (time >= 13_500L && time < 22_500L) return SceneTime.NIGHT;
        return SceneTime.DAY;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    /** Selects a non-recent nearby entity, detail, or landscape point. */
    public SceneSubject findEnvironmentSubject(MinecraftClient client) {
        List<WeightedSubject> candidates = new ArrayList<>();
        collectEntities(client, candidates);
        collectGroups(client, candidates);
        collectFeatures(client, candidates);
        collectLandscapes(client, candidates);
        candidates.sort(Comparator.comparingDouble(WeightedSubject::score).reversed());

        List<WeightedSubject> pool = chooseEnvironmentPool(candidates);
        if (pool.isEmpty()) return playerSubject(client);

        SceneSubject selected = pool.get(random.nextInt(pool.size())).subject();
        remember(selected.key());
        return selected;
    }

    /** Requests a particular coverage category, with ordinary environment selection as fallback. */
    public SceneSubject findSubject(MinecraftClient client, SubjectType desired) {
        if (desired == SubjectType.PLAYER) return playerSubject(client);
        List<WeightedSubject> candidates = new ArrayList<>();
        collectEntities(client, candidates);
        collectGroups(client, candidates);
        collectFeatures(client, candidates);
        collectLandscapes(client, candidates);
        candidates.sort(Comparator.comparingDouble(WeightedSubject::score).reversed());

        List<WeightedSubject> pool = candidates.stream()
                .filter(candidate -> candidate.subject().type() == desired)
                .filter(candidate -> !recentSubjects.contains(candidate.subject().key()))
                .limit(5)
                .toList();
        if (pool.isEmpty()) {
            pool = candidates.stream()
                    .filter(candidate -> candidate.subject().type() == desired)
                    .limit(5)
                    .toList();
        }
        if (pool.isEmpty()) return findEnvironmentSubject(client);

        SceneSubject selected = pool.get(random.nextInt(pool.size())).subject();
        remember(selected.key());
        return selected;
    }

    public void resetSubjects() {
        recentSubjects.clear();
        environmentDeck.clear();
    }

    /** Rotates through entities, interesting blocks, and landscapes when each is available. */
    private List<WeightedSubject> chooseEnvironmentPool(List<WeightedSubject> candidates) {
        for (int attempt = 0; attempt < 4; attempt++) {
            if (environmentDeck.isEmpty()) refillEnvironmentDeck();
            SubjectType desired = environmentDeck.removeFirst();
            List<WeightedSubject> freshOfType = candidates.stream()
                    .filter(candidate -> candidate.subject().type() == desired)
                    .filter(candidate -> !recentSubjects.contains(candidate.subject().key()))
                    .limit(5)
                    .toList();
            if (!freshOfType.isEmpty()) return freshOfType;

            List<WeightedSubject> anyOfType = candidates.stream()
                    .filter(candidate -> candidate.subject().type() == desired)
                    .limit(5)
                    .toList();
            if (!anyOfType.isEmpty()) return anyOfType;
        }
        return candidates.stream()
                .filter(candidate -> !recentSubjects.contains(candidate.subject().key()))
                .limit(5)
                .toList();
    }

    private void refillEnvironmentDeck() {
        List<SubjectType> types = new ArrayList<>(List.of(
                SubjectType.ENTITY,
                SubjectType.GROUP,
                SubjectType.FEATURE,
                SubjectType.LANDSCAPE
        ));
        Collections.shuffle(types, random);
        environmentDeck.addAll(types);
    }

    public CameraPose findPlayerView(MinecraftClient client) {
        Vec3d target = playerSubject(client).target();
        double baseAngle = Math.toRadians(client.player.getYaw() + 180.0);
        double[] radii = {1.15, 1.7, 2.5, 3.5, 4.5, 6.5};
        for (double radius : radii) {
            for (int index = 0; index < 12; index++) {
                double angle = baseAngle + Math.PI * 2.0 * index / 12.0;
                Vec3d camera = new Vec3d(
                        target.x + Math.cos(angle) * radius,
                        target.y + Math.min(1.3, 0.3 + radius * 0.22),
                        target.z + Math.sin(angle) * radius
                );
                if (isUsable(client, camera, target)) return LookAt.pose(camera, target);
            }
        }
        return null;
    }

    /** Rejects a generated spline if any sampled frame is unsafe or loses its moving focus. */
    public boolean isPathUsable(
            MinecraftClient client,
            CameraPath cameraPath,
            CameraPath focusPath,
            CameraPath subjectPath,
            EnvironmentProfile profile
    ) {
        Vec3d playerPosition = client.player.getEntityPos();
        for (int sample = 0; sample <= 24; sample++) {
            double progress = sample / 24.0;
            Vec3d camera = cameraPath.sample(progress);
            Vec3d focus = focusPath.sample(progress);
            Vec3d subject = subjectPath.sample(progress);
            double dx = camera.x - playerPosition.x;
            double dz = camera.z - playerPosition.z;
            if (dx * dx + dz * dz > profile.maxCameraDistance() * profile.maxCameraDistance()) return false;
            if (Math.abs(camera.y - playerPosition.y) > profile.maxVerticalRise()) return false;
            if (!isUsable(client, camera, focus)) return false;
            if (raycast(client, camera, subject).getType() != HitResult.Type.MISS) return false;
        }
        return true;
    }

    /** Scores open backgrounds, sky silhouettes, and water-backed compositions. */
    public double compositionScore(
            MinecraftClient client,
            ShotPlan plan,
            EnvironmentProfile profile
    ) {
        double score = 0.0;
        for (double progress : new double[]{0.15, 0.50, 0.85}) {
            Vec3d camera = plan.path().sample(progress);
            Vec3d subject = plan.subjectPath().sample(progress);
            Vec3d direction = subject.subtract(camera);
            if (direction.lengthSquared() < 0.001) continue;
            direction = direction.normalize();
            Vec3d backgroundStart = subject.add(direction.multiply(0.45));
            Vec3d backgroundEnd = subject.add(direction.multiply(14.0));
            boolean openBackground = raycast(client, backgroundStart, backgroundEnd).getType() == HitResult.Type.MISS;
            if (openBackground) score += 3.5;

            BlockPos subjectBlock = BlockPos.ofFloored(subject);
            boolean sky = client.world.isSkyVisible(subjectBlock);
            if (sky) score += 1.5;
            if (sky && openBackground
                    && (profile.sceneTime() == SceneTime.SUNRISE || profile.sceneTime() == SceneTime.SUNSET)) {
                score += 3.0;
            }
            for (int depth = 0; depth <= 3; depth++) {
                if (!client.world.getFluidState(subjectBlock.down(depth)).isEmpty()) {
                    score += profile.sceneTime() == SceneTime.DAY ? 1.0 : 2.0;
                    break;
                }
            }
        }
        return score;
    }

    /**
     * Keeps a tracked rig clear of terrain after its subject changes elevation.
     * A small upward correction is preferred; null asks the shot to retain its
     * previous safe frame when neither clearance nor line of sight can be kept.
     */
    public Vec3d resolveRuntimeCamera(MinecraftClient client, Vec3d camera, Vec3d focus) {
        for (double lift = 0.0; lift <= MAXIMUM_RUNTIME_CAMERA_LIFT; lift += 0.15) {
            Vec3d candidate = camera.add(0.0, lift, 0.0);
            if (isUsable(client, candidate, focus)) return candidate;
        }
        return null;
    }

    /** Finds a collision-clear 3D corridor between two intended rail endpoints. */
    public java.util.Optional<CameraPath> findNavigablePath(
            MinecraftClient client,
            Vec3d requestedStart,
            Vec3d requestedEnd
    ) {
        BlockPos start = nearestRoutable(client, BlockPos.ofFloored(requestedStart));
        BlockPos goal = nearestRoutable(client, BlockPos.ofFloored(requestedEnd));
        if (start == null || goal == null) return java.util.Optional.empty();

        int margin = 6;
        int minimumX = Math.min(start.getX(), goal.getX()) - margin;
        int maximumX = Math.max(start.getX(), goal.getX()) + margin;
        int minimumY = Math.min(start.getY(), goal.getY()) - 4;
        int maximumY = Math.max(start.getY(), goal.getY()) + 5;
        int minimumZ = Math.min(start.getZ(), goal.getZ()) - margin;
        int maximumZ = Math.max(start.getZ(), goal.getZ()) + margin;

        PriorityQueue<RouteNode> open = new PriorityQueue<>(Comparator.comparingDouble(RouteNode::estimatedTotal));
        Map<BlockPos, Double> cost = new HashMap<>();
        Map<BlockPos, BlockPos> previous = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();
        cost.put(start, 0.0);
        open.add(new RouteNode(start, heuristic(start, goal)));
        int maximumVisited = switch (com.cinecraft.config.CinecraftConfig.INSTANCE.quality()) {
            case PERFORMANCE -> 1_500;
            case BALANCED -> 3_000;
            case CINEMATIC -> 5_500;
        };

        int visited = 0;
        while (!open.isEmpty() && visited++ < maximumVisited) {
            BlockPos current = open.remove().position();
            if (!closed.add(current)) continue;
            if (current.equals(goal)) {
                List<Vec3d> controls = simplifyRoute(client, reconstruct(previous, current));
                if (controls.size() < 2) return java.util.Optional.empty();
                controls.set(0, requestedStart);
                controls.set(controls.size() - 1, requestedEnd);
                return java.util.Optional.of(new ArcLengthSplinePath(controls));
            }

            for (int[] step : ROUTE_STEPS) {
                BlockPos next = current.add(step[0], step[1], step[2]);
                if (next.getX() < minimumX || next.getX() > maximumX
                        || next.getY() < minimumY || next.getY() > maximumY
                        || next.getZ() < minimumZ || next.getZ() > maximumZ
                        || closed.contains(next)
                        || !isCameraVolumeClear(client, routePoint(next))) {
                    continue;
                }
                double stepCost = step[1] == 0
                        ? (step[0] != 0 && step[2] != 0 ? 1.42 : 1.0)
                        : 1.35;
                double tentative = cost.get(current) + stepCost;
                if (tentative >= cost.getOrDefault(next, Double.POSITIVE_INFINITY)) continue;
                cost.put(next, tentative);
                previous.put(next, current);
                open.add(new RouteNode(next, tentative + heuristic(next, goal)));
            }
        }
        return java.util.Optional.empty();
    }

    /** Pre-samples a landscape move and softly follows the measured terrain relief. */
    public CameraPath terrainFollowingPath(MinecraftClient client, CameraPath original, double desiredClearance) {
        List<Vec3d> points = new ArrayList<>();
        double[] requiredY = new double[25];
        for (int index = 0; index < requiredY.length; index++) {
            Vec3d point = original.sample(index / (double) (requiredY.length - 1));
            double surface = surfaceY(client, point);
            double terrainY = Double.isFinite(surface) ? surface + desiredClearance : point.y;
            requiredY[index] = clamp(terrainY, point.y - 4.0, point.y + 7.0);
        }
        for (int pass = 0; pass < 3; pass++) {
            double[] smoothed = requiredY.clone();
            for (int index = 1; index < requiredY.length - 1; index++) {
                smoothed[index] = requiredY[index - 1] * 0.25
                        + requiredY[index] * 0.50
                        + requiredY[index + 1] * 0.25;
            }
            requiredY = smoothed;
        }
        for (int index = 0; index < requiredY.length; index++) {
            Vec3d point = original.sample(index / (double) (requiredY.length - 1));
            Vec3d adjusted = new Vec3d(point.x, requiredY[index], point.z);
            for (double lift = 0.0; lift <= 3.0 && !isCameraVolumeClear(client, adjusted); lift += 0.20) {
                adjusted = new Vec3d(point.x, requiredY[index] + lift, point.z);
            }
            points.add(adjusted);
        }
        return new ArcLengthSplinePath(points);
    }

    private static final int[][] ROUTE_STEPS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1},
            {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1},
            {0, 1, 0}, {0, -1, 0}
    };

    private BlockPos nearestRoutable(MinecraftClient client, BlockPos origin) {
        for (int radius = 0; radius <= 2; radius++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        BlockPos candidate = origin.add(dx, dy, dz);
                        if (isCameraVolumeClear(client, routePoint(candidate))) return candidate;
                    }
                }
            }
        }
        return null;
    }

    private List<Vec3d> reconstruct(Map<BlockPos, BlockPos> previous, BlockPos end) {
        List<Vec3d> reversed = new ArrayList<>();
        BlockPos current = end;
        reversed.add(routePoint(current));
        while (previous.containsKey(current)) {
            current = previous.get(current);
            reversed.add(routePoint(current));
        }
        Collections.reverse(reversed);
        return reversed;
    }

    private List<Vec3d> simplifyRoute(MinecraftClient client, List<Vec3d> points) {
        if (points.size() <= 2) return new ArrayList<>(points);
        List<Vec3d> simplified = new ArrayList<>();
        int anchor = 0;
        simplified.add(points.getFirst());
        while (anchor < points.size() - 1) {
            int farthest = anchor + 1;
            for (int candidate = anchor + 2; candidate < points.size(); candidate++) {
                if (!volumeLineClear(client, points.get(anchor), points.get(candidate))) break;
                farthest = candidate;
            }
            simplified.add(points.get(farthest));
            anchor = farthest;
        }
        return simplified;
    }

    private boolean volumeLineClear(MinecraftClient client, Vec3d start, Vec3d end) {
        int samples = Math.max(2, (int) Math.ceil(start.distanceTo(end) / 0.35));
        for (int index = 0; index <= samples; index++) {
            if (!isCameraVolumeClear(client, start.lerp(end, index / (double) samples))) return false;
        }
        return true;
    }

    private double surfaceY(MinecraftClient client, Vec3d point) {
        int x = (int) Math.floor(point.x);
        int z = (int) Math.floor(point.z);
        int top = (int) Math.floor(point.y) + 12;
        int bottom = (int) Math.floor(point.y) - 24;
        for (int y = top; y >= bottom; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!client.world.getBlockState(pos).getCollisionShape(client.world, pos).isEmpty()) return y + 1.0;
        }
        return Double.NaN;
    }

    private boolean isCameraVolumeClear(MinecraftClient client, Vec3d camera) {
        Vec3d[] offsets = {
                Vec3d.ZERO,
                new Vec3d(0.24, 0.0, 0.0), new Vec3d(-0.24, 0.0, 0.0),
                new Vec3d(0.0, 0.0, 0.24), new Vec3d(0.0, 0.0, -0.24),
                new Vec3d(0.0, 0.18, 0.0), new Vec3d(0.0, -0.18, 0.0)
        };
        for (Vec3d offset : offsets) {
            BlockPos pos = BlockPos.ofFloored(camera.add(offset));
            if (!client.world.getBlockState(pos).getCollisionShape(client.world, pos).isEmpty()) return false;
        }
        HitResult floor = raycast(client, camera, camera.add(0.0, -MINIMUM_CAMERA_GROUND_CLEARANCE, 0.0));
        return floor.getType() == HitResult.Type.MISS;
    }

    private static Vec3d routePoint(BlockPos pos) {
        return new Vec3d(pos.getX() + 0.5, pos.getY() + 0.90, pos.getZ() + 0.5);
    }

    private static double heuristic(BlockPos first, BlockPos second) {
        double dx = first.getX() - second.getX();
        double dy = (first.getY() - second.getY()) * 1.25;
        double dz = first.getZ() - second.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private record RouteNode(BlockPos position, double estimatedTotal) { }

    private void collectEntities(MinecraftClient client, List<WeightedSubject> candidates) {
        PlayerEntity player = client.player;
        List<Entity> entities = client.world.getOtherEntities(
                player,
                player.getBoundingBox().expand(24.0),
                entity -> entity.isAlive() && !entity.isSpectator()
        );
        for (Entity entity : entities) {
            double distance = entity.distanceTo(player);
            double movement = Math.min(18.0, entity.getVelocity().lengthSquared() * 120.0);
            double livingBonus = entity instanceof LivingEntity ? 18.0 : 0.0;
            double score = 58.0 + livingBonus + movement - distance * 1.1;
            Vec3d target = entity.getEntityPos().add(0.0, entity.getHeight() * 0.55, 0.0);
            candidates.add(new WeightedSubject(
                    new SceneSubject(SubjectType.ENTITY, target, "entity:" + entity.getUuid(), entity),
                    score
            ));
        }
    }

    private void collectGroups(MinecraftClient client, List<WeightedSubject> candidates) {
        PlayerEntity player = client.player;
        List<Entity> entities = new ArrayList<>(client.world.getOtherEntities(
                player,
                player.getBoundingBox().expand(24.0),
                entity -> entity.isAlive() && !entity.isSpectator()
        ));
        entities.sort(Comparator.comparingDouble(entity -> entity.squaredDistanceTo(player)));
        int limit = Math.min(10, entities.size());
        int added = 0;
        for (int firstIndex = 0; firstIndex < limit && added < 12; firstIndex++) {
            Entity first = entities.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < limit && added < 12; secondIndex++) {
                Entity second = entities.get(secondIndex);
                double separation = first.distanceTo(second);
                if (separation < 1.2 || separation > 10.0) continue;
                double playerDistance = (first.distanceTo(player) + second.distanceTo(player)) * 0.5;
                double motion = (first.getVelocity().horizontalLengthSquared()
                        + second.getVelocity().horizontalLengthSquared()) * 65.0;
                double score = 88.0 + Math.min(14.0, motion) - separation * 1.7 - playerDistance * 0.65;
                candidates.add(new WeightedSubject(SceneSubject.entityGroup(first, second), score));
                added++;
            }
        }

        if (added > 0) return;
        List<WeightedSubject> features = new ArrayList<>();
        collectFeatures(client, features);
        features.stream()
                .filter(candidate -> candidate.subject().type() == SubjectType.FEATURE)
                .max(Comparator.comparingDouble(WeightedSubject::score))
                .ifPresent(feature -> candidates.add(new WeightedSubject(
                        SceneSubject.playerAndFeature(
                                player,
                                feature.subject().target(),
                                "player_feature:" + feature.subject().key()
                        ),
                        feature.score() + 8.0
                )));
    }

    private void collectFeatures(MinecraftClient client, List<WeightedSubject> candidates) {
        PlayerEntity player = client.player;
        int centerX = player.getBlockX();
        int centerY = player.getBlockY();
        int centerZ = player.getBlockZ();

        for (int x = centerX - 10; x <= centerX + 10; x++) {
            for (int y = centerY - 5; y <= centerY + 6; y++) {
                for (int z = centerZ - 10; z <= centerZ + 10; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = client.world.getBlockState(pos);
                    if (!state.hasBlockEntity() && state.getLuminance() < 8) continue;
                    double distance = Vec3d.ofCenter(pos).distanceTo(player.getEntityPos());
                    if (distance < 2.5) continue;
                    double score = (state.hasBlockEntity() ? 72.0 : 58.0) + state.getLuminance() - distance;
                    Vec3d target = Vec3d.ofCenter(pos).add(0.0, 0.65, 0.0);
                    candidates.add(new WeightedSubject(
                            new SceneSubject(SubjectType.FEATURE, target, "feature:" + pos.asLong()),
                            score
                    ));
                }
            }
        }
    }

    private void collectLandscapes(MinecraftClient client, List<WeightedSubject> candidates) {
        PlayerEntity player = client.player;
        int playerY = player.getBlockY();
        int[] distances = {10, 18, 26};
        for (int direction = 0; direction < 12; direction++) {
            double angle = Math.PI * 2.0 * direction / 12.0;
            for (int distance : distances) {
                int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
                int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
                SurfacePoint surface = findTopSurface(client, x, z, playerY);
                if (surface == null || surface.water()) continue;
                double heightInterest = Math.min(18.0, Math.abs(surface.position().getY() - playerY) * 1.6);
                double score = 38.0 + distance * 0.55 + heightInterest;
                Vec3d target = surface.focus();
                candidates.add(new WeightedSubject(
                        new SceneSubject(SubjectType.LANDSCAPE, target, "landscape:" + direction + ":" + distance),
                        score
                ));
            }
        }
    }

    private SurfaceSurvey surveySurface(MinecraftClient client, Vec3d playerFocus) {
        int playerY = client.player.getBlockY();
        List<SurfacePoint> land = new ArrayList<>();
        int water = 0;
        int sampled = 0;
        for (int dx = -40; dx <= 40; dx += 8) {
            for (int dz = -40; dz <= 40; dz += 8) {
                int x = (int) Math.floor(playerFocus.x) + dx;
                int z = (int) Math.floor(playerFocus.z) + dz;
                SurfacePoint point = findTopSurface(client, x, z, playerY);
                if (point == null) continue;
                sampled++;
                if (point.water()) water++;
                else land.add(point);
            }
        }

        if (land.isEmpty()) {
            return new SurfaceSurvey(playerFocus, List.of(playerFocus), 8.0, 0.0,
                    sampled == 0 ? 0.0 : water / (double) sampled);
        }

        double centerX = land.stream().mapToDouble(point -> point.focus().x).average().orElse(playerFocus.x);
        double centerZ = land.stream().mapToDouble(point -> point.focus().z).average().orElse(playerFocus.z);
        Vec3d approximateCenter = new Vec3d(centerX, playerFocus.y, centerZ);
        Vec3d center = land.stream()
                .min(Comparator.comparingDouble(point -> horizontalDistance(point.focus(), approximateCenter)))
                .map(SurfacePoint::focus)
                .orElse(playerFocus);
        double centerY = center.y;
        double minimumY = land.stream().mapToDouble(point -> point.focus().y).min().orElse(centerY);
        double maximumY = land.stream().mapToDouble(point -> point.focus().y).max().orElse(centerY);
        double radius = land.stream().mapToDouble(point -> horizontalDistance(point.focus(), center)).max().orElse(8.0);

        List<Vec3d> anchors = new ArrayList<>();
        anchors.add(center);
        anchors.add(playerFocus);
        land.stream()
                .sorted(Comparator.comparingDouble((SurfacePoint point) -> horizontalDistance(point.focus(), center)).reversed())
                .limit(8)
                .map(SurfacePoint::focus)
                .forEach(anchors::add);
        land.stream()
                .max(Comparator.comparingInt(point -> point.position().getY()))
                .map(SurfacePoint::focus)
                .ifPresent(anchors::add);

        double coverage = sampled == 0 ? 0.0 : water / (double) sampled;
        return new SurfaceSurvey(center, anchors, clamp(radius, 8.0, 56.0), maximumY - minimumY, coverage);
    }

    private SurfacePoint findTopSurface(MinecraftClient client, int x, int z, int playerY) {
        for (int y = playerY + 32; y >= playerY - 24; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = client.world.getBlockState(pos);
            if (state.isAir()) continue;
            boolean water = !state.getFluidState().isEmpty();
            if (water || !state.getCollisionShape(client.world, pos).isEmpty()) {
                double focusHeight = water ? 0.35 : 1.15;
                return new SurfacePoint(pos, Vec3d.ofCenter(pos).add(0.0, focusHeight, 0.0), water);
            }
        }
        return null;
    }

    private double measureSkyVisibility(MinecraftClient client, Vec3d playerFocus) {
        Vec3d[] offsets = {
                Vec3d.ZERO,
                new Vec3d(2.5, 0.0, 0.0), new Vec3d(-2.5, 0.0, 0.0),
                new Vec3d(0.0, 0.0, 2.5), new Vec3d(0.0, 0.0, -2.5),
                new Vec3d(2.5, 0.0, 2.5), new Vec3d(-2.5, 0.0, 2.5),
                new Vec3d(2.5, 0.0, -2.5), new Vec3d(-2.5, 0.0, -2.5)
        };
        int visible = 0;
        for (Vec3d offset : offsets) {
            Vec3d start = playerFocus.add(offset);
            HitResult hit = raycast(client, start, start.add(0.0, 48.0, 0.0));
            if (hit.getType() == HitResult.Type.MISS) visible++;
        }
        return visible / (double) offsets.length;
    }

    private double rayDistance(MinecraftClient client, Vec3d start, Vec3d end, double missDistance) {
        HitResult hit = raycast(client, start, end);
        return hit.getType() == HitResult.Type.MISS ? missDistance : start.distanceTo(hit.getPos());
    }

    private HitResult raycast(MinecraftClient client, Vec3d start, Vec3d end) {
        return client.world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                client.player
        ));
    }

    private void remember(String key) {
        recentSubjects.remove(key);
        recentSubjects.addFirst(key);
        while (recentSubjects.size() > RECENT_SUBJECT_LIMIT) recentSubjects.removeLast();
    }

    private boolean isUsable(MinecraftClient client, Vec3d camera, Vec3d target) {
        Vec3d[] clearanceOffsets = {
                Vec3d.ZERO,
                new Vec3d(0.24, 0.0, 0.0), new Vec3d(-0.24, 0.0, 0.0),
                new Vec3d(0.0, 0.0, 0.24), new Vec3d(0.0, 0.0, -0.24),
                new Vec3d(0.0, 0.18, 0.0), new Vec3d(0.0, -0.18, 0.0)
        };
        for (Vec3d offset : clearanceOffsets) {
            BlockPos pos = BlockPos.ofFloored(camera.add(offset));
            if (!client.world.getBlockState(pos).getCollisionShape(client.world, pos).isEmpty()) return false;
        }
        HitResult floor = raycast(
                client,
                camera,
                camera.add(0.0, -MINIMUM_CAMERA_GROUND_CLEARANCE, 0.0)
        );
        if (floor.getType() != HitResult.Type.MISS) return false;
        return raycast(client, camera, target).getType() == HitResult.Type.MISS;
    }

    private static double horizontalDistance(Vec3d first, Vec3d second) {
        double dx = first.x - second.x;
        double dz = first.z - second.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private record WeightedSubject(SceneSubject subject, double score) { }

    private record SurfacePoint(BlockPos position, Vec3d focus, boolean water) { }

    private record SurfaceSurvey(
            Vec3d center,
            List<Vec3d> anchors,
            double radius,
            double relief,
            double waterCoverage
    ) { }
}

package com.cinecraft.director;

import com.cinecraft.camera.CameraPose;
import com.cinecraft.camera.CinematicShot;
import com.cinecraft.camera.MovingShot;
import com.cinecraft.camera.StaticShot;
import com.cinecraft.camera.TrackedMovingShot;
import com.cinecraft.compat.CinecraftFlawlessFrames;
import com.cinecraft.config.CinecraftConfig;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;

/** Directs environment-aware, independently generated shots until the player returns. */
public final class CinematicDirector {
    private final SceneScanner scanner = new SceneScanner();
    private final ShotPlanner planner = new ShotPlanner();
    private final Random random = new Random();
    private CinematicShot currentShot;
    private CameraPose currentPose;
    private ShotType currentShotType;
    private SubjectType currentSubjectType;
    private EnvironmentProfile profile;
    private EntityAction currentAction;
    private ShotComposition currentComposition;
    private String currentSubjectKey;
    private int shotNumber;
    private boolean hidHud;
    private boolean hudWasHidden;

    public void tick(MinecraftClient client) {
        if (!hidHud) {
            hudWasHidden = client.options.hudHidden;
            client.options.hudHidden = true;
            hidHud = true;
            CinecraftFlawlessFrames.setActive(true);
        }
        client.options.hudHidden = CinecraftConfig.INSTANCE.hideHud()
                && !CinecraftConfig.INSTANCE.debugOverlay();
        if (currentShot == null || currentShot.finished()) {
            if (profile == null || shotNumber % 4 == 0) profile = scanner.survey(client);
            ShotSelection selection = chooseShot(client, profile);
            boolean wide = selection.wide();
            SceneSubject subject = selection.subject();
            Optional<ShotPlan> planned = planner.plan(client, scanner, subject, profile, wide);
            if (planned.isEmpty() && wide) {
                wide = false;
                subject = scanner.findEnvironmentSubject(client);
                planned = planner.plan(client, scanner, subject, profile, false);
            }
            if (planned.isEmpty() && subject.type() != SubjectType.PLAYER) {
                subject = scanner.playerSubject(client);
                planned = planner.plan(client, scanner, subject, profile, false);
            }

            if (planned.isPresent()) {
                ShotPlan plan = planned.get();
                currentShotType = plan.type();
                currentSubjectType = subject.type();
                currentAction = subject.action();
                currentComposition = plan.composition();
                currentSubjectKey = subject.key();
                if (subject.hasLiveTracking()) {
                    currentShot = new TrackedMovingShot(
                            plan.path(),
                            plan.focusPath(),
                            subject::currentTarget,
                            subject::isAvailable,
                            subject.target(),
                            plan.fovPath(),
                            plan.durationMillis(),
                            (camera, focus) -> scanner.resolveRuntimeCamera(client, camera, focus)
                    );
                } else {
                    currentShot = new MovingShot(
                            plan.path(),
                            plan.focusPath(),
                            plan.fovPath(),
                            plan.durationMillis()
                    );
                }
            } else {
                CameraPose startingPose = scanner.findPlayerView(client);
                if (startingPose == null) return; // Never force the camera through a wall.
                currentShotType = null;
                currentSubjectType = SubjectType.PLAYER;
                currentShot = new StaticShot(startingPose, 4_000L);
            }
            shotNumber++;
        }
        currentPose = currentShot.sample(0.0f);
    }

    private ShotSelection chooseShot(MinecraftClient client, EnvironmentProfile currentProfile) {
        ShotSelection selected = chooseWeighted(client, currentProfile);
        for (int retry = 0; retry < 3 && selected.subject().type() == currentSubjectType; retry++) {
            selected = chooseWeighted(client, currentProfile);
        }
        return selected;
    }

    private ShotSelection chooseWeighted(MinecraftClient client, EnvironmentProfile currentProfile) {
        CinecraftConfig config = CinecraftConfig.INSTANCE;
        List<WeightedChoice> choices = new ArrayList<>();
        double wideBonus = switch (currentProfile.biomeMood()) {
            case MOUNTAIN, OCEAN, END -> 10.0;
            case PLAINS, DESERT, SNOW -> 4.0;
            case FOREST, SWAMP -> -5.0;
            default -> 0.0;
        };
        if (currentProfile.sceneTime() == SceneTime.SUNRISE || currentProfile.sceneTime() == SceneTime.SUNSET) {
            wideBonus += 6.0;
        }
        if (currentProfile.weather() == SceneWeather.THUNDER) wideBonus -= 4.0;

        if (config.landscapeShots() && currentProfile.supportsWideShots()) {
            add(choices, Math.max(5.0, 20.0 + wideBonus),
                    () -> new ShotSelection(scanner.landscapeSubject(currentProfile), true));
        }
        if (config.entityShots()) {
            add(choices, currentProfile.underground() ? 26.0 : 22.0,
                    () -> new ShotSelection(scanner.findSubject(client, SubjectType.ENTITY), false));
        }
        if (config.groupShots()) {
            add(choices, currentProfile.underground() ? 9.0 : 13.0,
                    () -> new ShotSelection(scanner.findSubject(client, SubjectType.GROUP), false));
        }
        if (config.featureShots() && (!currentProfile.underground() || config.interiorShots())) {
            double featureWeight = currentProfile.biomeMood() == BiomeMood.FOREST || currentProfile.underground()
                    ? 21.0
                    : 14.0;
            add(choices, featureWeight,
                    () -> new ShotSelection(scanner.findSubject(client, SubjectType.FEATURE), false));
        }
        if (config.landscapeShots() && (!currentProfile.underground() || config.interiorShots())) {
            add(choices, currentProfile.underground() ? 10.0 : 12.0,
                    () -> new ShotSelection(scanner.findSubject(client, SubjectType.LANDSCAPE), false));
        }
        if (config.playerShots()) {
            add(choices, 10.0, () -> new ShotSelection(scanner.playerSubject(client), false));
        }
        if (config.playerDetailShots()) {
            add(choices, 11.0, () -> new ShotSelection(scanner.playerDetailSubject(client), false));
        }
        if (choices.isEmpty()) return new ShotSelection(scanner.playerSubject(client), false);

        double total = choices.stream().mapToDouble(WeightedChoice::weight).sum();
        double roll = random.nextDouble(total);
        for (WeightedChoice choice : choices) {
            roll -= choice.weight();
            if (roll <= 0.0) return choice.factory().get();
        }
        return choices.getLast().factory().get();
    }

    private static void add(List<WeightedChoice> choices, double weight, Supplier<ShotSelection> factory) {
        if (weight > 0.0) choices.add(new WeightedChoice(weight, factory));
    }

    public CameraPose pose(float tickDelta) {
        if (currentShot != null) currentPose = currentShot.sample(tickDelta);
        return currentPose;
    }

    public void stop() {
        CinecraftFlawlessFrames.setActive(false);
        currentShot = null;
        currentPose = null;
        currentShotType = null;
        currentSubjectType = null;
        currentAction = null;
        currentComposition = null;
        currentSubjectKey = null;
        profile = null;
        shotNumber = 0;
        scanner.resetSubjects();
        planner.reset();
        if (hidHud) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) client.options.hudHidden = hudWasHidden;
            hidHud = false;
        }
    }

    public ShotType currentShotType() {
        return currentShotType;
    }

    public SubjectType currentSubjectType() {
        return currentSubjectType;
    }

    public SpaceType currentSpaceType() {
        return profile == null ? null : profile.spaceType();
    }

    public boolean isUnderground() {
        return profile != null && profile.underground();
    }

    public boolean isActive() {
        return currentShot != null;
    }

    /** Ends only the current composition so the next director tick plans a fresh cut. */
    public void nextShot() {
        currentShot = null;
        currentPose = null;
    }

    public float currentFov() {
        return currentPose == null ? Float.NaN : currentPose.fov();
    }

    /** Public compatibility hint for shader/recording integrations. */
    public float currentFocusDistance() {
        return currentPose == null ? Float.NaN : currentPose.focusDistance();
    }

    public EntityAction currentAction() { return currentAction; }
    public ShotComposition currentComposition() { return currentComposition; }
    public String currentSubjectKey() { return currentSubjectKey; }
    public EnvironmentProfile currentProfile() { return profile; }
    public String plannerSource() { return planner.diagnosticSource(); }
    public String plannerDecision() { return planner.diagnosticDecision(); }
    public int plannerRejected() { return planner.diagnosticRejected(); }

    private record ShotSelection(SceneSubject subject, boolean wide) { }
    private record WeightedChoice(double weight, Supplier<ShotSelection> factory) { }
}

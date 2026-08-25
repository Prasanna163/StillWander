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

import com.cinecraft.camera.CameraPose;
import com.cinecraft.camera.CinematicShot;
import com.cinecraft.camera.MovingShot;
import com.cinecraft.camera.StaticShot;
import com.cinecraft.camera.TrackedMovingShot;
import com.cinecraft.compat.CinecraftFlawlessFrames;
import com.cinecraft.config.CinecraftConfig;
import com.cinecraft.director.BiomeMood;
import com.cinecraft.director.EntityAction;
import com.cinecraft.director.EnvironmentProfile;
import com.cinecraft.director.SceneScanner;
import com.cinecraft.director.SceneSubject;
import com.cinecraft.director.SceneTime;
import com.cinecraft.director.SceneWeather;
import com.cinecraft.director.ShotComposition;
import com.cinecraft.director.ShotPlan;
import com.cinecraft.director.ShotPlanner;
import com.cinecraft.director.ShotType;
import com.cinecraft.director.SpaceType;
import com.cinecraft.director.SubjectType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_243;
import net.minecraft.class_310;

@Environment(value=EnvType.CLIENT)
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

    public void tick(class_310 client) {
        if (!this.hidHud) {
            this.hudWasHidden = client.field_1690.field_1842;
            client.field_1690.field_1842 = true;
            this.hidHud = true;
            CinecraftFlawlessFrames.setActive(true);
        }
        boolean bl = client.field_1690.field_1842 = CinecraftConfig.INSTANCE.hideHud() && !CinecraftConfig.INSTANCE.debugOverlay();
        if (this.currentShot == null || this.currentShot.finished()) {
            if (this.profile == null || this.shotNumber % 4 == 0) {
                this.profile = this.scanner.survey(client);
            }
            ShotSelection selection = this.chooseShot(client, this.profile);
            boolean wide = selection.wide();
            SceneSubject subject = selection.subject();
            Optional<ShotPlan> planned = this.planner.plan(client, this.scanner, subject, this.profile, wide);
            if (planned.isEmpty() && wide) {
                wide = false;
                subject = this.scanner.findEnvironmentSubject(client);
                planned = this.planner.plan(client, this.scanner, subject, this.profile, false);
            }
            if (planned.isEmpty() && subject.type() != SubjectType.PLAYER) {
                subject = this.scanner.playerSubject(client);
                planned = this.planner.plan(client, this.scanner, subject, this.profile, false);
            }
            if (planned.isPresent()) {
                ShotPlan plan = planned.get();
                this.currentShotType = plan.type();
                this.currentSubjectType = subject.type();
                this.currentAction = subject.action();
                this.currentComposition = plan.composition();
                this.currentSubjectKey = subject.key();
                this.currentShot = subject.hasLiveTracking() ? new TrackedMovingShot(plan.path(), plan.focusPath(), subject::currentTarget, subject::isAvailable, subject.target(), plan.fovPath(), plan.durationMillis(), (camera, focus) -> this.scanner.resolveRuntimeCamera(client, (class_243)camera, (class_243)focus)) : new MovingShot(plan.path(), plan.focusPath(), plan.fovPath(), plan.durationMillis());
            } else {
                CameraPose startingPose = this.scanner.findPlayerView(client);
                if (startingPose == null) {
                    return;
                }
                this.currentShotType = null;
                this.currentSubjectType = SubjectType.PLAYER;
                this.currentShot = new StaticShot(startingPose, 4000L);
            }
            ++this.shotNumber;
        }
        this.currentPose = this.currentShot.sample(0.0f);
    }

    private ShotSelection chooseShot(class_310 client, EnvironmentProfile currentProfile) {
        ShotSelection selected = this.chooseWeighted(client, currentProfile);
        for (int retry = 0; retry < 3 && selected.subject().type() == this.currentSubjectType; ++retry) {
            selected = this.chooseWeighted(client, currentProfile);
        }
        return selected;
    }

    private ShotSelection chooseWeighted(class_310 client, EnvironmentProfile currentProfile) {
        double wideBonus;
        CinecraftConfig config = CinecraftConfig.INSTANCE;
        ArrayList<WeightedChoice> choices = new ArrayList<WeightedChoice>();
        switch (currentProfile.biomeMood()) {
            case MOUNTAIN: 
            case OCEAN: 
            case END: {
                double d = 10.0;
                break;
            }
            case PLAINS: 
            case DESERT: 
            case SNOW: {
                double d = 4.0;
                break;
            }
            case FOREST: 
            case SWAMP: {
                double d = -5.0;
                break;
            }
            default: {
                double d = wideBonus = 0.0;
            }
        }
        if (currentProfile.sceneTime() == SceneTime.SUNRISE || currentProfile.sceneTime() == SceneTime.SUNSET) {
            wideBonus += 6.0;
        }
        if (currentProfile.weather() == SceneWeather.THUNDER) {
            wideBonus -= 4.0;
        }
        if (config.landscapeShots() && currentProfile.supportsWideShots()) {
            CinematicDirector.add(choices, Math.max(5.0, 20.0 + wideBonus), () -> new ShotSelection(this.scanner.landscapeSubject(currentProfile), true));
        }
        if (config.entityShots()) {
            CinematicDirector.add(choices, currentProfile.underground() ? 26.0 : 22.0, () -> new ShotSelection(this.scanner.findSubject(client, SubjectType.ENTITY), false));
        }
        if (config.groupShots()) {
            CinematicDirector.add(choices, currentProfile.underground() ? 9.0 : 13.0, () -> new ShotSelection(this.scanner.findSubject(client, SubjectType.GROUP), false));
        }
        if (config.featureShots() && (!currentProfile.underground() || config.interiorShots())) {
            double featureWeight = currentProfile.biomeMood() == BiomeMood.FOREST || currentProfile.underground() ? 21.0 : 14.0;
            CinematicDirector.add(choices, featureWeight, () -> new ShotSelection(this.scanner.findSubject(client, SubjectType.FEATURE), false));
        }
        if (config.landscapeShots() && (!currentProfile.underground() || config.interiorShots())) {
            CinematicDirector.add(choices, currentProfile.underground() ? 10.0 : 12.0, () -> new ShotSelection(this.scanner.findSubject(client, SubjectType.LANDSCAPE), false));
        }
        if (config.playerShots()) {
            CinematicDirector.add(choices, 10.0, () -> new ShotSelection(this.scanner.playerSubject(client), false));
        }
        if (config.playerDetailShots()) {
            CinematicDirector.add(choices, 11.0, () -> new ShotSelection(this.scanner.playerDetailSubject(client), false));
        }
        if (choices.isEmpty()) {
            return new ShotSelection(this.scanner.playerSubject(client), false);
        }
        double total = choices.stream().mapToDouble(WeightedChoice::weight).sum();
        double roll = this.random.nextDouble(total);
        for (WeightedChoice choice : choices) {
            if (!((roll -= choice.weight()) <= 0.0)) continue;
            return choice.factory().get();
        }
        return ((WeightedChoice)choices.getLast()).factory().get();
    }

    private static void add(List<WeightedChoice> choices, double weight, Supplier<ShotSelection> factory) {
        if (weight > 0.0) {
            choices.add(new WeightedChoice(weight, factory));
        }
    }

    public CameraPose pose(float tickDelta) {
        if (this.currentShot != null) {
            this.currentPose = this.currentShot.sample(tickDelta);
        }
        return this.currentPose;
    }

    public void stop() {
        CinecraftFlawlessFrames.setActive(false);
        this.currentShot = null;
        this.currentPose = null;
        this.currentShotType = null;
        this.currentSubjectType = null;
        this.currentAction = null;
        this.currentComposition = null;
        this.currentSubjectKey = null;
        this.profile = null;
        this.shotNumber = 0;
        this.scanner.resetSubjects();
        this.planner.reset();
        if (this.hidHud) {
            class_310 client = class_310.method_1551();
            if (client != null) {
                client.field_1690.field_1842 = this.hudWasHidden;
            }
            this.hidHud = false;
        }
    }

    public ShotType currentShotType() {
        return this.currentShotType;
    }

    public SubjectType currentSubjectType() {
        return this.currentSubjectType;
    }

    public SpaceType currentSpaceType() {
        return this.profile == null ? null : this.profile.spaceType();
    }

    public boolean isUnderground() {
        return this.profile != null && this.profile.underground();
    }

    public boolean isActive() {
        return this.currentShot != null;
    }

    public void nextShot() {
        this.currentShot = null;
        this.currentPose = null;
    }

    public float currentFov() {
        return this.currentPose == null ? Float.NaN : this.currentPose.fov();
    }

    public float currentFocusDistance() {
        return this.currentPose == null ? Float.NaN : this.currentPose.focusDistance();
    }

    public EntityAction currentAction() {
        return this.currentAction;
    }

    public ShotComposition currentComposition() {
        return this.currentComposition;
    }

    public String currentSubjectKey() {
        return this.currentSubjectKey;
    }

    public EnvironmentProfile currentProfile() {
        return this.profile;
    }

    public String plannerSource() {
        return this.planner.diagnosticSource();
    }

    public String plannerDecision() {
        return this.planner.diagnosticDecision();
    }

    public int plannerRejected() {
        return this.planner.diagnosticRejected();
    }

    @Environment(value=EnvType.CLIENT)
    private record ShotSelection(SceneSubject subject, boolean wide) {
    }

    @Environment(value=EnvType.CLIENT)
    private record WeightedChoice(double weight, Supplier<ShotSelection> factory) {
    }
}


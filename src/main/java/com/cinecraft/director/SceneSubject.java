/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.minecraft.class_1297
 *  net.minecraft.class_1306
 *  net.minecraft.class_1309
 *  net.minecraft.class_243
 */
package com.cinecraft.director;

import com.cinecraft.director.EntityAction;
import com.cinecraft.director.SubjectFocus;
import com.cinecraft.director.SubjectType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_1306;
import net.minecraft.class_1309;
import net.minecraft.class_243;

@Environment(value=EnvType.CLIENT)
public final class SceneSubject {
    private final SubjectType type;
    private final class_243 target;
    private final String key;
    private final class_1297 trackedEntity;
    private final SubjectFocus focus;
    private final class_243 primaryTarget;
    private final class_1297 secondaryEntity;
    private final class_243 secondaryTarget;
    private final EntityAction action;

    public SceneSubject(SubjectType type, class_243 target, String key) {
        this(type, target, key, null, SubjectFocus.CENTER);
    }

    public SceneSubject(SubjectType type, class_243 target, String key, class_1297 trackedEntity) {
        this(type, target, key, trackedEntity, SubjectFocus.CENTER);
    }

    public SceneSubject(SubjectType type, class_243 target, String key, class_1297 trackedEntity, SubjectFocus focus) {
        this(type, target, key, trackedEntity, focus, target, null, null, trackedEntity == null ? EntityAction.STILL : EntityAction.detect(trackedEntity));
    }

    private SceneSubject(SubjectType type, class_243 target, String key, class_1297 trackedEntity, SubjectFocus focus, class_243 primaryTarget, class_1297 secondaryEntity, class_243 secondaryTarget, EntityAction action) {
        this.type = type;
        this.target = target;
        this.key = key;
        this.trackedEntity = trackedEntity;
        this.focus = focus;
        this.primaryTarget = primaryTarget;
        this.secondaryEntity = secondaryEntity;
        this.secondaryTarget = secondaryTarget;
        this.action = action;
    }

    public static SceneSubject entityGroup(class_1297 first, class_1297 second) {
        class_243 firstTarget = SceneSubject.targetFor(first, SubjectFocus.CENTER);
        class_243 secondTarget = SceneSubject.targetFor(second, SubjectFocus.CENTER);
        return new SceneSubject(SubjectType.GROUP, firstTarget.method_35590(secondTarget, 0.5), "group:" + String.valueOf(first.method_5667()) + ":" + String.valueOf(second.method_5667()), first, SubjectFocus.CENTER, firstTarget, second, secondTarget, SceneSubject.faster(EntityAction.detect(first), EntityAction.detect(second)));
    }

    public static SceneSubject playerAndFeature(class_1297 player, class_243 feature, String key) {
        class_243 playerTarget = SceneSubject.targetFor(player, SubjectFocus.CENTER);
        return new SceneSubject(SubjectType.GROUP, playerTarget.method_35590(feature, 0.5), key, player, SubjectFocus.CENTER, playerTarget, null, feature, EntityAction.STILL);
    }

    public SubjectType type() {
        return this.type;
    }

    public class_243 target() {
        return this.target;
    }

    public String key() {
        return this.key;
    }

    public class_1297 trackedEntity() {
        return this.trackedEntity;
    }

    public SubjectFocus focus() {
        return this.focus;
    }

    public class_1297 secondaryEntity() {
        return this.secondaryEntity;
    }

    public EntityAction action() {
        return this.action;
    }

    public boolean isGroup() {
        return this.type == SubjectType.GROUP;
    }

    public boolean hasLiveTracking() {
        return this.trackedEntity != null;
    }

    public class_243 movementVector() {
        if (this.trackedEntity == null) {
            return class_243.field_1353;
        }
        class_243 movement = this.trackedEntity.method_18798();
        if (this.secondaryEntity != null) {
            movement = movement.method_1019(this.secondaryEntity.method_18798()).method_1021(0.5);
        }
        return movement;
    }

    public boolean isAvailable() {
        if (this.trackedEntity != null && (this.trackedEntity.method_31481() || !this.trackedEntity.method_5805())) {
            return false;
        }
        return this.secondaryEntity == null || !this.secondaryEntity.method_31481() && this.secondaryEntity.method_5805();
    }

    public class_243 currentTarget() {
        class_243 averageVelocity;
        class_243 primary = this.primaryCurrentTarget();
        if (this.secondaryTarget == null) {
            return SceneSubject.addLead(primary, this.trackedEntity, this.action);
        }
        class_243 secondary = this.secondaryCurrentTarget();
        class_243 center = primary.method_35590(secondary, 0.5);
        class_243 class_2432 = averageVelocity = this.trackedEntity == null ? class_243.field_1353 : this.trackedEntity.method_18798();
        if (this.secondaryEntity != null) {
            averageVelocity = averageVelocity.method_1019(this.secondaryEntity.method_18798()).method_1021(0.5);
        }
        return SceneSubject.addLead(center, averageVelocity, this.action.leadDistance() * 0.65);
    }

    public class_243 rackFocusTarget(double progress) {
        if (this.secondaryTarget == null) {
            return this.currentTarget();
        }
        double eased = progress * progress * (3.0 - 2.0 * progress);
        return this.primaryCurrentTarget().method_35590(this.secondaryCurrentTarget(), eased);
    }

    private class_243 primaryCurrentTarget() {
        if (this.trackedEntity == null || this.trackedEntity.method_31481()) {
            return this.primaryTarget;
        }
        return SceneSubject.targetFor(this.trackedEntity, this.focus);
    }

    private class_243 secondaryCurrentTarget() {
        if (this.secondaryEntity == null || this.secondaryEntity.method_31481()) {
            return this.secondaryTarget;
        }
        return SceneSubject.targetFor(this.secondaryEntity, SubjectFocus.CENTER);
    }

    private static class_243 addLead(class_243 point, class_1297 entity, EntityAction action) {
        if (entity == null) {
            return point;
        }
        return SceneSubject.addLead(point, entity.method_18798(), action.leadDistance());
    }

    private static class_243 addLead(class_243 point, class_243 velocity, double distance) {
        class_243 horizontal = new class_243(velocity.field_1352, 0.0, velocity.field_1350);
        if (horizontal.method_1027() < 1.0E-4 || distance <= 0.0) {
            return point;
        }
        return point.method_1019(horizontal.method_1029().method_1021(distance));
    }

    public static class_243 targetFor(class_1297 entity, SubjectFocus focus) {
        class_243 position = entity.method_73189();
        double height = entity.method_17682();
        return switch (focus) {
            default -> throw new MatchException(null, null);
            case SubjectFocus.CENTER -> position.method_1031(0.0, height * 0.55, 0.0);
            case SubjectFocus.HEAD -> position.method_1031(0.0, height * 0.86, 0.0);
            case SubjectFocus.CHEST -> position.method_1031(0.0, height * 0.62, 0.0);
            case SubjectFocus.MAIN_HAND, SubjectFocus.OFF_HAND -> SceneSubject.handTarget(entity, focus == SubjectFocus.MAIN_HAND);
        };
    }

    private static class_243 handTarget(class_1297 entity, boolean mainHand) {
        class_1309 living;
        double yaw = Math.toRadians(entity.method_36454());
        class_243 forward = new class_243(-Math.sin(yaw), 0.0, Math.cos(yaw));
        class_243 right = new class_243(Math.cos(yaw), 0.0, Math.sin(yaw));
        boolean mainArmRight = !(entity instanceof class_1309) || (living = (class_1309)entity).method_6068() == class_1306.field_6183;
        boolean useRight = mainHand == mainArmRight;
        double side = useRight ? 0.42 : -0.42;
        return entity.method_73189().method_1031(0.0, (double)entity.method_17682() * 0.56, 0.0).method_1019(right.method_1021(side)).method_1019(forward.method_1021(0.14));
    }

    private static EntityAction faster(EntityAction first, EntityAction second) {
        return SceneSubject.actionRank(first) >= SceneSubject.actionRank(second) ? first : second;
    }

    private static int actionRank(EntityAction action) {
        return switch (action) {
            default -> throw new MatchException(null, null);
            case EntityAction.COMBAT -> 7;
            case EntityAction.FLYING, EntityAction.RUNNING -> 6;
            case EntityAction.SWIMMING -> 5;
            case EntityAction.WALKING -> 4;
            case EntityAction.USING_ITEM -> 3;
            case EntityAction.STILL -> 2;
            case EntityAction.SLEEPING -> 1;
        };
    }
}


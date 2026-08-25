package com.cinecraft.director;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.Vec3d;

/** One live subject, a fixed feature, or a two-subject composition. */
public final class SceneSubject {
    private final SubjectType type;
    private final Vec3d target;
    private final String key;
    private final Entity trackedEntity;
    private final SubjectFocus focus;
    private final Vec3d primaryTarget;
    private final Entity secondaryEntity;
    private final Vec3d secondaryTarget;
    private final EntityAction action;

    public SceneSubject(SubjectType type, Vec3d target, String key) {
        this(type, target, key, null, SubjectFocus.CENTER);
    }

    public SceneSubject(SubjectType type, Vec3d target, String key, Entity trackedEntity) {
        this(type, target, key, trackedEntity, SubjectFocus.CENTER);
    }

    public SceneSubject(
            SubjectType type,
            Vec3d target,
            String key,
            Entity trackedEntity,
            SubjectFocus focus
    ) {
        this(
                type,
                target,
                key,
                trackedEntity,
                focus,
                target,
                null,
                null,
                trackedEntity == null ? EntityAction.STILL : EntityAction.detect(trackedEntity)
        );
    }

    private SceneSubject(
            SubjectType type,
            Vec3d target,
            String key,
            Entity trackedEntity,
            SubjectFocus focus,
            Vec3d primaryTarget,
            Entity secondaryEntity,
            Vec3d secondaryTarget,
            EntityAction action
    ) {
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

    public static SceneSubject entityGroup(Entity first, Entity second) {
        Vec3d firstTarget = targetFor(first, SubjectFocus.CENTER);
        Vec3d secondTarget = targetFor(second, SubjectFocus.CENTER);
        return new SceneSubject(
                SubjectType.GROUP,
                firstTarget.lerp(secondTarget, 0.5),
                "group:" + first.getUuid() + ":" + second.getUuid(),
                first,
                SubjectFocus.CENTER,
                firstTarget,
                second,
                secondTarget,
                faster(EntityAction.detect(first), EntityAction.detect(second))
        );
    }

    public static SceneSubject playerAndFeature(Entity player, Vec3d feature, String key) {
        Vec3d playerTarget = targetFor(player, SubjectFocus.CENTER);
        return new SceneSubject(
                SubjectType.GROUP,
                playerTarget.lerp(feature, 0.5),
                key,
                player,
                SubjectFocus.CENTER,
                playerTarget,
                null,
                feature,
                EntityAction.STILL
        );
    }

    public SubjectType type() { return type; }
    public Vec3d target() { return target; }
    public String key() { return key; }
    public Entity trackedEntity() { return trackedEntity; }
    public SubjectFocus focus() { return focus; }
    public Entity secondaryEntity() { return secondaryEntity; }
    public EntityAction action() { return action; }
    public boolean isGroup() { return type == SubjectType.GROUP; }
    public boolean hasLiveTracking() { return trackedEntity != null; }

    public Vec3d movementVector() {
        if (trackedEntity == null) return Vec3d.ZERO;
        Vec3d movement = trackedEntity.getVelocity();
        if (secondaryEntity != null) movement = movement.add(secondaryEntity.getVelocity()).multiply(0.5);
        return movement;
    }

    public boolean isAvailable() {
        if (trackedEntity != null && (trackedEntity.isRemoved() || !trackedEntity.isAlive())) return false;
        return secondaryEntity == null || (!secondaryEntity.isRemoved() && secondaryEntity.isAlive());
    }

    public Vec3d currentTarget() {
        Vec3d primary = primaryCurrentTarget();
        if (secondaryTarget == null) return addLead(primary, trackedEntity, action);
        Vec3d secondary = secondaryCurrentTarget();
        Vec3d center = primary.lerp(secondary, 0.5);
        Vec3d averageVelocity = trackedEntity == null ? Vec3d.ZERO : trackedEntity.getVelocity();
        if (secondaryEntity != null) averageVelocity = averageVelocity.add(secondaryEntity.getVelocity()).multiply(0.5);
        return addLead(center, averageVelocity, action.leadDistance() * 0.65);
    }

    public Vec3d rackFocusTarget(double progress) {
        if (secondaryTarget == null) return currentTarget();
        double eased = progress * progress * (3.0 - 2.0 * progress);
        return primaryCurrentTarget().lerp(secondaryCurrentTarget(), eased);
    }

    private Vec3d primaryCurrentTarget() {
        if (trackedEntity == null || trackedEntity.isRemoved()) return primaryTarget;
        return targetFor(trackedEntity, focus);
    }

    private Vec3d secondaryCurrentTarget() {
        if (secondaryEntity == null || secondaryEntity.isRemoved()) return secondaryTarget;
        return targetFor(secondaryEntity, SubjectFocus.CENTER);
    }

    private static Vec3d addLead(Vec3d point, Entity entity, EntityAction action) {
        if (entity == null) return point;
        return addLead(point, entity.getVelocity(), action.leadDistance());
    }

    private static Vec3d addLead(Vec3d point, Vec3d velocity, double distance) {
        Vec3d horizontal = new Vec3d(velocity.x, 0.0, velocity.z);
        if (horizontal.lengthSquared() < 0.0001 || distance <= 0.0) return point;
        return point.add(horizontal.normalize().multiply(distance));
    }

    public static Vec3d targetFor(Entity entity, SubjectFocus focus) {
        Vec3d position = entity.getEntityPos();
        double height = entity.getHeight();
        return switch (focus) {
            case CENTER -> position.add(0.0, height * 0.55, 0.0);
            case HEAD -> position.add(0.0, height * 0.86, 0.0);
            case CHEST -> position.add(0.0, height * 0.62, 0.0);
            case MAIN_HAND, OFF_HAND -> handTarget(entity, focus == SubjectFocus.MAIN_HAND);
        };
    }

    private static Vec3d handTarget(Entity entity, boolean mainHand) {
        double yaw = Math.toRadians(entity.getYaw());
        Vec3d forward = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3d right = new Vec3d(Math.cos(yaw), 0.0, Math.sin(yaw));
        boolean mainArmRight = !(entity instanceof LivingEntity living) || living.getMainArm() == Arm.RIGHT;
        boolean useRight = mainHand == mainArmRight;
        double side = useRight ? 0.42 : -0.42;
        return entity.getEntityPos()
                .add(0.0, entity.getHeight() * 0.56, 0.0)
                .add(right.multiply(side))
                .add(forward.multiply(0.14));
    }

    private static EntityAction faster(EntityAction first, EntityAction second) {
        return actionRank(first) >= actionRank(second) ? first : second;
    }

    private static int actionRank(EntityAction action) {
        return switch (action) {
            case COMBAT -> 7;
            case FLYING, RUNNING -> 6;
            case SWIMMING -> 5;
            case WALKING -> 4;
            case USING_ITEM -> 3;
            case STILL -> 2;
            case SLEEPING -> 1;
        };
    }
}

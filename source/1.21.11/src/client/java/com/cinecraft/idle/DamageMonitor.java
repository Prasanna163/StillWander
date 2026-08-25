package com.cinecraft.idle;

import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

/** Detects actual damage, including damage absorbed by golden hearts. */
public final class DamageMonitor {
    private static final float EPSILON = 0.001f;

    private UUID playerId;
    private float previousVitality = Float.NaN;

    public boolean update(PlayerEntity player) {
        UUID currentId = player.getUuid();
        float vitality = player.getHealth() + player.getAbsorptionAmount();

        if (!currentId.equals(playerId) || Float.isNaN(previousVitality)) {
            playerId = currentId;
            previousVitality = vitality;
            return false;
        }

        boolean damaged = vitality + EPSILON < previousVitality;
        previousVitality = vitality;
        return damaged;
    }

    public void reset() {
        playerId = null;
        previousVitality = Float.NaN;
    }
}

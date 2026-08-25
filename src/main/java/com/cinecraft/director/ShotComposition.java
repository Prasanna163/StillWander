/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.director;

import com.cinecraft.director.EntityAction;
import com.cinecraft.director.Framing;
import com.cinecraft.director.ScreenPlacement;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public record ShotComposition(Framing framing, ScreenPlacement placement, int movementDirection, EntityAction action, boolean rackFocus) {
}


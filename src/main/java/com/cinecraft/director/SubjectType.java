/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 */
package com.cinecraft.director;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public enum SubjectType {
    PLAYER,
    PLAYER_DETAIL,
    ENTITY,
    GROUP,
    FEATURE,
    LANDSCAPE;

}


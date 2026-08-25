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
public enum ShotType {
    PROCEDURAL_SUBJECT,
    PROCEDURAL_TRAVERSE,
    PROCEDURAL_REVEAL,
    PROCEDURAL_PASSAGE,
    PROCEDURAL_PANORAMA,
    PROCEDURAL_AERIAL,
    PROCEDURAL_INTERIOR;

}


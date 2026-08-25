/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.EnvType
 *  net.fabricmc.api.Environment
 *  net.fabricmc.loader.api.FabricLoader
 */
package com.cinecraft.compat;

import java.lang.reflect.Method;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(value=EnvType.CLIENT)
public final class RendererCompatibility {
    private static volatile boolean irisResolved;
    private static Object irisApi;
    private static Method irisShaderQuery;

    private RendererCompatibility() {
    }

    public static boolean shadersActive() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return FabricLoader.getInstance().isModLoaded("oculus");
        }
        if (!irisResolved) {
            RendererCompatibility.resolveIris();
        }
        if (irisApi == null || irisShaderQuery == null) {
            return true;
        }
        try {
            return (Boolean)irisShaderQuery.invoke(irisApi, new Object[0]);
        }
        catch (LinkageError | ReflectiveOperationException ignored) {
            return true;
        }
    }

    private static synchronized void resolveIris() {
        if (irisResolved) {
            return;
        }
        irisResolved = true;
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisApi = apiClass.getMethod("getInstance", new Class[0]).invoke(null, new Object[0]);
            irisShaderQuery = apiClass.getMethod("isShaderPackInUse", new Class[0]);
        }
        catch (LinkageError | ReflectiveOperationException ignored) {
            irisApi = null;
            irisShaderQuery = null;
        }
    }

    public static boolean distantHorizonsAvailable() {
        return FabricLoader.getInstance().isModLoaded("distanthorizons");
    }

    public static boolean dynamicFpsAvailable() {
        return FabricLoader.getInstance().isModLoaded("dynamic_fps");
    }
}


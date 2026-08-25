package com.cinecraft.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Method;

/**
 * Deliberately detects render mods without linking to or modifying them.
 * Cinecraft owns camera state only; vanilla/Sodium/Iris retain the render path.
 */
public final class RendererCompatibility {
    private static volatile boolean irisResolved;
    private static Object irisApi;
    private static Method irisShaderQuery;

    private RendererCompatibility() { }

    public static boolean shadersActive() {
        if (!FabricLoader.getInstance().isModLoaded("iris")) {
            return FabricLoader.getInstance().isModLoaded("oculus");
        }
        if (!irisResolved) resolveIris();
        if (irisApi == null || irisShaderQuery == null) return true;
        try {
            return (Boolean) irisShaderQuery.invoke(irisApi);
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return true;
        }
    }

    private static synchronized void resolveIris() {
        if (irisResolved) return;
        irisResolved = true;
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            irisApi = apiClass.getMethod("getInstance").invoke(null);
            irisShaderQuery = apiClass.getMethod("isShaderPackInUse");
        } catch (ReflectiveOperationException | LinkageError ignored) {
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

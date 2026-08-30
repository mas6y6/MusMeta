package com.mas6y6.musmeta.plugin.mixin;

import com.mas6y6.musmeta.launch.KnotClassLoader;
import com.mas6y6.musmeta.launch.LaunchTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.service.MixinService;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public final class PluginMixinManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("musmeta.plugin.mixin");

    private static final PluginMixinManager INSTANCE = new PluginMixinManager();

    private boolean booted;
    private boolean finished;

    private PluginMixinManager() {
    }

    public static PluginMixinManager getInstance() {
        return INSTANCE;
    }

    public synchronized void boot() {
        if (booted) {
            return;
        }
        MixinService.boot();
        MixinBootstrap.init();
        booted = true;
        LOGGER.info("Mixin subsystem booted with service '{}'.", MixinService.getService().getName());
    }

    public synchronized void registerConfig(String configFile, ClassLoader pluginLoader) {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(pluginLoader);
            Mixins.addConfiguration(configFile);
            LOGGER.info("Registered mixin config '{}'.", configFile);
        } catch (Throwable t) {
            LOGGER.error("Failed to register mixin config '{}': {}", configFile, t.getMessage(), t);
        } finally {
            Thread.currentThread().setContextClassLoader(previous);
        }
    }

    public synchronized void finish() {
        if (finished) {
            return;
        }
        Object transformer = createTransformer();
        LaunchTransformer bridge = new MixinLaunchBridge(transformer);
        KnotClassLoader knot = KnotClassLoader.instance();
        if (knot != null) {
            knot.installTransformer(bridge);
        } else {
            LOGGER.warn("No knot classloader installed; mixins will not be applied to application classes.");
        }
        finished = true;
        LOGGER.info("Mixin transformer active ({} unvisited config(s)).", Mixins.getUnvisitedCount());
    }

    private static Object createTransformer() {
        try {
            advanceToDefaultPhase();
            Class<?> transformerClass = Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer");
            Constructor<?> constructor = transformerClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot create the mixin transformer", e);
        }
    }

    private static void advanceToDefaultPhase() {
        try {
            Class<?> environmentClass = Class.forName("org.spongepowered.asm.mixin.MixinEnvironment");
            Class<?> phaseClass = Class.forName("org.spongepowered.asm.mixin.MixinEnvironment$Phase");
            Object defaultPhase = phaseClass.getField("DEFAULT").get(null);
            Method gotoPhase = environmentClass.getDeclaredMethod("gotoPhase", phaseClass);
            gotoPhase.setAccessible(true);
            gotoPhase.invoke(null, defaultPhase);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot advance mixin environment to DEFAULT phase", e);
        }
    }
}
package com.mas6y6.musmeta.plugin.mixin;

import com.mas6y6.musmeta.launch.LaunchTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

final class MixinLaunchBridge implements LaunchTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger("musmeta.plugin.mixin");

    private final Object transformer;
    private final Method transformMethod;

    MixinLaunchBridge(Object transformer) {
        this.transformer = transformer;
        try {
            this.transformMethod = transformer.getClass().getDeclaredMethod(
                    "transformClassBytes", String.class, String.class, byte[].class);
            this.transformMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Cannot find mixin transformer method", e);
        }
    }

    @Override
    public byte[] transform(String name, byte[] bytes) {
        try {
            return (byte[]) transformMethod.invoke(transformer, name, name, bytes);
        } catch (Throwable t) {
            LOGGER.error("Mixin transform failed for {}: {}", name, t.toString(), t);
            throw new RuntimeException("Mixin transform failed for " + name, t);
        }
    }
}
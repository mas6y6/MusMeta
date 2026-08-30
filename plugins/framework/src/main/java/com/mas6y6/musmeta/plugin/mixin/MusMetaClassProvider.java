package com.mas6y6.musmeta.plugin.mixin;

import org.spongepowered.asm.service.IClassProvider;

import java.net.URL;

final class MusMetaClassProvider implements IClassProvider {

    @Override
    public URL[] getClassPath() {
        return new URL[0];
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, false);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        ClassNotFoundException failure = null;
        for (ClassLoader loader : MusMetaMixinService.loaders()) {
            try {
                return Class.forName(name, initialize, loader);
            } catch (ClassNotFoundException e) {
                failure = e;
            } catch (LinkageError ignored) {
                failure = new ClassNotFoundException(name, ignored);
            }
        }
        throw failure != null ? failure : new ClassNotFoundException(name);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return findClass(name, initialize);
    }
}
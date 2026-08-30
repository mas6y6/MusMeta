package com.mas6y6.musmeta.plugin.mixin;

import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.IClassTracker;
import org.spongepowered.asm.service.IMixinAuditTrail;
import org.spongepowered.asm.service.ITransformerProvider;
import org.spongepowered.asm.service.MixinServiceAbstract;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class MusMetaMixinService extends MixinServiceAbstract {

    private final IClassProvider classProvider = new MusMetaClassProvider();
    private final IClassBytecodeProvider bytecodeProvider = new MusMetaBytecodeProvider();

    @Override
    public String getName() {
        return "MusMeta";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public IClassProvider getClassProvider() {
        return classProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return bytecodeProvider;
    }

    @Override
    public ITransformerProvider getTransformerProvider() {
        return null;
    }

    @Override
    public IClassTracker getClassTracker() {
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return Collections.emptyList();
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        return new ContainerHandleVirtual("MusMeta");
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        for (ClassLoader loader : loaders()) {
            InputStream in = loader.getResourceAsStream(name);
            if (in != null) {
                return in;
            }
        }
        return null;
    }

    static List<ClassLoader> loaders() {
        List<ClassLoader> result = new ArrayList<>();
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) {
            result.add(context);
        }
        result.add(MusMetaMixinService.class.getClassLoader());
        for (ClassLoader loader : PluginClassLoaderRegistry.pluginLoaders()) {
            if (!result.contains(loader)) {
                result.add(loader);
            }
        }
        return result;
    }
}
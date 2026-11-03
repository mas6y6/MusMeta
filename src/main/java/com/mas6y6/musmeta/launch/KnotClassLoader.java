package com.mas6y6.musmeta.launch;

import java.io.IOException;
import java.io.InputStream;

public final class KnotClassLoader extends ClassLoader {

    private static final String KNOT_PACKAGE_PREFIX = "com.mas6y6.musmeta.";
    private static final String[] KNOT_EXCLUDED_PREFIXES = {
            "com.mas6y6.musmeta.launch.",
            "com.mas6y6.musmeta.plugin.mixin."
    };

    private static volatile KnotClassLoader instance;

    private volatile LaunchTransformer transformer;

    public KnotClassLoader(ClassLoader parent) {
        super(parent);
        instance = this;
    }

    public static KnotClassLoader instance() {
        return instance;
    }

    public void installTransformer(LaunchTransformer transformer) {
        this.transformer = transformer;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                loaded = loadKnotClass(name);
            }
            if (loaded == null && isKnotScoped(name)) {
                loaded = super.loadClass(name, false);
            }
            if (loaded == null) {
                throw new ClassNotFoundException(name);
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }

    private Class<?> loadKnotClass(String name) {
        if (isKnotScoped(name)) {
            return null;
        }
        byte[] bytes = readClassBytes(name);
        if (bytes == null) {
            return null;
        }
        LaunchTransformer current = transformer;
        if (current != null) {
            bytes = current.transform(name, bytes);
        }
        return defineClass(name, bytes, 0, bytes.length);
    }

    private static boolean isKnotScoped(String name) {
        if (!name.startsWith(KNOT_PACKAGE_PREFIX)) {
            return true;
        }
        for (String prefix : KNOT_EXCLUDED_PREFIXES) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] readClassBytes(String name) {
        String resource = name.replace('.', '/') + ".class";
        try (InputStream in = ClassLoader.getSystemResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }
}
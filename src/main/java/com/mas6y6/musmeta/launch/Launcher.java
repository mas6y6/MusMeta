package com.mas6y6.musmeta.launch;

import com.mas6y6.musmeta.CrashHandler;

public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler::handle);
        KnotClassLoader knot = new KnotClassLoader(ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(knot);
        try {
            Class<?> bootstrap = Class.forName("com.mas6y6.musmeta.Bootstrap", true, knot);
            bootstrap.getMethod("main", String[].class).invoke(null, (Object) args);
        } catch (Throwable t) {
            CrashHandler.handle(Thread.currentThread(), t);
        }
    }
}
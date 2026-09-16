package com.mas6y6.musmeta.launch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LauncherTest {

    @TempDir
    Path tempDir;

    @SuppressWarnings("unchecked")
    private static List<String> buildRestartCommand(String[] args) throws Exception {
        Method method = Launcher.class.getDeclaredMethod("buildRestartCommand", String[].class);
        method.setAccessible(true);
        try {
            return (List<String>) method.invoke(null, (Object) args);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }

    @Test
    void devModeReliesOnCurrentClasspath() throws Exception {
        String previous = System.getProperty("jpackage.app-path");
        try {
            System.clearProperty("jpackage.app-path");

            List<String> command = buildRestartCommand(new String[]{"--debug"});

            assertTrue(Files.isRegularFile(Path.of(command.get(0))));

            int classpathIndex = command.indexOf("-classpath");
            assertTrue(classpathIndex >= 0, "Command should contain -classpath");
            assertEquals(
                    System.getProperty("java.class.path"),
                    command.get(classpathIndex + 1)
            );

            int mainClassIndex = command.indexOf(Launcher.class.getName());
            assertTrue(mainClassIndex > classpathIndex, "Command should contain main class");
            assertTrue(command.subList(mainClassIndex + 1, command.size()).contains("--debug"));
        } finally {
            if (previous == null) {
                System.clearProperty("jpackage.app-path");
            } else {
                System.setProperty("jpackage.app-path", previous);
            }
        }
    }
}
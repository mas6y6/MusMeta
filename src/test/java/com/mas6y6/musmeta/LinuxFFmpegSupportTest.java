package com.mas6y6.musmeta;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LinuxFFmpegSupportTest {

    @Test
    public void testGetSupportedLinuxPackageManagers() {
        List<Utils.LinuxPackageManager> managers = Utils.getSupportedLinuxPackageManagers();
        assertNotNull(managers);
        assertFalse(managers.isEmpty());

        List<String> names = managers.stream()
                .map(Utils.LinuxPackageManager::commandName)
                .toList();

        assertTrue(names.contains("apt-get"), "Should support apt-get");
        assertTrue(names.contains("apt"), "Should support apt");
        assertTrue(names.contains("dnf"), "Should support dnf");
        assertTrue(names.contains("pacman"), "Should support pacman");
        assertTrue(names.contains("zypper"), "Should support zypper");
        assertTrue(names.contains("apk"), "Should support apk");
        assertTrue(names.contains("xbps-install"), "Should support xbps-install");
        assertTrue(names.contains("eopkg"), "Should support eopkg");
        assertTrue(names.contains("yum"), "Should support yum");
        assertTrue(names.contains("emerge"), "Should support emerge");
        assertTrue(names.contains("nix-env"), "Should support nix-env");
        assertTrue(names.contains("snap"), "Should support snap");
        assertTrue(names.contains("flatpak"), "Should support flatpak");
    }

    @Test
    public void testLinuxPackageManagersInstallCommandsContainFfmpeg() {
        List<Utils.LinuxPackageManager> managers = Utils.getSupportedLinuxPackageManagers();

        for (Utils.LinuxPackageManager pm : managers) {
            assertNotNull(pm.commandName());
            assertFalse(pm.commandName().isBlank());
            assertNotNull(pm.installCommands());
            assertFalse(pm.installCommands().isEmpty());

            boolean hasFfmpeg = pm.installCommands().stream()
                    .anyMatch(cmd -> cmd.stream().anyMatch(arg -> arg.toLowerCase().contains("ffmpeg")));
            assertTrue(hasFfmpeg, "Package manager " + pm.commandName() + " must include ffmpeg in install command");
        }
    }

    @Test
    public void testIsCommandAvailableHandlesInvalidInput() {
        assertFalse(Utils.isCommandAvailable(null));
        assertFalse(Utils.isCommandAvailable(""));
        assertFalse(Utils.isCommandAvailable("   "));
        assertFalse(Utils.isCommandAvailable("definitely_non_existent_command_123456789"));
    }

    @Test
    public void testFindSystemExecutableHandlesInvalidInput() {
        assertNull(Utils.findSystemExecutable(null));
        assertNull(Utils.findSystemExecutable(""));
        assertNull(Utils.findSystemExecutable("   "));
        assertNull(Utils.findSystemExecutable("definitely_non_existent_command_123456789"));
    }

    @Test
    public void testIsRunningAsRootDoesNotThrow() {
        assertDoesNotThrow(Utils::isRunningAsRoot);
    }
}

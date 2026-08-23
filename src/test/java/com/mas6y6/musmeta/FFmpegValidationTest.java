package com.mas6y6.musmeta;

import com.mas6y6.musmeta.settings.Settings;
import com.mas6y6.musmeta.utils.FFmpegUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FFmpegValidationTest {

    @TempDir
    Path tempDir;

    @AfterEach
    public void tearDown() {
        if (Settings.FFMPEG_INSTALLATION_PATH != null) {
            Settings.FFMPEG_INSTALLATION_PATH.set("");
        }
    }

    @Test
    public void testGetFFmpegExecutableFromSettingsFile() throws IOException {
        String os = System.getProperty("os.name", "").toLowerCase();
        String execName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
        Path dummyExec = tempDir.resolve(execName);
        Files.createFile(dummyExec);

        Settings.FFMPEG_INSTALLATION_PATH.set(dummyExec.toAbsolutePath().toString());

        Path found = FFmpegUtils.getFFmpegExecutable();
        assertNotNull(found);
        assertEquals(dummyExec.toAbsolutePath(), found.toAbsolutePath());
        assertEquals(found, FFmpegUtils.getFFmpegPath());
        assertEquals(found, FFmpegUtils.findFFmpegExecutable());
    }

    @Test
    public void testGetFFmpegExecutableFromSettingsDirectory() throws IOException {
        Path binDir = tempDir.resolve("bin");
        Files.createDirectories(binDir);

        String os = System.getProperty("os.name", "").toLowerCase();
        String execName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
        Path dummyExec = binDir.resolve(execName);
        Files.createFile(dummyExec);

        Settings.FFMPEG_INSTALLATION_PATH.set(binDir.toAbsolutePath().toString());

        Path found = FFmpegUtils.getFFmpegExecutable();
        assertNotNull(found);
        assertEquals(dummyExec.toAbsolutePath(), found.toAbsolutePath());
    }

    @Test
    public void testFindFFmpegExecutableInBinDirectory() throws IOException {
        Path binDir = tempDir.resolve("bin");
        Files.createDirectories(binDir);

        String os = System.getProperty("os.name", "").toLowerCase();
        String execName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
        Path dummyExec = binDir.resolve(execName);
        Files.createFile(dummyExec);

        Path found = FFmpegUtils.findFFmpegExecutable(binDir);
        assertNotNull(found);
        assertEquals(dummyExec.toAbsolutePath(), found.toAbsolutePath());
    }

    @Test
    public void testFindFFmpegExecutableInSubBinDirectory() throws IOException {
        Path rootDir = tempDir.resolve("ffmpeg_root");
        Path subBinDir = rootDir.resolve("bin");
        Files.createDirectories(subBinDir);

        String os = System.getProperty("os.name", "").toLowerCase();
        String execName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
        Path dummyExec = subBinDir.resolve(execName);
        Files.createFile(dummyExec);

        Path found = FFmpegUtils.findFFmpegExecutable(rootDir);
        assertNotNull(found);
        assertEquals(dummyExec.toAbsolutePath(), found.toAbsolutePath());
    }

    @Test
    public void testFindFFmpegExecutableNotFound() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        Path found = FFmpegUtils.findFFmpegExecutable(emptyDir);
        assertNull(found);
    }

    @Test
    public void testFindFFmpegExecutableNullOrNonExistent() {
        assertNull(FFmpegUtils.findFFmpegExecutable(null));
        assertNull(FFmpegUtils.findFFmpegExecutable(tempDir.resolve("non_existent_folder")));
    }

    @Test
    public void testValidateFFmpegExecutableFailsOnInvalidExecutable() throws IOException {
        Path invalidExec = tempDir.resolve("fake_ffmpeg.exe");
        Files.writeString(invalidExec, "not a real binary");

        assertFalse(FFmpegUtils.validateFFmpegExecutable(invalidExec));
        assertFalse(FFmpegUtils.validateFFmpegExecutable(null));
    }
}

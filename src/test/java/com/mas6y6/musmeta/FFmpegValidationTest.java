package com.mas6y6.musmeta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class FFmpegValidationTest {

    @TempDir
    Path tempDir;

    @Test
    public void testFindFFmpegExecutableInBinDirectory() throws IOException {
        Path binDir = tempDir.resolve("bin");
        Files.createDirectories(binDir);

        String os = System.getProperty("os.name", "").toLowerCase();
        String execName = os.contains("win") ? "ffmpeg.exe" : "ffmpeg";
        Path dummyExec = binDir.resolve(execName);
        Files.createFile(dummyExec);

        Path found = Utils.findFFmpegExecutable(binDir);
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

        Path found = Utils.findFFmpegExecutable(rootDir);
        assertNotNull(found);
        assertEquals(dummyExec.toAbsolutePath(), found.toAbsolutePath());
    }

    @Test
    public void testFindFFmpegExecutableNotFound() throws IOException {
        Path emptyDir = tempDir.resolve("empty");
        Files.createDirectories(emptyDir);

        Path found = Utils.findFFmpegExecutable(emptyDir);
        assertNull(found);
    }

    @Test
    public void testFindFFmpegExecutableNullOrNonExistent() {
        assertNull(Utils.findFFmpegExecutable(null));
        assertNull(Utils.findFFmpegExecutable(tempDir.resolve("non_existent_folder")));
    }

    @Test
    public void testValidateFFmpegExecutableFailsOnInvalidExecutable() throws IOException {
        Path invalidExec = tempDir.resolve("fake_ffmpeg.exe");
        Files.writeString(invalidExec, "not a real binary");

        assertFalse(Utils.validateFFmpegExecutable(invalidExec));
        assertFalse(Utils.validateFFmpegExecutable(null));
    }
}

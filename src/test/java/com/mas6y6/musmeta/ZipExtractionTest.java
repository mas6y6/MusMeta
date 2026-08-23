package com.mas6y6.musmeta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class ZipExtractionTest {

    @TempDir
    Path tempDir;

    @Test
    void testExtractZipWithSingleRootFolderStripsRootFolder() throws IOException {
        Path zipFile = tempDir.resolve("ffmpeg-7.1-essentials_build.zip");
        Path destDir = tempDir.resolve("extracted");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            // Root dir entry
            zos.putNextEntry(new ZipEntry("ffmpeg-7.1-essentials_build/"));
            zos.closeEntry();

            // bin directory and executable inside root dir
            zos.putNextEntry(new ZipEntry("ffmpeg-7.1-essentials_build/bin/"));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("ffmpeg-7.1-essentials_build/bin/ffmpeg.exe"));
            zos.write("fake-ffmpeg-binary".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // License file inside root dir
            zos.putNextEntry(new ZipEntry("ffmpeg-7.1-essentials_build/LICENSE.txt"));
            zos.write("GPL License".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertEquals("ffmpeg-7.1-essentials_build/", Utils.getSingleRootDirectoryPrefix(zipFile));

        Utils.extractZip(zipFile, destDir);

        // Verify root folder was stripped
        assertFalse(Files.exists(destDir.resolve("ffmpeg-7.1-essentials_build")));

        // Verify contents are directly under destDir
        assertTrue(Files.exists(destDir.resolve("bin").resolve("ffmpeg.exe")));
        assertTrue(Files.exists(destDir.resolve("LICENSE.txt")));
        assertEquals("fake-ffmpeg-binary", Files.readString(destDir.resolve("bin").resolve("ffmpeg.exe")));
        assertEquals("GPL License", Files.readString(destDir.resolve("LICENSE.txt")));
    }

    @Test
    void testExtractZipWithoutSingleRootFolderExtractsDirectly() throws IOException {
        Path zipFile = tempDir.resolve("archive_without_root.zip");
        Path destDir = tempDir.resolve("extracted_no_root");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("bin/ffmpeg.exe"));
            zos.write("binary-content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("doc/manual.txt"));
            zos.write("manual-content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertNull(Utils.getSingleRootDirectoryPrefix(zipFile));

        Utils.extractZip(zipFile, destDir);

        assertTrue(Files.exists(destDir.resolve("bin").resolve("ffmpeg.exe")));
        assertTrue(Files.exists(destDir.resolve("doc").resolve("manual.txt")));
        assertEquals("binary-content", Files.readString(destDir.resolve("bin").resolve("ffmpeg.exe")));
    }

    @Test
    void testExtractZipWithSingleRootFile() throws IOException {
        Path zipFile = tempDir.resolve("single_file.zip");
        Path destDir = tempDir.resolve("extracted_single_file");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("ffmpeg"));
            zos.write("mac-ffmpeg-binary".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertNull(Utils.getSingleRootDirectoryPrefix(zipFile));

        Utils.extractZip(zipFile, destDir);

        assertTrue(Files.exists(destDir.resolve("ffmpeg")));
        assertEquals("mac-ffmpeg-binary", Files.readString(destDir.resolve("ffmpeg")));
    }

    @Test
    void testExtractZipPreventsPathTraversal() throws IOException {
        Path zipFile = tempDir.resolve("malicious.zip");
        Path destDir = tempDir.resolve("extracted_safe");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("../outside.txt"));
            zos.write("evil".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertThrows(IOException.class, () -> Utils.extractZip(zipFile, destDir));
    }

    @Test
    void testExtractZipPreventsNestedPathTraversal() throws IOException {
        Path zipFile = tempDir.resolve("nested_malicious.zip");
        Path destDir = tempDir.resolve("extracted_nested_safe");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("ffmpeg-7.1/../../outside.txt"));
            zos.write("evil".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertThrows(IOException.class, () -> Utils.extractZip(zipFile, destDir));
    }

    @Test
    void testExtractZipWithWindowsBackslashesInEntries() throws IOException {
        Path zipFile = tempDir.resolve("windows_paths.zip");
        Path destDir = tempDir.resolve("extracted_windows");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry("ffmpeg-root\\bin\\ffmpeg.exe"));
            zos.write("win-binary".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertEquals("ffmpeg-root/", Utils.getSingleRootDirectoryPrefix(zipFile));

        Utils.extractZip(zipFile, destDir);

        assertTrue(Files.exists(destDir.resolve("bin").resolve("ffmpeg.exe")));
        assertEquals("win-binary", Files.readString(destDir.resolve("bin").resolve("ffmpeg.exe")));
    }
}

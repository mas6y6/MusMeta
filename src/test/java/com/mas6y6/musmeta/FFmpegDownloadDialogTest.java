package com.mas6y6.musmeta;

import com.mas6y6.musmeta.ui.dialogs.FFmpegDownloadDialog;
import com.mas6y6.musmeta.utils.FFmpegUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class FFmpegDownloadDialogTest {

    @TempDir
    Path tempDir;

    @Test
    public void testDialogInitializationAndComponents() {
        JFrame parentFrame = new JFrame();
        FFmpegDownloadDialog dialog = new FFmpegDownloadDialog(parentFrame, tempDir);

        assertNotNull(dialog.getProgressBar());
        assertTrue(dialog.getProgressBar().isIndeterminate());
        assertNotNull(dialog.getStatusLabel());
        assertNotNull(dialog.getDetailLabel());
        assertNotNull(dialog.getActionButton());
        assertEquals("Cancel", dialog.getActionButton().getText());
        assertFalse(dialog.isSuccess());
        assertFalse(dialog.isCancelled());

        dialog.dispose();
        parentFrame.dispose();
    }

    @Test
    public void testUpdateProgressDeterminateAndIndeterminate() {
        JFrame parentFrame = new JFrame();
        FFmpegDownloadDialog dialog = new FFmpegDownloadDialog(parentFrame, tempDir);

        dialog.updateProgress("Downloading FFmpeg...", 45, "15.0 MB / 33.0 MB (45%)");
        assertEquals("Downloading FFmpeg...", dialog.getStatusLabel().getText());
        assertEquals("15.0 MB / 33.0 MB (45%)", dialog.getDetailLabel().getText());
        assertFalse(dialog.getProgressBar().isIndeterminate());
        assertEquals(45, dialog.getProgressBar().getValue());
        assertTrue(dialog.getProgressBar().isStringPainted());
        assertEquals("45%", dialog.getProgressBar().getString());

        dialog.updateProgress("Extracting FFmpeg...", -1, "");
        assertEquals("Extracting FFmpeg...", dialog.getStatusLabel().getText());
        assertTrue(dialog.getProgressBar().isIndeterminate());
        assertFalse(dialog.getProgressBar().isStringPainted());

        dialog.dispose();
        parentFrame.dispose();
    }

    @Test
    public void testInstallProgressListenerCallback() {
        AtomicReference<String> lastStatus = new AtomicReference<>();
        AtomicInteger lastPercent = new AtomicInteger(-1);
        AtomicReference<String> lastDetails = new AtomicReference<>();
        AtomicBoolean errorCalled = new AtomicBoolean(false);

        FFmpegUtils.InstallProgressListener listener = new FFmpegUtils.InstallProgressListener() {
            @Override
            public void onProgress(String status, int percentage, String details) {
                lastStatus.set(status);
                lastPercent.set(percentage);
                lastDetails.set(details);
            }

            @Override
            public void onError(String errorMessage, Throwable throwable) {
                errorCalled.set(true);
            }
        };

        listener.onProgress("Testing status", 50, "detail info");
        assertEquals("Testing status", lastStatus.get());
        assertEquals(50, lastPercent.get());
        assertEquals("detail info", lastDetails.get());

        listener.onError("test error", new RuntimeException());
        assertTrue(errorCalled.get());
    }
}

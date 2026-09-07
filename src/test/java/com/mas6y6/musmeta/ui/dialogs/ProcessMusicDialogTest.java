package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.core.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProcessMusicDialogTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        Library.getInstance().clear();
    }

    @Test
    void testProcessWithNullOrEmptyFiles() {
        ProcessMusicDialog dialogNull = new ProcessMusicDialog(null, null);
        assertNotNull(dialogNull.getSongsWithoutTags());
        assertNotNull(dialogNull.getProcessedSongs());
        assertEquals(0, dialogNull.getSongsWithoutTags().size());
        assertEquals(0, dialogNull.getProcessedSongs().size());

        ProcessMusicDialog dialogEmpty = new ProcessMusicDialog(null, new File[0]);
        assertEquals(0, dialogEmpty.getSongsWithoutTags().size());
        assertEquals(0, dialogEmpty.getProcessedSongs().size());
    }

    @Test
    void testProcessWithNonExistentAndNonAudioFiles() throws IOException {
        File nonExistent = tempDir.resolve("missing.mp3").toFile();
        File textFile = tempDir.resolve("notes.txt").toFile();
        try (FileOutputStream fos = new FileOutputStream(textFile)) {
            fos.write("some text".getBytes());
        }

        ProcessMusicDialog dialog = new ProcessMusicDialog(null, new File[]{nonExistent, textFile});
        assertDoesNotThrow(dialog::process);
        assertEquals(0, dialog.getProcessedSongs().size());
    }

    @Test
    void testStartAndShowInHeadless() {
        ProcessMusicDialog dialog = new ProcessMusicDialog(null, new File[0]);
        boolean result = dialog.startAndShow();
        assertTrue(result);
        assertTrue(dialog.isSuccess());
        assertFalse(dialog.isProcessing());
    }
}

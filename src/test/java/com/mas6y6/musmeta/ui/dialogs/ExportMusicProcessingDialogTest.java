package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.core.Song;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class ExportMusicProcessingDialogTest {

    @TempDir
    Path tempDir;

    private File createDummyMp3(String name) throws IOException {
        File file = tempDir.resolve(name).toFile();
        // A minimal valid MPEG-1 Layer 3 frame, enough for the file to have
        // the shape of an mp3 on disk.
        int frameSize = 417; // 144 * 128000 / 44100
        byte[] frame = new byte[frameSize * 2];
        for (int f = 0; f < 2; f++) {
            int offset = f * frameSize;
            frame[offset] = (byte) 0xFF;
            frame[offset + 1] = (byte) 0xFB;
            frame[offset + 2] = (byte) 0x90;
            frame[offset + 3] = (byte) 0x64;
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(frame);
        }
        return file;
    }

    private Song song(String fileName, String title) throws Exception {
        ID3v24Tag tag = new ID3v24Tag();
        if (title != null) {
            tag.setField(FieldKey.TITLE, title);
        }

        return new Song(
                new AudioFile(
                        createDummyMp3(fileName),
                        new GenericAudioHeader(),
                        title != null ? tag : null
                )
        );
    }

    private List<String> exportAndListEntries(
            List<Song> songs,
            String chosenName
    ) throws Exception {
        File chosen = tempDir.resolve(chosenName).toFile();

        ExportMusicProcessingDialog dialog =
                new ExportMusicProcessingDialog(null, songs, chosen);

        assertTrue(dialog.process());

        List<String> names = new ArrayList<>();

        try (ZipFile zip = new ZipFile(resolveOutput(chosen))) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                names.add(entries.nextElement().getName());
            }
        }

        return names;
    }

    /**
     * The dialog decides for itself what the file is called, so the test asks
     * the same way the user does: by looking for the only zip that was written.
     */
    private File resolveOutput(File chosen) {
        String name = chosen.getName();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;

        return tempDir.resolve(base + ".zip").toFile();
    }

    @Test
    void testEveryExportedSongKeepsItsExtension() throws Exception {
        List<Song> songs = List.of(
                song("first.mp3", "First"),
                song("second.flac", "Second")
        );

        List<String> entries = exportAndListEntries(songs, "album");

        assertEquals(2, entries.size());
        assertTrue(entries.get(0).endsWith(".mp3"), entries.get(0));
        assertTrue(entries.get(1).endsWith(".flac"), entries.get(1));
    }

    @Test
    void testExportedSongsAreNumberedInOrder() throws Exception {
        List<Song> songs = List.of(
                song("a.mp3", "Alpha"),
                song("b.mp3", "Beta"),
                song("c.mp3", "Gamma")
        );

        List<String> entries = exportAndListEntries(songs, "album");

        assertEquals(3, entries.size());
        assertTrue(entries.get(0).startsWith("1 - "), entries.get(0));
        assertTrue(entries.get(1).startsWith("2 - "), entries.get(1));
        assertTrue(entries.get(2).startsWith("3 - "), entries.get(2));
        assertTrue(entries.get(0).contains("Alpha"), entries.get(0));
        assertTrue(entries.get(2).contains("Gamma"), entries.get(2));
    }

    @Test
    void testAnUntaggedSongDoesNotEndUpWithTwoExtensions() throws Exception {
        // An untagged song is named after its file, extension and all.
        Song song = song("Track01.mp3", null);

        assertEquals("Track01.mp3", song.getTitle());

        List<String> entries = exportAndListEntries(List.of(song), "album");

        assertEquals(1, entries.size());
        assertTrue(entries.get(0).endsWith("Track01.mp3"), entries.get(0));
        assertFalse(entries.get(0).endsWith(".mp3.mp3"), entries.get(0));
    }

    @Test
    void testANameThatAlreadyEndsInZipIsNotGivenAnotherOne() throws Exception {
        List<Song> songs = List.of(song("a.mp3", "Alpha"));

        ExportMusicProcessingDialog dialog = new ExportMusicProcessingDialog(
                null,
                songs,
                tempDir.resolve("album.ZIP").toFile()
        );

        assertTrue(dialog.process());

        assertTrue(Files.exists(tempDir.resolve("album.ZIP")));
    }

    @Test
    void testANameWithADotThatIsNotAnExtensionKeepsTheWholeName()
            throws Exception {
        List<Song> songs = List.of(song("a.mp3", "Alpha"));

        ExportMusicProcessingDialog dialog = new ExportMusicProcessingDialog(
                null,
                songs,
                tempDir.resolve("Mr. Blue Sky").toFile()
        );

        assertTrue(dialog.process());

        assertTrue(Files.exists(tempDir.resolve("Mr. Blue Sky.zip")));
    }

    @Test
    void testAWrongExtensionIsReplacedRatherThanStackedOn() throws Exception {
        List<Song> songs = List.of(song("a.mp3", "Alpha"));

        ExportMusicProcessingDialog dialog = new ExportMusicProcessingDialog(
                null,
                songs,
                tempDir.resolve("album.mp3").toFile()
        );

        assertTrue(dialog.process());

        assertTrue(Files.exists(tempDir.resolve("album.zip")));
        assertFalse(Files.exists(tempDir.resolve("album.mp3.zip")));
    }
}

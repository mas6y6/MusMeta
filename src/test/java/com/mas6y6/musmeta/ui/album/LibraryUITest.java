package com.mas6y6.musmeta.ui.album;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Library;
import com.mas6y6.musmeta.core.Song;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LibraryUITest {

    @TempDir
    Path tempDir;

    private File createDummyMp3(String name) throws Exception {
        File file = tempDir.resolve(name).toFile();
        int frameSize = 417;
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

    @BeforeEach
    void setUp() {
        Library.getInstance().clear();
    }

    private List<AlbumUI> getDisplayedAlbumCards(LibraryUI libraryUI) {
        JPanel content = (JPanel) libraryUI.getViewport().getView();
        List<AlbumUI> albumCards = new ArrayList<>();
        for (Component component : content.getComponents()) {
            if (component instanceof AlbumUI albumUI) {
                albumCards.add(albumUI);
            }
        }
        return albumCards;
    }

    @Test
    void testEmptyUnknownAlbumDoesNotAppearInLibraryUI() {
        Album unknownAlbum = new Album("Unknown");
        assertTrue(unknownAlbum.isUnknown());
        assertTrue(unknownAlbum.getSongs().isEmpty());

        Library.getInstance().registerAlbum(unknownAlbum);

        LibraryUI libraryUI = new LibraryUI();
        List<AlbumUI> albumCards = getDisplayedAlbumCards(libraryUI);

        assertTrue(albumCards.isEmpty(), "Empty unknown album should not appear in the library UI");
    }

    @Test
    void testNonEmptyUnknownAlbumAppearsInLibraryUI() throws Exception {
        File dummyFile = createDummyMp3("unknown_track.mp3");
        AudioFile audioFile = new AudioFile(dummyFile, new GenericAudioHeader(), null);
        Song song = new Song(audioFile);

        Library library = Library.getInstance();
        library.addSong(song);

        Album unknownAlbum = library.getAlbum("Unknown");
        assertNotNull(unknownAlbum);
        assertTrue(unknownAlbum.isUnknown());
        assertFalse(unknownAlbum.getSongs().isEmpty());

        LibraryUI libraryUI = new LibraryUI();
        List<AlbumUI> albumCards = getDisplayedAlbumCards(libraryUI);

        assertEquals(1, albumCards.size(), "Non-empty unknown album should appear in the library UI");
        assertEquals("Unknown", albumCards.get(0).getAlbum().getTitle());
    }

    @Test
    void testEmptyRegularAlbumAppearsInLibraryUI() {
        Album regularAlbum = new Album("Favorite Album");
        assertFalse(regularAlbum.isUnknown());
        assertTrue(regularAlbum.getSongs().isEmpty());

        Library.getInstance().registerAlbum(regularAlbum);

        LibraryUI libraryUI = new LibraryUI();
        List<AlbumUI> albumCards = getDisplayedAlbumCards(libraryUI);

        assertEquals(1, albumCards.size(), "Empty regular album should still appear in the library UI");
        assertEquals("Favorite Album", albumCards.get(0).getAlbum().getTitle());
    }
}

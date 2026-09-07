package com.mas6y6.musmeta.core;

import com.mas6y6.musmeta.ui.dialogs.ProcessTagsDialog;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class EditAlbumAndSongTest {
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
    void setup() {
        Library.getInstance().clear();
    }

    @Test
    void testSongMetadataGettersAndSetters() throws Exception {
        File file = createDummyMp3("song1.mp3");
        AudioFile audioFile = AudioFileIO.read(file);
        Song song = new Song(audioFile);

        song.setTagField(FieldKey.TITLE, "Track 1");
        song.setTagField(FieldKey.ARTIST, "Artist 1");
        song.setTagField(FieldKey.ALBUM, "Album 1");
        song.setTagField(FieldKey.ALBUM_ARTIST, "Album Artist 1");
        song.setTagField(FieldKey.GENRE, "Electronic");
        song.setTagField(FieldKey.YEAR, "2023");
        song.setCompilation(true);
        song.setTagField(FieldKey.COMPOSER, "Composer 1");
        song.setTagField(FieldKey.GROUPING, "Group 1");
        song.setTagField(FieldKey.RATING, "80");
        song.setTagField(FieldKey.BPM, "128");
        song.setTagField(FieldKey.COMMENT, "Test comment");
        song.setTagField(FieldKey.TRACK, "1");
        song.setTagField(FieldKey.TRACK_TOTAL, "10");
        song.setTagField(FieldKey.DISC_NO, "1");
        song.setTagField(FieldKey.DISC_TOTAL, "2");

        byte[] dummyPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        song.setArtwork(dummyPng, "image/png");

        song.saveToFile();

        AudioFile reloaded = AudioFileIO.read(file);
        Song loadedSong = new Song(reloaded);

        assertEquals("Track 1", loadedSong.getTitle());
        assertEquals("Artist 1", loadedSong.getArtist());
        assertEquals("Album 1", loadedSong.getAlbum());
        assertEquals("Album Artist 1", loadedSong.getAlbumArtist());
        assertEquals("Electronic", loadedSong.getGenre());
        assertEquals("2023", loadedSong.getYear());
        assertTrue(loadedSong.isCompilation());
        assertEquals("Composer 1", loadedSong.getComposer());
        assertEquals("Group 1", loadedSong.getGrouping());
        assertEquals("80", loadedSong.getRating());
        assertEquals("128", loadedSong.getBpm());
        assertEquals("Test comment", loadedSong.getComment());
        assertEquals(1, loadedSong.getTrackNumber());
        assertEquals(10, loadedSong.getTrackTotal());
        assertEquals(1, loadedSong.getDiscNumber());
        assertEquals(2, loadedSong.getDiscTotal());
        assertNotNull(loadedSong.getArtworkData());
    }

    @Test
    void testAlbumMetadataResolution() throws Exception {
        File f1 = createDummyMp3("a1.mp3");
        File f2 = createDummyMp3("a2.mp3");

        AudioFile af1 = AudioFileIO.read(f1);
        AudioFile af2 = AudioFileIO.read(f2);

        Song s1 = new Song(af1);
        Song s2 = new Song(af2);

        s1.setTagField(FieldKey.TITLE, "Song 1");
        s1.setTagField(FieldKey.ALBUM, "My Masterpiece");
        s1.setTagField(FieldKey.ARTIST, "Artist A");
        s1.setTagField(FieldKey.GENRE, "Rock");
        s1.setTagField(FieldKey.YEAR, "2021");
        s1.setTagField(FieldKey.COMPOSER, "Composer X");
        s1.setTagField(FieldKey.GROUPING, "Box Set");
        s1.setTagField(FieldKey.RATING, "100");
        s1.setTagField(FieldKey.BPM, "140");
        s1.setTagField(FieldKey.COMMENT, "Remastered");
        s1.setTagField(FieldKey.TRACK, "1");
        s1.setTagField(FieldKey.TRACK_TOTAL, "2");
        s1.setTagField(FieldKey.DISC_NO, "1");
        s1.setTagField(FieldKey.DISC_TOTAL, "2");

        s2.setTagField(FieldKey.TITLE, "Song 2");
        s2.setTagField(FieldKey.ALBUM, "My Masterpiece");
        s2.setTagField(FieldKey.ARTIST, "Artist B");
        s2.setTagField(FieldKey.TRACK, "2");
        s2.setTagField(FieldKey.TRACK_TOTAL, "2");
        s2.setTagField(FieldKey.DISC_NO, "2");
        s2.setTagField(FieldKey.DISC_TOTAL, "2");

        Album album = new Album("My Masterpiece");
        album.addSong(s1);
        album.addSong(s2);

        assertEquals("Rock", album.getGenre());
        assertEquals("2021", album.getYear());
        assertEquals("Composer X", album.getComposer());
        assertEquals("Box Set", album.getGrouping());
        assertEquals("100", album.getRating());
        assertEquals("140", album.getBpm());
        assertEquals("Remastered", album.getComment());
        assertTrue(album.isCompilation());
        assertEquals(2, album.getDiscTotal());
        assertEquals(2, album.getTrackTotal());
    }

    @Test
    void testProcessTagsForAlbum() throws Exception {
        File f1 = createDummyMp3("alb1.mp3");
        File f2 = createDummyMp3("alb2.mp3");

        Song s1 = new Song(AudioFileIO.read(f1));
        Song s2 = new Song(AudioFileIO.read(f2));

        s1.setTagField(FieldKey.TITLE, "Track A");
        s1.setTagField(FieldKey.ALBUM, "Old Album");
        s2.setTagField(FieldKey.TITLE, "Track B");
        s2.setTagField(FieldKey.ALBUM, "Old Album");

        Library library = Library.getInstance();
        library.addSong(s1);
        library.addSong(s2);

        Album album = library.getAlbums().get(0);
        assertEquals("Old Album", album.getTitle());

        Map<FieldKey, String> toSet = new HashMap<>();
        toSet.put(FieldKey.ALBUM, "New Album Title");
        toSet.put(FieldKey.ALBUM_ARTIST, "Various Artists");
        toSet.put(FieldKey.GENRE, "Jazz");
        toSet.put(FieldKey.YEAR, "2025");
        toSet.put(FieldKey.COMPOSER, "Miles Davis");
        toSet.put(FieldKey.GROUPING, "Live Session");
        toSet.put(FieldKey.RATING, "80");
        toSet.put(FieldKey.BPM, "110");
        toSet.put(FieldKey.COMMENT, "Recorded live in Tokyo");
        toSet.put(FieldKey.DISC_TOTAL, "1");
        toSet.put(FieldKey.TRACK_TOTAL, "2");

        album.setTitle("New Album Title");

        List<ProcessTagsDialog.SongTagUpdate> updates = List.of(
                new ProcessTagsDialog.SongTagUpdate(s1, toSet, Set.of(), true, ProcessTagsDialog.ArtworkAction.KEEP, null, null),
                new ProcessTagsDialog.SongTagUpdate(s2, toSet, Set.of(), true, ProcessTagsDialog.ArtworkAction.KEEP, null, null)
        );

        for (ProcessTagsDialog.SongTagUpdate update : updates) {
            Song s = update.song();
            for (Map.Entry<FieldKey, String> entry : update.fieldsToSet().entrySet()) {
                s.setTagField(entry.getKey(), entry.getValue());
            }
            s.setCompilation(update.compilation());
            s.saveToFile();
        }

        library.renameAlbum(album, "New Album Title");
        for (ProcessTagsDialog.SongTagUpdate update : updates) {
            library.reorganizeSong(update.song());
        }

        AudioFile reloaded1 = AudioFileIO.read(f1);
        assertEquals("New Album Title", reloaded1.getTag().getFirst(FieldKey.ALBUM));
        assertEquals("Jazz", reloaded1.getTag().getFirst(FieldKey.GENRE));
        assertEquals("2025", reloaded1.getTag().getFirst(FieldKey.YEAR));
        assertEquals("Miles Davis", reloaded1.getTag().getFirst(FieldKey.COMPOSER));
        assertEquals("1", reloaded1.getTag().getFirst(FieldKey.IS_COMPILATION));

        assertTrue(library.containsAlbum("New Album Title"));
        assertFalse(library.containsAlbum("Old Album"));
    }

    @Test
    void testProcessTagsForMultiSelectedSongs() throws Exception {
        File f1 = createDummyMp3("multi1.mp3");
        File f2 = createDummyMp3("multi2.mp3");

        Song s1 = new Song(AudioFileIO.read(f1));
        Song s2 = new Song(AudioFileIO.read(f2));

        s1.setTagField(FieldKey.TITLE, "Song Alpha");
        s1.setTagField(FieldKey.ARTIST, "Artist Alpha");
        s1.setTagField(FieldKey.ALBUM, "Compilation Album");

        s2.setTagField(FieldKey.TITLE, "Song Beta");
        s2.setTagField(FieldKey.ARTIST, "Artist Beta");
        s2.setTagField(FieldKey.ALBUM, "Compilation Album");

        Library.getInstance().addSong(s1);
        Library.getInstance().addSong(s2);

        Map<FieldKey, String> sharedFields = new HashMap<>();
        sharedFields.put(FieldKey.GENRE, "Soundtrack");
        sharedFields.put(FieldKey.YEAR, "2024");
        sharedFields.put(FieldKey.COMPOSER, "John Williams");

        List<ProcessTagsDialog.SongTagUpdate> updates = List.of(
                new ProcessTagsDialog.SongTagUpdate(s1, sharedFields, Set.of(), null, ProcessTagsDialog.ArtworkAction.KEEP, null, null),
                new ProcessTagsDialog.SongTagUpdate(s2, sharedFields, Set.of(), null, ProcessTagsDialog.ArtworkAction.KEEP, null, null)
        );

        for (ProcessTagsDialog.SongTagUpdate u : updates) {
            Song s = u.song();
            for (Map.Entry<FieldKey, String> entry : u.fieldsToSet().entrySet()) {
                s.setTagField(entry.getKey(), entry.getValue());
            }
            s.saveToFile();
        }

        AudioFile r1 = AudioFileIO.read(f1);
        AudioFile r2 = AudioFileIO.read(f2);

        assertEquals("Song Alpha", r1.getTag().getFirst(FieldKey.TITLE));
        assertEquals("Artist Alpha", r1.getTag().getFirst(FieldKey.ARTIST));
        assertEquals("Soundtrack", r1.getTag().getFirst(FieldKey.GENRE));
        assertEquals("2024", r1.getTag().getFirst(FieldKey.YEAR));
        assertEquals("John Williams", r1.getTag().getFirst(FieldKey.COMPOSER));

        assertEquals("Song Beta", r2.getTag().getFirst(FieldKey.TITLE));
        assertEquals("Artist Beta", r2.getTag().getFirst(FieldKey.ARTIST));
        assertEquals("Soundtrack", r2.getTag().getFirst(FieldKey.GENRE));
        assertEquals("2024", r2.getTag().getFirst(FieldKey.YEAR));
        assertEquals("John Williams", r2.getTag().getFirst(FieldKey.COMPOSER));
    }

    @Test
    void testArtworkRemovalAndReplacement() throws Exception {
        File file = createDummyMp3("art_ops.mp3");
        Song song = new Song(AudioFileIO.read(file));

        byte[] initialArt = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        song.setArtwork(initialArt, "image/png");
        song.saveToFile();

        Song loadedWithArt = new Song(AudioFileIO.read(file));
        assertNotNull(loadedWithArt.getArtworkData());

        // Delete artwork
        song.deleteArtwork();
        song.saveToFile();

        Song loadedWithoutArt = new Song(AudioFileIO.read(file));
        assertNull(loadedWithoutArt.getArtworkData());

        // Replace artwork
        byte[] newArt = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        song.setArtwork(newArt, "image/jpeg");
        song.saveToFile();

        Song loadedWithNewArt = new Song(AudioFileIO.read(file));
        assertNotNull(loadedWithNewArt.getArtworkData());
        assertEquals(4, loadedWithNewArt.getArtworkData().length);
    }
}

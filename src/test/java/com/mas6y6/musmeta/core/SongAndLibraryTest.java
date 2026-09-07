package com.mas6y6.musmeta.core;

import com.mas6y6.musmeta.config.ConfigBuilder;
import com.mas6y6.musmeta.config.ConfigManager;
import com.mas6y6.musmeta.settings.ConfigCodecs;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.generic.GenericAudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SongAndLibraryTest {

    @TempDir
    Path tempDir;

    private File createDummyMp3(String name) throws IOException {
        File file = tempDir.resolve(name).toFile();
        // Generate a minimal valid MPEG-1 Layer 3 frame
        // 0xFF, 0xFB (sync word + MPEG 1 + Layer 3 + no protection)
        // 0x90 (128 kbps, 44100 Hz, no padding, private)
        // 0x64 (joint stereo, mode extension 01, not copyrighted, original, no emphasis)
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

    @BeforeEach
    void setUp() {
        Library.getInstance().clear();
    }

    @Test
    void testUntaggedSongDefaultsToUnknownAlbum() throws Exception {
        File dummyFile = createDummyMp3("sample_untagged.mp3");
        AudioFile audioFile = new AudioFile(dummyFile, new GenericAudioHeader(), null);

        Song song = new Song(audioFile);
        assertNull(song.getTag());
        assertFalse(song.hasTag());
        assertFalse(song.hasBasicTags());
        assertEquals("Unknown", song.getAlbum());
        assertEquals("Unknown Artist", song.getArtist());
        assertEquals("sample_untagged.mp3", song.getTitle());

        Library library = Library.getInstance();
        library.addSong(song);

        assertEquals(1, library.getAlbums().size());
        Album album = library.getAlbums().get(0);
        assertEquals("Unknown", album.getTitle());
        assertTrue(album.isUnknown());
        assertEquals(1, album.getSongs().size());
        assertEquals(song, album.getSongs().get(0));
    }

    @Test
    void testIncompleteTaggedSongDefaultsToUnknownAlbum() throws Exception {
        File dummyFile = createDummyMp3("sample_incomplete.mp3");
        ID3v24Tag tag = new ID3v24Tag();
        // Tag with title but without album
        tag.setField(FieldKey.TITLE, "Test Song");
        AudioFile audioFile = new AudioFile(dummyFile, new GenericAudioHeader(), tag);

        Song song = new Song(audioFile);
        assertTrue(song.hasTag());
        assertFalse(song.hasBasicTags());
        assertEquals("Unknown", song.getAlbum());
        assertEquals("Test Song", song.getTitle());

        Library library = Library.getInstance();
        library.addSong(song);

        assertEquals(1, library.getAlbums().size());
        Album album = library.getAlbums().get(0);
        assertEquals("Unknown", album.getTitle());
        assertTrue(album.isUnknown());
        assertEquals(1, album.getSongs().size());
        assertEquals(song, album.getSongs().get(0));
    }

    @Test
    void testCompleteTaggedSongPreservesAlbum() throws Exception {
        File dummyFile = createDummyMp3("sample_complete.mp3");
        ID3v24Tag tag = new ID3v24Tag();
        tag.setField(FieldKey.TITLE, "My Song");
        tag.setField(FieldKey.ALBUM, "My Album");
        AudioFile audioFile = new AudioFile(dummyFile, new GenericAudioHeader(), tag);

        Song song = new Song(audioFile);
        assertTrue(song.hasTag());
        assertTrue(song.hasBasicTags());
        assertEquals("My Album", song.getAlbum());
        assertEquals("My Song", song.getTitle());

        Library library = Library.getInstance();
        library.addSong(song);

        assertEquals(1, library.getAlbums().size());
        Album album = library.getAlbums().get(0);
        assertEquals("My Album", album.getTitle());
        assertFalse(album.isUnknown());
        assertEquals(1, album.getSongs().size());
        assertEquals(song, album.getSongs().get(0));
    }

    @Test
    void testAlbumUnknownInitialization() {
        Album unknownAlbum = new Album("Unknown");
        assertTrue(unknownAlbum.isUnknown());

        Album regularAlbum = new Album("Abbey Road");
        assertFalse(regularAlbum.isUnknown());
    }

    @Test
    void testLibrarySerializationPreservesUnknownFlag() {
        Library library = Library.getInstance();
        Album unknown = new Album("Unknown");
        unknown.setUnknown(true);
        library.registerAlbum(unknown);

        Album regular = new Album("My Album");
        library.registerAlbum(regular);

        library.save();

        Library loaded = Library.load();
        assertTrue(loaded.containsAlbum("Unknown"));
        assertTrue(loaded.getAlbumsByTitle().get("Unknown").isUnknown());

        assertTrue(loaded.containsAlbum("My Album"));
        assertFalse(loaded.getAlbumsByTitle().get("My Album").isUnknown());
    }

    @Test
    void testSongCodec() throws Exception {
        File dummyFile = createDummyMp3("codec_song.mp3");
        AudioFile audioFile = new AudioFile(dummyFile, new GenericAudioHeader(), null);
        Song song = new Song(audioFile);

        ConfigBuilder builder = new ConfigBuilder();
        Song.CODEC.encode(song, builder);
        assertEquals(dummyFile.getAbsolutePath(), builder.getString("path"));

        Song decoded = Song.CODEC.decode(builder);
        assertNotNull(decoded);
        assertEquals(dummyFile.getAbsolutePath(), decoded.getAudioFile().getFile().getAbsolutePath());

        // Test non-existent file path decoding returns null
        ConfigBuilder invalidBuilder = new ConfigBuilder();
        invalidBuilder.setString("path", tempDir.resolve("non_existent.mp3").toString());
        assertNull(Song.CODEC.decode(invalidBuilder));

        // Test null/empty builder
        assertNull(Song.CODEC.decode(null));
        assertNull(Song.CODEC.decode(new ConfigBuilder()));
    }

    @Test
    void testDiscCodec() throws Exception {
        File dummyFile = createDummyMp3("disc_song.mp3");
        AudioFile audioFile = new AudioFile(dummyFile, new GenericAudioHeader(), null);
        Song song = new Song(audioFile);

        Disc disc = new Disc(2, 3);
        disc.addSong(song);

        ConfigBuilder builder = new ConfigBuilder();
        Disc.CODEC.encode(disc, builder);

        assertEquals(2, builder.getInt("index"));
        assertEquals(3, builder.getInt("total"));
        assertTrue(builder.has("songs"));

        Disc decoded = Disc.CODEC.decode(builder);
        assertNotNull(decoded);
        assertEquals(2, decoded.getDiscIndex());
        assertEquals(3, decoded.getDiscTotal());
        assertEquals(1, decoded.getSongs().size());
        assertEquals(dummyFile.getAbsolutePath(), decoded.getSongs().get(0).getAudioFile().getFile().getAbsolutePath());

        // Test disc codec with missing song path
        ConfigBuilder missingSongDiscBuilder = new ConfigBuilder();
        missingSongDiscBuilder.setInt("index", 1);
        missingSongDiscBuilder.setInt("total", 1);
        com.google.gson.JsonArray songsArray = new com.google.gson.JsonArray();
        com.google.gson.JsonObject missingSongObj = new com.google.gson.JsonObject();
        missingSongObj.addProperty("path", tempDir.resolve("missing_in_disc.mp3").toString());
        songsArray.add(missingSongObj);
        missingSongDiscBuilder.set("songs", songsArray);

        Disc decodedWithMissing = Disc.CODEC.decode(missingSongDiscBuilder);
        assertNotNull(decodedWithMissing);
        assertEquals(0, decodedWithMissing.getSongs().size());
        assertEquals(1, decodedWithMissing.getMissingSongPaths().size());
        assertEquals(tempDir.resolve("missing_in_disc.mp3").toString(), decodedWithMissing.getMissingSongPaths().get(0));
    }

    @Test
    void testAlbumCodec() throws Exception {
        File dummyFile = createDummyMp3("album_song.mp3");
        AudioFile audioFile = new AudioFile(dummyFile, new GenericAudioHeader(), null);
        Song song = new Song(audioFile);

        Path artPath = tempDir.resolve("art.png");
        long createdAt = 1234567890L;
        Album album = new Album("Test Album", artPath, createdAt);
        album.addSong(song);

        ConfigBuilder builder = new ConfigBuilder();
        Album.CODEC.encode(album, builder);

        assertEquals("Test Album", builder.getString("title"));
        assertEquals(artPath.toString(), builder.getString("artworkPath"));
        assertEquals(createdAt, builder.getLong("createdAt"));
        assertTrue(builder.has("discs"));

        Album decoded = Album.CODEC.decode(builder);
        assertNotNull(decoded);
        assertEquals("Test Album", decoded.getTitle());
        assertEquals(artPath, decoded.getArtworkPath());
        assertEquals(createdAt, decoded.getCreatedAt());
        assertFalse(decoded.isUnknown());
        assertEquals(1, decoded.getDiscs().size());
        assertEquals(1, decoded.getSongs().size());
    }

    @Test
    void testLibraryCodecHandlesMissingSongs() throws Exception {
        File dummyFile = createDummyMp3("valid_song.mp3");
        AudioFile audioFile = new AudioFile(dummyFile, new GenericAudioHeader(), null);
        Song song = new Song(audioFile);

        Library library = Library.getInstance();
        library.addSong(song);

        Album album = library.getAlbums().get(0);
        Disc disc = album.getDiscs().get(0);
        disc.addMissingSongPath(tempDir.resolve("non_existent_library_song.mp3").toString());

        ConfigBuilder builder = new ConfigBuilder();
        Library.CODEC.encode(library, builder);

        Library decoded = Library.CODEC.decode(builder);
        assertNotNull(decoded);
        assertEquals(1, decoded.getAlbums().size());
        assertEquals(1, decoded.getSongs().size());
        assertEquals(1, decoded.getMissingSongs().size());
        Library.MissingSong missingSong = decoded.getMissingSongs().get(0);
        assertEquals(tempDir.resolve("non_existent_library_song.mp3").toString(), missingSong.path());
        assertEquals(album.getTitle(), missingSong.albumTitle());
        assertEquals(disc.getDiscIndex(), missingSong.discIndex());
    }

    @Test
    void testConfigCodecsRegistration() {
        ConfigCodecs.register();
        assertNotNull(ConfigManager.getCodec(Song.class));
        assertNotNull(ConfigManager.getCodec(Disc.class));
        assertNotNull(ConfigManager.getCodec(Album.class));
        assertNotNull(ConfigManager.getCodec(Library.class));
    }
}

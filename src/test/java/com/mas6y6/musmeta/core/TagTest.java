package com.mas6y6.musmeta.core;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.mp3.MP3File;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TagTest {
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

    @Test
    void testFieldKeys() {
        assertNotNull(FieldKey.ALBUM);
        assertNotNull(FieldKey.ALBUM_ARTIST);
        assertNotNull(FieldKey.ARTIST);
        assertNotNull(FieldKey.GENRE);
        assertNotNull(FieldKey.YEAR);
        assertNotNull(FieldKey.IS_COMPILATION);
        assertNotNull(FieldKey.COMPOSER);
        assertNotNull(FieldKey.GROUPING);
        assertNotNull(FieldKey.RATING);
        assertNotNull(FieldKey.BPM);
        assertNotNull(FieldKey.COMMENT);
        assertNotNull(FieldKey.TRACK);
        assertNotNull(FieldKey.TRACK_TOTAL);
        assertNotNull(FieldKey.DISC_NO);
        assertNotNull(FieldKey.DISC_TOTAL);
        assertNotNull(FieldKey.TITLE);
    }

    @Test
    void testArtworkModificationAndWrite() throws Exception {
        File file = createDummyMp3("art_test.mp3");
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        Artwork artwork = ArtworkFactory.getNew();
        byte[] dummyPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        artwork.setBinaryData(dummyPng);
        artwork.setMimeType("image/png");
        tag.setField(artwork);

        AudioFileIO.write(audioFile);

        AudioFile reloaded = AudioFileIO.read(file);
        Tag reloadedTag = reloaded.getTag();
        assertNotNull(reloadedTag);
        Artwork reloadedArt = reloadedTag.getFirstArtwork();
        assertNotNull(reloadedArt);
        assertNotNull(reloadedArt.getBinaryData());
    }

    @Test
    void testTagModificationAndWrite() throws Exception {
        File file = createDummyMp3("tag_test.mp3");
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        tag.setField(FieldKey.TITLE, "My Song");
        tag.setField(FieldKey.ARTIST, "Artist A");
        tag.setField(FieldKey.ALBUM, "Album A");
        tag.setField(FieldKey.ALBUM_ARTIST, "Album Artist A");
        tag.setField(FieldKey.GENRE, "Rock");
        tag.setField(FieldKey.YEAR, "2024");
        tag.setField(FieldKey.IS_COMPILATION, "1");
        tag.setField(FieldKey.COMPOSER, "Composer A");
        tag.setField(FieldKey.GROUPING, "Group A");
        tag.setField(FieldKey.RATING, "80");
        tag.setField(FieldKey.BPM, "120");
        tag.setField(FieldKey.COMMENT, "Great song");
        tag.setField(FieldKey.TRACK, "3");
        tag.setField(FieldKey.TRACK_TOTAL, "10");
        tag.setField(FieldKey.DISC_NO, "1");
        tag.setField(FieldKey.DISC_TOTAL, "2");

        AudioFileIO.write(audioFile);

        AudioFile reloaded = AudioFileIO.read(file);
        Tag reloadedTag = reloaded.getTag();
        assertNotNull(reloadedTag);
        assertEquals("My Song", reloadedTag.getFirst(FieldKey.TITLE));
        assertEquals("Artist A", reloadedTag.getFirst(FieldKey.ARTIST));
        assertEquals("Album A", reloadedTag.getFirst(FieldKey.ALBUM));
        assertEquals("Album Artist A", reloadedTag.getFirst(FieldKey.ALBUM_ARTIST));
        assertEquals("Rock", reloadedTag.getFirst(FieldKey.GENRE));
        assertEquals("2024", reloadedTag.getFirst(FieldKey.YEAR));
        assertEquals("1", reloadedTag.getFirst(FieldKey.IS_COMPILATION));
        assertEquals("Composer A", reloadedTag.getFirst(FieldKey.COMPOSER));
        assertEquals("Group A", reloadedTag.getFirst(FieldKey.GROUPING));
        assertEquals("80", reloadedTag.getFirst(FieldKey.RATING));
        assertEquals("120", reloadedTag.getFirst(FieldKey.BPM));
        assertEquals("Great song", reloadedTag.getFirst(FieldKey.COMMENT));
        assertEquals("3", reloadedTag.getFirst(FieldKey.TRACK));
        assertEquals("10", reloadedTag.getFirst(FieldKey.TRACK_TOTAL));
        assertEquals("1", reloadedTag.getFirst(FieldKey.DISC_NO));
        assertEquals("2", reloadedTag.getFirst(FieldKey.DISC_TOTAL));
    }
}

package com.mas6y6.musmeta.core;

import com.mas6y6.musmeta.config.ConfigBuilder;
import com.mas6y6.musmeta.config.ConfigCodec;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.nio.file.Path;

public class Song {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Song.class);

    private static final String UNKNOWN_ARTIST = "Unknown Artist";
    public static final String UNKNOWN_ALBUM = "Unknown";

    public static final ConfigCodec<Song> CODEC = ConfigCodec.of(
            Song::serialize,
            Song::decodeFrom
    );

    private AudioFile audioFile;

    public Song(AudioFile audioFile) {
        this.audioFile = audioFile;
    }

    private static void serialize(Song song, ConfigBuilder builder) {
        if (song != null && song.getAudioFile() != null && song.getAudioFile().getFile() != null) {
            builder.setString("path", song.getAudioFile().getFile().getAbsolutePath());
        }
    }

    private static Song decodeFrom(ConfigBuilder builder) {
        if (builder == null) {
            return null;
        }
        String path = builder.getString("path");
        if (path == null || path.isBlank()) {
            path = builder.getString("value");
        }
        if (path == null || path.isBlank()) {
            return null;
        }
        try {
            return new Song(AudioFileIO.read(Path.of(path).toFile()));
        } catch (Exception e) {
            LOGGER.warn("Skipping unreadable song in saved library: {}", path);
            return null;
        }
    }

    public AudioFile getAudioFile() {
        return audioFile;
    }

    public void replaceAudioFile(AudioFile audioFile) {
        this.audioFile = audioFile;
    }

    public Tag getTag() {
        return audioFile.getTag();
    }

    public boolean hasTag() {
        return getTag() != null;
    }

    public boolean hasBasicTags() {
        return hasTag()
                && !UNKNOWN_ALBUM.equalsIgnoreCase(getAlbum())
                && !"Unknown Album".equalsIgnoreCase(getAlbum())
                && !getTitle().isBlank();
    }

    public String getTitle() {
        return tagFirst(FieldKey.TITLE, audioFile.getFile().getName());
    }

    public String getRawTitle() {
        return tagFirst(FieldKey.TITLE, "");
    }

    public String getArtist() {
        return tagFirst(FieldKey.ARTIST, UNKNOWN_ARTIST);
    }

    public String getRawArtist() {
        return tagFirst(FieldKey.ARTIST, "");
    }

    public String getAlbumArtist() {
        String albumArtist = tagFirst(FieldKey.ALBUM_ARTIST, "");
        return albumArtist.isBlank() ? getArtist() : albumArtist;
    }

    public String getRawAlbumArtist() {
        return tagFirst(FieldKey.ALBUM_ARTIST, "");
    }

    public String getAlbum() {
        return tagFirst(FieldKey.ALBUM, UNKNOWN_ALBUM);
    }

    public String getRawAlbum() {
        return tagFirst(FieldKey.ALBUM, "");
    }

    public String getGenre() {
        return tagFirst(FieldKey.GENRE, "");
    }

    public String getYear() {
        return tagFirst(FieldKey.YEAR, "");
    }

    public boolean isCompilation() {
        String val = tagFirst(FieldKey.IS_COMPILATION, "");
        return "1".equals(val) || "true".equalsIgnoreCase(val) || "yes".equalsIgnoreCase(val);
    }

    public String getComposer() {
        return tagFirst(FieldKey.COMPOSER, "");
    }

    public String getGrouping() {
        return tagFirst(FieldKey.GROUPING, "");
    }

    public String getRating() {
        return tagFirst(FieldKey.RATING, "");
    }

    public String getBpm() {
        return tagFirst(FieldKey.BPM, "");
    }

    public String getComment() {
        return tagFirst(FieldKey.COMMENT, "");
    }

    public int getDiscNumber() {
        return positiveNumber(tagFirst(FieldKey.DISC_NO, ""), 1);
    }

    public int getDiscTotal() {
        int discTotal = positiveNumber(tagFirst(FieldKey.DISC_TOTAL, ""), 1);
        return Math.max(discTotal, getDiscNumber());
    }

    public boolean hasDiscs() {
        return getDiscTotal() > 1 || getDiscNumber() > 1;
    }

    public int getTrackNumber() {
        return positiveNumber(tagFirst(FieldKey.TRACK, ""), 0);
    }

    public int getTrackTotal() {
        return positiveNumber(tagFirst(FieldKey.TRACK_TOTAL, ""), 0);
    }

    public byte[] getArtworkData() {
        Tag tag = getTag();
        if (tag != null) {
            try {
                Artwork art = tag.getFirstArtwork();
                if (art != null) {
                    return art.getBinaryData();
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public Image getArtworkImage() {
        byte[] data = getArtworkData();
        if (data != null && data.length > 0) {
            try {
                return ImageIO.read(new ByteArrayInputStream(data));
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public void setTagField(FieldKey key, String value) {
        try {
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            if (value == null || value.isBlank()) {
                tag.deleteField(key);
            } else {
                tag.setField(key, value.trim());
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to set tag field {} on {}: {}", key, audioFile.getFile(), e.getMessage());
        }
    }

    public void setCompilation(boolean compilation) {
        try {
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            if (compilation) {
                tag.setField(FieldKey.IS_COMPILATION, "1");
            } else {
                tag.deleteField(FieldKey.IS_COMPILATION);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to set compilation tag on {}: {}", audioFile.getFile(), e.getMessage());
        }
    }

    public void setArtwork(byte[] data, String mimeType) {
        try {
            Tag tag = audioFile.getTagOrCreateAndSetDefault();
            tag.deleteArtworkField();
            if (data != null && data.length > 0) {
                Artwork artwork = ArtworkFactory.getNew();
                artwork.setBinaryData(data);
                artwork.setMimeType(mimeType != null && !mimeType.isBlank() ? mimeType : "image/jpeg");
                tag.setField(artwork);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to set artwork on {}: {}", audioFile.getFile(), e.getMessage());
        }
    }

    public void deleteArtwork() {
        try {
            Tag tag = getTag();
            if (tag != null) {
                tag.deleteArtworkField();
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to delete artwork on {}: {}", audioFile.getFile(), e.getMessage());
        }
    }

    public void saveToFile() throws Exception {
        AudioFileIO.write(audioFile);
    }

    private String tagFirst(FieldKey key, String fallback) {
        Tag tag = getTag();
        if (tag == null) {
            return fallback;
        }
        try {
            String value = tag.getFirst(key);
            return value == null || value.isBlank() ? fallback : value.trim();
        } catch (UnsupportedOperationException e) {
            // Some tag types (e.g. WAV Info tags) do not support every field key.
            return fallback;
        }
    }

    private static int positiveNumber(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String number = value.trim().split("/", 2)[0];
        try {
            int parsed = Integer.parseInt(number);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
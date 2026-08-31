package com.mas6y6.musmeta.core;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

public class Song {
    private static final String UNKNOWN_ARTIST = "Unknown Artist";

    private AudioFile audioFile;

    public Song(AudioFile audioFile) {
        this.audioFile = audioFile;
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

    public String getTitle() {
        return tagFirst(FieldKey.TITLE, audioFile.getFile().getName());
    }

    public String getArtist() {
        return tagFirst(FieldKey.ARTIST, UNKNOWN_ARTIST);
    }

    public String getAlbumArtist() {
        String albumArtist = tagFirst(FieldKey.ALBUM_ARTIST, "");
        return albumArtist.isBlank() ? getArtist() : albumArtist;
    }

    public String getAlbum() {
        return tagFirst(FieldKey.ALBUM, "Unknown Album");
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
package com.mas6y6.musmeta.core;

import org.jaudiotagger.audio.AudioFile;

import static com.mas6y6.musmeta.core.Core.LOGGER;

public class UntaggedSong {
    private final AudioFile audioFile;

    public UntaggedSong(AudioFile audioFile) {
        this.audioFile = audioFile;
    }

    public Song attachTags() {
        try {
            audioFile.getTagAndConvertOrCreateAndSetDefault();
            audioFile.commit();
        } catch (Exception e) {
            LOGGER.error("Error attaching tags to song: {}", audioFile.getFile().getName(), e);
        }
        return new Song(audioFile);
    }
}

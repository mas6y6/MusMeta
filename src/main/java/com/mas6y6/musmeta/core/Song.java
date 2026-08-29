package com.mas6y6.musmeta.core;

import org.jaudiotagger.audio.AudioFile;

public class Song {
    private AudioFile audioFile;
    private Album album;
    private Disc disc;

    public Song(AudioFile audioFile) {
        this.audioFile = audioFile;
    }

    public AudioFile getAudioFile() {
        return audioFile;
    }

    public Album getAlbum() {
        return album;
    }

    public Disc getDisc() {
        return disc;
    }

    public void replaceAudioFile(AudioFile audioFile) {
        this.audioFile = audioFile;
    }

    void assignTo(Album album, Disc disc) {
        this.album = album;
        this.disc = disc;
    }
}

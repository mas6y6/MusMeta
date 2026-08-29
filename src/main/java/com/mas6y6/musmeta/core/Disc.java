package com.mas6y6.musmeta.core;

import java.util.ArrayList;
import java.util.List;

public class Disc {
    private final int discIndex;
    private int discTotal;
    private final ArrayList<Song> songs = new ArrayList<>();

    public Disc(int discIndex, int discTotal) {
        this.discIndex = discIndex;
        this.discTotal = discTotal;
    }

    public int getDiscIndex() {
        return discIndex;
    }

    public int getDiscTotal() {
        return discTotal;
    }

    public List<Song> getSongs() {
        return List.copyOf(songs);
    }

    void setDiscTotal(int discTotal) {
        this.discTotal = discTotal;
    }

    void addSong(Song song) {
        songs.add(song);
    }
}

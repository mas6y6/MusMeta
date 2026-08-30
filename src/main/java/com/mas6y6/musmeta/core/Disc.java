package com.mas6y6.musmeta.core;

import java.util.ArrayList;
import java.util.Comparator;
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
        this.discTotal = Math.max(this.discTotal, discTotal);
    }

    void addSong(Song song) {
        songs.add(song);
        songs.sort(Comparator
                .comparingInt((Song s) -> s.getTrackNumber() == 0 ? Integer.MAX_VALUE : s.getTrackNumber())
                .thenComparing(Song::getTitle, String.CASE_INSENSITIVE_ORDER));
    }
}
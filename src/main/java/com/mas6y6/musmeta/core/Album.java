package com.mas6y6.musmeta.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Album {
    private static final String UNKNOWN_ARTIST = "Unknown Artist";

    private final String title;
    private final ArrayList<Disc> discs = new ArrayList<>();

    public Album(String title) {
        this.title = Objects.requireNonNull(title, "Album title cannot be null");
    }

    public String getTitle() {
        return title;
    }

    /**
     * Resolves the album artist from its songs, falling back to the first
     * artist and finally to "Unknown Artist".
     */
    public String getArtist() {
        for (Disc disc : discs) {
            for (Song song : disc.getSongs()) {
                String albumArtist = song.getAlbumArtist();
                if (!albumArtist.isBlank() && !UNKNOWN_ARTIST.equalsIgnoreCase(albumArtist)) {
                    return albumArtist;
                }
            }
        }
        for (Disc disc : discs) {
            for (Song song : disc.getSongs()) {
                String artist = song.getArtist();
                if (!artist.isBlank() && !UNKNOWN_ARTIST.equalsIgnoreCase(artist)) {
                    return artist;
                }
            }
        }
        return UNKNOWN_ARTIST;
    }

    public List<Disc> getDiscs() {
        return List.copyOf(discs);
    }

    public boolean hasDiscs() {
        return discs.size() > 1;
    }

    public List<Song> getSongs() {
        ArrayList<Song> songs = new ArrayList<>();
        for (Disc disc : discs) {
            songs.addAll(disc.getSongs());
        }
        return List.copyOf(songs);
    }

    /**
     * Adds a song to the album, placing it into the disc its tags declare.
     * A single-disc song simply lands on disc 1.
     */
    public void addSong(Song song) {
        Objects.requireNonNull(song, "Song cannot be null");

        int discIndex = song.getDiscNumber();
        int discTotal = song.getDiscTotal();

        Disc disc = discs.stream()
                .filter(candidate -> candidate.getDiscIndex() == discIndex)
                .findFirst()
                .orElseGet(() -> {
                    Disc created = new Disc(discIndex, discTotal);
                    discs.add(created);
                    return created;
                });

        disc.setDiscTotal(discTotal);
        disc.addSong(song);
        discs.sort(Comparator.comparingInt(Disc::getDiscIndex));
    }

    void addDisc(Disc disc) {
        discs.add(disc);
        discs.sort(Comparator.comparingInt(Disc::getDiscIndex));
    }
}
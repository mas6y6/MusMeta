package com.mas6y6.musmeta.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Album {
    private final ArrayList<Disc> discs;
    private final String title;

    public Album(String title) {
        this.title = title;
        this.discs = new ArrayList<>();
    }

    public String getTitle() {
        return title;
    }

    public List<Disc> getDiscs() {
        return List.copyOf(discs);
    }

    void addSong(Song song, int discIndex, int discTotal) {
        Disc disc = discs.stream()
                .filter(candidate -> candidate.getDiscIndex() == discIndex)
                .findFirst()
                .orElseGet(() -> {
                    Disc created = new Disc(discIndex, discTotal);
                    discs.add(created);
                    return created;
        });

        disc.setDiscTotal(Math.max(disc.getDiscTotal(), discTotal));
        song.assignTo(this, disc);
        disc.addSong(song);
        discs.sort(Comparator.comparingInt(Disc::getDiscIndex));
    }
}

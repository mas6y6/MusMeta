package com.mas6y6.musmeta.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Duplicates {

    public record Group(List<Song> existing, List<Song> incoming) {

        public Group {
            existing = List.copyOf(existing);
            incoming = List.copyOf(incoming);
        }
    }

    private Duplicates() {
    }

    public static List<Group> findGroups(List<Song> incomingSongs, List<Song> librarySongs) {
        Map<String, List<Song>> incomingByIdentity = new LinkedHashMap<>();
        Map<Song, String> identityBySong = new IdentityHashMap<>();

        for (Song song : incomingSongs) {
            String key = identityKey(song);
            identityBySong.put(song, key);
            incomingByIdentity.computeIfAbsent(key, k -> new ArrayList<>()).add(song);
        }

        Set<String> libraryPaths = new HashSet<>();
        for (Song song : librarySongs) {
            libraryPaths.add(pathKey(song));
        }

        Set<String> groupedIncomingPaths = new HashSet<>();
        List<Group> groups = new ArrayList<>();
        for (Map.Entry<String, List<Song>> entry : incomingByIdentity.entrySet()) {
            String key = entry.getKey();
            List<Song> groupIncoming = entry.getValue();

            if (groupIncoming.size() >= 2) {
                Group group = new Group(List.of(), groupIncoming);
                groups.add(group);
                for (Song incoming : group.incoming()) {
                    groupedIncomingPaths.add(pathKey(incoming));
                }
                continue;
            }

            Song onlyIncoming = groupIncoming.get(0);
            String onlyPath = pathKey(onlyIncoming);
            if (libraryPaths.contains(onlyPath) || groupedIncomingPaths.contains(onlyPath)) {
                continue;
            }

            List<Song> existingMatches = new ArrayList<>();
            for (Song librarySong : librarySongs) {
                if (identityKey(librarySong).equals(key) || pathKey(librarySong).equals(onlyPath)) {
                    existingMatches.add(librarySong);
                }
                for (Song incoming : groupIncoming) {
                    if (incoming == librarySong) {
                        existingMatches.add(librarySong);
                    }
                }
            }

            if (!existingMatches.isEmpty()) {
                groups.add(new Group(existingMatches, List.copyOf(groupIncoming)));
            }
        }

        return groups;
    }

    private static String identityKey(Song song) {
        String title = normalize(song.getRawTitle());
        if (title.isEmpty()) {
            return pathKey(song);
        }
        return "song:"
                + title
                + "\u0000"
                + normalize(song.getAlbum())
                + "\u0000"
                + normalize(song.getArtist())
                + "\u0000"
                + normalize(song.getAlbumArtist());
    }

    private static String pathKey(Song song) {
        return song.getAudioFile()
                .getFile()
                .toPath()
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

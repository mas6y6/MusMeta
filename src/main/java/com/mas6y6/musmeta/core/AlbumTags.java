package com.mas6y6.musmeta.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class AlbumTags {

    private static final String YES = "Yes";
    private static final String NO = "No";

    public enum Essential {

        ALBUM("Album", false),
        ALBUM_ARTIST("Album Artist", true),
        COMPILATION("Compilation", true);

        private final String label;
        private final boolean negotiable;

        Essential(String label, boolean negotiable) {
            this.label = label;
            this.negotiable = negotiable;
        }

        public String label() {
            return label;
        }

        public boolean isNegotiable() {
            return negotiable;
        }

        public String albumValue(Album album) {
            return switch (this) {
                case ALBUM -> trimToEmpty(album.getTitle());
                case ALBUM_ARTIST -> trimToEmpty(album.getAlbumArtist());
                case COMPILATION -> album.isCompilation() ? YES : NO;
            };
        }

        public String songValue(Song song) {
            return switch (this) {
                case ALBUM -> trimToEmpty(song.getRawAlbum());
                case ALBUM_ARTIST -> trimToEmpty(song.getRawAlbumArtist());
                case COMPILATION -> compilationValue(song.getRawCompilation());
            };
        }

        public boolean conflicts(Song song, Album album) {
            String songValue = songValue(song);
            return !songValue.isBlank() && !songValue.equalsIgnoreCase(albumValue(album));
        }
    }

    private AlbumTags() {
    }

    public static List<Essential> all() {
        return List.of(Essential.values());
    }

    public static Set<Essential> negotiableConflicts(List<Song> songs, Album album) {
        Set<Essential> conflicts = EnumSet.noneOf(Essential.class);
        if (songs == null) {
            return conflicts;
        }
        for (Essential tag : Essential.values()) {
            if (!tag.isNegotiable()) {
                continue;
            }
            for (Song song : songs) {
                if (song != null && tag.conflicts(song, album)) {
                    conflicts.add(tag);
                    break;
                }
            }
        }
        return conflicts;
    }

    public static List<String> songValues(List<Song> songs, Essential tag) {
        List<String> values = new ArrayList<>();
        if (songs == null) {
            return values;
        }
        for (Song song : songs) {
            String value = tag.songValue(song);
            if (!value.isBlank() && !values.contains(value)) {
                values.add(value);
            }
        }
        return values;
    }

    private static String compilationValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim();
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)
                ? YES
                : NO;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

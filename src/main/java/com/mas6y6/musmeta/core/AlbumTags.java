package com.mas6y6.musmeta.core;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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

        public @NonNull String albumValue(@NonNull Album album) {
            return switch (this) {
                case ALBUM -> trimToEmpty(album.getTitle());
                case ALBUM_ARTIST -> trimToEmpty(album.getAlbumArtist());
                case COMPILATION -> album.isCompilation() ? YES : NO;
            };
        }

        public String songValue(@NonNull Song song) {
            return switch (this) {
                case ALBUM -> trimToEmpty(song.getRawAlbum());
                case ALBUM_ARTIST -> trimToEmpty(song.getRawAlbumArtist());
                case COMPILATION -> compilationValue(song.getRawCompilation());
            };
        }

        public boolean conflicts(@NonNull Song song, @NonNull Album album) {
            String songValue = songValue(song);
            return !songValue.isBlank() && !songValue.equalsIgnoreCase(albumValue(album));
        }
    }

    private AlbumTags() {
    }

    public static @NonNull List<Essential> all() {
        return List.of(Essential.values());
    }

    public static @NonNull Set<Essential> negotiableConflicts(@Nullable List<Song> songs, @NonNull Album album) {
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

    public static @NonNull List<String> songValues(@Nullable List<Song> songs, @NonNull Essential tag) {
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

    private static @NonNull String compilationValue(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim();
        return "1".equals(value) || "true".equalsIgnoreCase(value) || "yes".equalsIgnoreCase(value)
                ? YES
                : NO;
    }

    private static @NonNull String trimToEmpty(@Nullable String value) {
        return value == null ? "" : value.trim();
    }
}

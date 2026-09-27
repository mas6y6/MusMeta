package com.mas6y6.musmeta.core;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The album level tags that every song of an album is expected to carry, and
 * the rules for copying them onto a song that is being moved onto that album.
 *
 * <p>Only the tags listed here take part. Anything belonging to a single song
 * (title, song artist, track number, comments...) is left untouched, and a song
 * that states none of these tags has nothing to lose, so nothing is asked
 * about it.
 */
public final class AlbumTags {

    private static final String YES = "Yes";
    private static final String NO = "No";

    public enum Essential {

        /**
         * The album title. This is the value the library files a song under,
         * so moving a song onto an album always rewrites it; there is nothing
         * for the user to decide here.
         */
        ALBUM("Album", false),

        /**
         * The artist of the whole album, the tag that groups an album's songs
         * together independently of their individual artists.
         */
        ALBUM_ARTIST("Album Artist", true),

        /**
         * Whether the song takes part in a compilation.
         */
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

        /**
         * @return {@code true} when the user may keep the value a song already
         *         states instead of taking the album's
         */
        public boolean isNegotiable() {
            return negotiable;
        }

        /**
         * @return the value every song of the album has to end up carrying, or
         *         an empty string when the album itself states none
         */
        public String albumValue(Album album) {
            return switch (this) {
                case ALBUM -> trimToEmpty(album.getTitle());
                case ALBUM_ARTIST -> trimToEmpty(album.getAlbumArtist());
                case COMPILATION -> album.isCompilation() ? YES : NO;
            };
        }

        /**
         * @return the value the song carries, or an empty string when the song
         *         states nothing at all, which is what keeps "says no" apart
         *         from "says nothing"
         */
        public String songValue(Song song) {
            return switch (this) {
                case ALBUM -> trimToEmpty(song.getRawAlbum());
                case ALBUM_ARTIST -> trimToEmpty(song.getRawAlbumArtist());
                case COMPILATION -> compilationValue(song.getRawCompilation());
            };
        }

        /**
         * A conflict is a tag the song states in its own words while the album
         * asks for something else. A song that states nothing is never in
         * conflict, it simply has no value of its own to protect.
         */
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

    /**
     * Collects the album tags the given songs disagree with the album about,
     * limited to the ones the user actually gets to decide on. A song whose
     * album title differs is not a conflict here: moving it onto the album
     * rewrites the title by definition.
     *
     * @return the negotiable tags at least one song states differently, empty
     *         when there is nothing to ask about
     */
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

    /**
     * @return every distinct value the songs state for the tag, in the order
     *         they are met, for showing the user what they are choosing between
     */
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

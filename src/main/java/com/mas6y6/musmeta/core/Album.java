package com.mas6y6.musmeta.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.mas6y6.musmeta.Main;
import com.mas6y6.musmeta.config.ConfigBuilder;
import com.mas6y6.musmeta.config.ConfigCodec;
import com.mas6y6.musmeta.ui.album.AlbumUI;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class Album {
    public static final ConfigCodec<Album> CODEC = ConfigCodec.of(
            Album::serialize,
            Album::decodeFrom
    );

    private static final String UNKNOWN_ARTIST = "Unknown Artist";
    private static final String ARTWORK_DIR = "album_art";

    private String title;
    private Path artworkPath;
    private final long createdAt;
    private boolean unknown;
    private final ArrayList<Disc> discs = new ArrayList<>();

    public Album(String title) {
        this(title, null, System.currentTimeMillis());
    }

    public Album(String title, Path artworkPath) {
        this(title, artworkPath, System.currentTimeMillis());
    }

    public Album(String title, Path artworkPath, long createdAt) {
        this.title = Objects.requireNonNull(title, "Album title cannot be null");
        this.artworkPath = artworkPath;
        this.createdAt = createdAt;
        if (Song.UNKNOWN_ALBUM.equalsIgnoreCase(title) || "Unknown Album".equalsIgnoreCase(title)) {
            this.unknown = true;
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = Objects.requireNonNull(title, "Album title cannot be null");
        this.unknown = Song.UNKNOWN_ALBUM.equalsIgnoreCase(title) || "Unknown Album".equalsIgnoreCase(title);
    }

    public Path getArtworkPath() {
        return artworkPath;
    }

    public void setArtworkPath(Path artworkPath) {
        this.artworkPath = artworkPath;
    }

    public void setArtworkBytes(byte[] data) {
        cacheArtwork(data);
    }

    public void removeArtwork() {
        if (artworkPath != null) {
            try {
                Files.deleteIfExists(artworkPath);
            } catch (Exception ignored) {
            }
            artworkPath = null;
            Library.getInstance().save();
        }
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isUnknown() {
        return unknown;
    }

    public void setUnknown(boolean unknown) {
        this.unknown = unknown;
    }

    public String getGenre() {
        for (Song song : getSongs()) {
            String genre = song.getGenre();
            if (!genre.isBlank()) {
                return genre;
            }
        }
        return "";
    }

    public String getYear() {
        for (Song song : getSongs()) {
            String year = song.getYear();
            if (!year.isBlank()) {
                return year;
            }
        }
        return "";
    }

    public String getComposer() {
        for (Song song : getSongs()) {
            String composer = song.getComposer();
            if (!composer.isBlank()) {
                return composer;
            }
        }
        return "";
    }

    public String getGrouping() {
        for (Song song : getSongs()) {
            String grouping = song.getGrouping();
            if (!grouping.isBlank()) {
                return grouping;
            }
        }
        return "";
    }

    public String getRating() {
        for (Song song : getSongs()) {
            String rating = song.getRating();
            if (!rating.isBlank()) {
                return rating;
            }
        }
        return "";
    }

    public String getBpm() {
        for (Song song : getSongs()) {
            String bpm = song.getBpm();
            if (!bpm.isBlank()) {
                return bpm;
            }
        }
        return "";
    }

    public String getComment() {
        for (Song song : getSongs()) {
            String comment = song.getComment();
            if (!comment.isBlank()) {
                return comment;
            }
        }
        return "";
    }

    public String getAlbumArtist() {
        String first = null;
        for (Disc disc : discs) {
            for (Song song : disc.getSongs()) {
                String albumArtist = song.getRawAlbumArtist();
                if (albumArtist.isBlank() || UNKNOWN_ARTIST.equalsIgnoreCase(albumArtist)) {
                    continue;
                }
                if (first == null) {
                    first = albumArtist;
                } else if (!normalizeArtist(albumArtist).equals(normalizeArtist(first))) {
                    return "";
                }
            }
        }
        return first == null ? "" : first;
    }

    public boolean isCompilation() {
        if (getArtist().variousArtists()) {
            return true;
        }
        for (Song song : getSongs()) {
            if (song.isCompilation()) {
                return true;
            }
        }
        return false;
    }

    public int getDiscTotal() {
        int total = discs.size();
        for (Disc disc : discs) {
            total = Math.max(total, disc.getDiscTotal());
            for (Song song : disc.getSongs()) {
                total = Math.max(total, song.getDiscTotal());
            }
        }
        return Math.max(1, total);
    }

    public int getTrackTotal() {
        int max = 0;
        for (Song song : getSongs()) {
            max = Math.max(max, song.getTrackTotal());
        }
        return max > 0 ? max : getSongs().size();
    }

    public void removeSong(Song song) {
        for (Disc disc : discs) {
            disc.removeSong(song);
        }
        discs.removeIf(disc -> disc.getSongs().isEmpty() && disc.getMissingSongPaths().isEmpty());
    }

    public record ArtistInfo(String artist, boolean variousArtists) {
    }

    public ArtistInfo getArtist() {
        String firstAlbumArtist = null;
        boolean multipleAlbumArtists = false;

        for (Disc disc : discs) {
            for (Song song : disc.getSongs()) {
                String albumArtist = song.getAlbumArtist();
                if (!albumArtist.isBlank() && !UNKNOWN_ARTIST.equalsIgnoreCase(albumArtist)) {
                    if (firstAlbumArtist == null) {
                        firstAlbumArtist = albumArtist;
                    } else if (!normalizeArtist(albumArtist).equals(normalizeArtist(firstAlbumArtist))) {
                        multipleAlbumArtists = true;
                    }
                }
            }
        }

        if (firstAlbumArtist != null && !multipleAlbumArtists
                && !"Various Artists".equalsIgnoreCase(firstAlbumArtist)) {
            return new ArtistInfo(firstAlbumArtist, false);
        }

        return resolveFromSongArtists();
    }

    /**
     * Derives the album artist from the songs' artist tags. Returns the shared
     * artist when every song has the same artist, "Various Artists" when the
     * songs belong to different artists, and "Unknown Artist" when no usable
     * artist tag exists.
     */
    private ArtistInfo resolveFromSongArtists() {
        String firstArtist = null;
        boolean multipleArtists = false;

        for (Disc disc : discs) {
            for (Song song : disc.getSongs()) {
                String artist = song.getArtist();
                if (!artist.isBlank() && !UNKNOWN_ARTIST.equalsIgnoreCase(artist)) {
                    if (firstArtist == null) {
                        firstArtist = artist;
                    } else if (!normalizeArtist(artist).equals(normalizeArtist(firstArtist))) {
                        multipleArtists = true;
                    }
                }
            }
        }

        if (firstArtist == null) {
            return new ArtistInfo(UNKNOWN_ARTIST, false);
        }

        boolean various = multipleArtists
                || "Various Artists".equalsIgnoreCase(firstArtist);
        return new ArtistInfo(various ? "Various Artists" : firstArtist, various);
    }

    private static String normalizeArtist(String artist) {
        return artist == null ? "" : artist.trim().toLowerCase(Locale.ROOT);
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

    public Image getArtworkImage() {
        if (artworkPath != null && Files.isRegularFile(artworkPath)) {
            try {
                Image image = ImageIO.read(artworkPath.toFile());
                if (image != null) {
                    return image;
                }
            } catch (IOException ignored) {
            }
        }

        for (Song song : getSongs()) {
            try {
                Tag tag = song.getTag();
                if (tag == null) {
                    continue;
                }

                Artwork artwork = tag.getFirstArtwork();
                if (artwork == null) {
                    continue;
                }

                byte[] data = artwork.getBinaryData();
                if (data == null || data.length == 0) {
                    continue;
                }

                Image image = ImageIO.read(new ByteArrayInputStream(data));
                if (image != null) {
                    cacheArtwork(data);
                    return image;
                }
            } catch (UnsupportedOperationException | IOException ignored) {
            }
        }
        return new ImageIcon(
                Objects.requireNonNull(
                        AlbumUI.class.getResource("/placeholder_album.png")
                )
        ).getImage();
    }

    private void cacheArtwork(byte[] data) {
        try {
            Path artDir = Main.appDir.resolve(ARTWORK_DIR);
            Files.createDirectories(artDir);

            Path cached = artDir.resolve(fileSafe(title) + ".png");
            Files.write(cached, data);

            artworkPath = cached;
            Library.getInstance().save();
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static String fileSafe(String name) {
        String sanitized = name
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .trim();
        return sanitized.isBlank() ? "album" : sanitized;
    }

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

    private static void serialize(Album album, ConfigBuilder builder) {
        builder.setString("title", album.getTitle());
        builder.setLong("createdAt", album.getCreatedAt());
        if (album.isUnknown()) {
            builder.setBoolean("unknown", true);
        }

        Path artworkPath = album.getArtworkPath();
        if (artworkPath != null) {
            builder.setString("artworkPath", artworkPath.toString());
        }

        JsonArray discsArray = new JsonArray();
        for (Disc disc : album.getDiscs()) {
            ConfigBuilder discBuilder = new ConfigBuilder();
            Disc.CODEC.encode(disc, discBuilder);
            discsArray.add(discBuilder.toJsonObject());
        }
        builder.set("discs", discsArray);
    }

    private static Album decodeFrom(ConfigBuilder builder) {
        if (builder == null) {
            return null;
        }
        String title = builder.getString("title", Song.UNKNOWN_ALBUM);

        Path artworkPath = null;
        if (builder.has("artworkPath")) {
            String raw = builder.getString("artworkPath");
            if (raw != null && !raw.isBlank()) {
                artworkPath = Path.of(raw);
            }
        }

        long createdAt = builder.getLong("createdAt", System.currentTimeMillis());

        Album album = new Album(title, artworkPath, createdAt);
        if (builder.getBoolean("unknown", false)) {
            album.setUnknown(true);
        }

        JsonElement discsElement = builder.get("discs");
        if (discsElement != null && discsElement.isJsonArray()) {
            for (JsonElement discElement : discsElement.getAsJsonArray()) {
                if (!discElement.isJsonObject()) {
                    continue;
                }
                ConfigBuilder discBuilder = ConfigBuilder.from(discElement.getAsJsonObject());
                Disc disc = Disc.CODEC.decode(discBuilder);
                if (disc != null) {
                    album.addDisc(disc);
                }
            }
        }

        return album;
    }
}
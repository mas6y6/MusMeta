package com.mas6y6.musmeta.core;

import com.mas6y6.musmeta.Main;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class Album {
    private static final String UNKNOWN_ARTIST = "Unknown Artist";
    private static final String ARTWORK_DIR = "album_art";

    private final String title;
    private Path artworkPath;
    private final long createdAt;
    private final ArrayList<Disc> discs = new ArrayList<>();

    public Album(String title) {
        this(title, null, System.currentTimeMillis());
    }

    /**
     * Creates an album with an optional stored artwork image.
     *
     * @param title       the album title (must not be null)
     * @param artworkPath path to a stored artwork image, or {@code null} if none
     */
    public Album(String title, Path artworkPath) {
        this(title, artworkPath, System.currentTimeMillis());
    }

    /**
     * Creates an album with an optional stored artwork image and creation time.
     *
     * @param title       the album title (must not be null)
     * @param artworkPath path to a stored artwork image, or {@code null} if none
     * @param createdAt   creation timestamp (epoch millis), used to order albums
     */
    public Album(String title, Path artworkPath, long createdAt) {
        this.title = Objects.requireNonNull(title, "Album title cannot be null");
        this.artworkPath = artworkPath;
        this.createdAt = createdAt;
    }

    public String getTitle() {
        return title;
    }

    /**
     * @return the path to the album's stored artwork, or {@code null} if none
     */
    public Path getArtworkPath() {
        return artworkPath;
    }

    /**
     * @return this album's creation timestamp (epoch millis)
     */
    public long getCreatedAt() {
        return createdAt;
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
     * Returns the album's artwork: the stored (cached) artwork image if one is
     * set, otherwise the embedded artwork found in the album's songs. The first
     * time embedded artwork is found it is written to the {@code .musmeta}
     * folder so later lookups read it straight from disk and never re-decode
     * the audio tags. Returns {@code null} if no usable artwork exists.
     */
    public Image getArtworkImage() {
        if (artworkPath != null && Files.isRegularFile(artworkPath)) {
            try {
                Image image = ImageIO.read(artworkPath.toFile());
                if (image != null) {
                    return image;
                }
            } catch (IOException ignored) {
                // Fall through to embedded artwork below.
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
                // Some tag types don't support artwork, or the image failed to decode.
            }
        }
        return null;
    }

    /**
     * Persists the raw artwork bytes to {@code ~/.musmeta/album_art/<title>.png}
     * and points this album at the cached file, so the embedded art is decoded
     * only once. The image itself is not validated here; callers pass the bytes
     * of artwork they have already decoded successfully.
     */
    private void cacheArtwork(byte[] data) {
        try {
            Path artDir = Main.appDir.resolve(ARTWORK_DIR);
            Files.createDirectories(artDir);

            Path cached = artDir.resolve(fileSafe(title) + ".png");
            Files.write(cached, data);

            artworkPath = cached;
            Library.getInstance().save();
        } catch (IOException | RuntimeException ignored) {
            // Caching is best-effort; artwork is still returned from memory.
        }
    }

    private static String fileSafe(String name) {
        String sanitized = name
                .replaceAll("[\\\\/:*?\"<>|]", "_")
                .trim();
        return sanitized.isBlank() ? "album" : sanitized;
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
package com.mas6y6.musmeta.core;

import com.mas6y6.musmeta.Constants;
import com.mas6y6.musmeta.settings.Settings;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

public class Core {
    static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Core.class);
    static final ArrayList<Album> albums = new ArrayList<>();

    public record ScanResult(List<Song> musicFiles, List<UntaggedSong> untaggedSongs, List<Album> albums) {}

    public static List<Album> getAlbums() {
        return List.copyOf(albums);
    }

    public static ScanResult scanForMusicFiles(Path musicDir,Path... ignorePaths) {
        LOGGER.info("Scanning for music files...");
        LOGGER.info("Ignoring paths: {}", Arrays.toString(ignorePaths));

        Set<Path> ignored = Arrays.stream(ignorePaths)
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(Collectors.toUnmodifiableSet());

        Path musicDirectory = musicDir
                .toAbsolutePath()
                .normalize();

        ArrayList<Song> musicFiles = new ArrayList<>();
        ArrayList<UntaggedSong> untaggedSongs = new ArrayList<>();

        try {
            Files.walkFileTree(musicDirectory, new SimpleFileVisitor<>() {

                @Override
                public @NonNull FileVisitResult preVisitDirectory(
                        @NonNull Path dir,
                        @NonNull BasicFileAttributes attrs
                ) {
                    if (ignored.contains(dir.toAbsolutePath().normalize())) {
                        LOGGER.info("Skipping directory: {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFile(
                        @NonNull Path file,
                        @NonNull BasicFileAttributes attrs
                ) {
                    if (!attrs.isRegularFile()) {
                        return FileVisitResult.CONTINUE;
                    }

                    String extension = com.google.common.io.Files.getFileExtension(file.toString()).toLowerCase(Locale.ROOT);
                    try {
                        if (Constants.MUSIC_EXTENSIONS.contains(extension)) {
                            AudioFile audioFile = AudioFileIO.read(file.toFile());

                            if (audioFile.getTag() == null) {
                                untaggedSongs.add(new UntaggedSong(audioFile));
                            } else {
                                musicFiles.add(new Song(audioFile));
                            }

                            LOGGER.info("Processing file: {}", file);
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error processing file: {}", file, e);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NonNull FileVisitResult visitFileFailed(
                        @NonNull Path file,
                        @NonNull IOException exc
                ) {
                    LOGGER.error("{}{}", "Skipping unreadable item: "
                            + file
                            + " due to ", exc.getMessage());

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan music directory", e);
        }

        assortMusicIntoAlbums(musicFiles);
        return new ScanResult(List.copyOf(musicFiles), List.copyOf(untaggedSongs), getAlbums());
    }

    private static void assortMusicIntoAlbums(List<Song> songs) {
        Map<String, Album> albumsByTitle = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Song song : songs) {
            Tag tag = song.getAudioFile().getTag();
            String title = tag == null ? "" : tag.getFirst(FieldKey.ALBUM).trim();
            if (title.isEmpty()) {
                LOGGER.debug("Leaving song without an album tag unassigned: {}", song.getAudioFile().getFile());
                continue;
            }

            Album album = albumsByTitle.computeIfAbsent(title, Album::new);
            int discIndex = positiveNumber(tag == null ? "" : tag.getFirst(FieldKey.DISC_NO), 1);
            int discTotal = Math.max(discIndex,
                    positiveNumber(tag == null ? "" : tag.getFirst(FieldKey.DISC_TOTAL), 1));
            album.addSong(song, discIndex, discTotal);
        }

        albums.clear();
        albums.addAll(albumsByTitle.values());
        LOGGER.info("Assorted {} songs into {} albums", songs.size(), albums.size());
    }

    private static int positiveNumber(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        String number = value.trim().split("/", 2)[0];
        try {
            int parsed = Integer.parseInt(number);
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}

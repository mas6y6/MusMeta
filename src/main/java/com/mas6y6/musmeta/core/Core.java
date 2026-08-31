package com.mas6y6.musmeta.core;

import com.mas6y6.musmeta.Constants;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotReadVideoException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class Core {
    static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(Core.class);

    public record ScanResult(List<Song> musicFiles, List<UntaggedSong> untaggedSongs, List<Album> albums) {}

    public static List<Album> getAlbums() {
        return Library.getInstance().getAlbums();
    }

    public static List<Song> getLibrary() {
        return Library.getInstance().getSongs();
    }

    public static void addSong(Song song) {
        Library.getInstance().addSong(song);
    }

    public static void reset() {
        Library.getInstance().clear();
    }

    /**
     * Scans a directory tree for music, rebuilds the library from the tagged
     * songs found, and persists it.
     */
    public static ScanResult scanForMusicFiles(Path musicDir, List<Path> ignorePaths) {
        LOGGER.info("Scanning for music files...");
        LOGGER.info("Ignoring paths: {}", ignorePaths);

        Set<Path> ignored = ignorePaths.stream()
                .map(Path::toAbsolutePath)
                .map(Path::normalize)
                .collect(Collectors.toUnmodifiableSet());

        Path musicDirectory = musicDir
                .toAbsolutePath()
                .normalize();

        Path musMetaDirectory = musicDirectory.resolve("MusMeta");

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

                    if (dir.toAbsolutePath().normalize().equals(musMetaDirectory)) {
                        LOGGER.info("Skipping already-converted MusMeta directory: {}", dir);
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
                    if (Constants.MUSIC_EXTENSIONS.contains(extension)) {
                        try {
                            AudioFile audioFile = AudioFileIO.read(file.toFile());

                            if (audioFile.getTag() == null) {
                                untaggedSongs.add(new UntaggedSong(audioFile));
                            } else {
                                musicFiles.add(new Song(audioFile));
                            }

                            LOGGER.info("Processing file: {}", file);
                        } catch (CannotReadVideoException e) {
                            // Container holds a video track (e.g. MP4/M4A/OGG video) -> not audio-only.
                            LOGGER.warn("Skipping video file (not audio-only): {}", file);
                        } catch (CannotReadException e) {
                            // File could not be parsed as audio (e.g. broken or video-only container).
                            LOGGER.warn("Skipping file that cannot be read as audio: {} ({})", file, e.getMessage());
                        } catch (IOException | TagException | ReadOnlyFileException |
                                 InvalidAudioFrameException e) {
                            LOGGER.error("Error processing file: {}", file, e);
                        }
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

        Library library = Library.getInstance();
        library.clear();
        for (Song song : musicFiles) {
            library.addSong(song);
        }
        library.save();

        return new ScanResult(List.copyOf(musicFiles), List.copyOf(untaggedSongs), library.getAlbums());
    }
}
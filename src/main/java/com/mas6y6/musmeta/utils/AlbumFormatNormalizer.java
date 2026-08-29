package com.mas6y6.musmeta.utils;

import com.mas6y6.musmeta.core.Album;
import com.mas6y6.musmeta.core.Disc;
import com.mas6y6.musmeta.core.Song;
import com.mas6y6.musmeta.ui.dialogs.base.EXTDialog;
import org.jaudiotagger.audio.AudioFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Prompts for and normalizes mixed audio formats within scanned albums.
 * Source files are only removed after FFmpeg has successfully written their
 * replacement, and an existing target file is never overwritten.
 */
public final class AlbumFormatNormalizer {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlbumFormatNormalizer.class);

    private AlbumFormatNormalizer() {
    }

    public enum AudioFormat {
        FLAC("flac", "flac", List.of()),
        MP3("mp3", "libmp3lame", List.of("-q:a", "0")),
        M4A("m4a", "aac", List.of("-b:a", "256k")),
        OGG("ogg", "libvorbis", List.of("-q:a", "6"));

        private final String extension;
        private final String codec;
        private final List<String> codecArguments;

        AudioFormat(String extension, String codec, List<String> codecArguments) {
            this.extension = extension;
            this.codec = codec;
            this.codecArguments = codecArguments;
        }

        public String extension() {
            return extension;
        }

        private String codec() {
            return codec;
        }

        private List<String> codecArguments() {
            return codecArguments;
        }

        @Override
        public String toString() {
            return extension.toUpperCase(Locale.ROOT);
        }
    }

    public record ConversionResult(int converted, List<Path> failedFiles) {
        public boolean succeeded() {
            return failedFiles.isEmpty();
        }
    }

    /**
     * Prompts once per mixed-format album. Choosing "Convert all remaining"
     * applies the selected format to every later mixed-format album.
     */
    public static void promptToNormalizeMixedAlbums(Component parent, Collection<Album> albums) {
        Path ffmpeg = FFmpegUtils.getFFmpegExecutable();
        if (ffmpeg == null) {
            EXTDialog.showMessageDialog(parent,
                    "FFmpeg is required to convert album audio formats.",
                    "FFmpeg Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AudioFormat applyToAllFormat = null;
        for (Album album : albums) {
            Set<String> formats = extensionsIn(album);
            if (formats.size() < 2) {
                continue;
            }

            AudioFormat target = applyToAllFormat;
            if (target == null) {
                target = promptForFormat(parent, album, formats);
                if (target == null) {
                    continue;
                }

                int action = EXTDialog.showOptionDialog(parent,
                        "Convert the tracks in \"" + album.getTitle() + "\" to " + target
                                + "?\nOriginal files are replaced only after a successful conversion.",
                        "Normalize Album Format",
                        JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                        new Object[]{"Convert album", "Convert all remaining", "Skip album"},
                        "Convert album");
                if (action == 2 || action == JOptionPane.CLOSED_OPTION) {
                    continue;
                }
                if (action == 1) {
                    applyToAllFormat = target;
                }
            }

            ConversionResult result = normalize(album, target, ffmpeg);
            if (!result.succeeded()) {
                EXTDialog.showMessageDialog(parent,
                        "Converted " + result.converted() + " track(s) in \"" + album.getTitle()
                                + "\". " + result.failedFiles().size() + " track(s) could not be converted.",
                        "Some Tracks Were Not Converted", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Converts every track in {@code album} that is not already in
     * {@code targetFormat}. Callers can use this without displaying a prompt.
     */
    public static ConversionResult normalize(Album album, AudioFormat targetFormat) {
        Path ffmpeg = FFmpegUtils.getFFmpegExecutable();
        if (ffmpeg == null) {
            throw new IllegalStateException("FFmpeg is required to convert album audio formats.");
        }
        return normalize(album, targetFormat, ffmpeg);
    }

    private static AudioFormat promptForFormat(Component parent, Album album, Set<String> formats) {
        AudioFormat recommended = recommendedFormat(album, formats);
        JComboBox<AudioFormat> formatPicker = new JComboBox<>(AudioFormat.values());
        formatPicker.setSelectedItem(recommended);

        Object[] message = {
                "\"" + album.getTitle() + "\" contains " + formatDescription(formats) + ".",
                "Choose one format for its tracks:",
                formatPicker
        };
        int choice = EXTDialog.showConfirmDialog(parent, message, "Mixed Album Formats",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        return choice == JOptionPane.OK_OPTION ? (AudioFormat) formatPicker.getSelectedItem() : null;
    }

    private static ConversionResult normalize(Album album, AudioFormat targetFormat, Path ffmpeg) {
        int converted = 0;
        List<Path> failures = new ArrayList<>();
        for (Song song : songsIn(album)) {
            Path source = song.getAudioFile().getFile().toPath();
            if (extensionOf(source).equals(targetFormat.extension())) {
                continue;
            }

            try {
                convert(song, source, targetFormat, ffmpeg);
                converted++;
            } catch (Exception exception) {
                LOGGER.error("Could not convert {} to {}", source, targetFormat, exception);
                failures.add(source);
            }
        }
        return new ConversionResult(converted, List.copyOf(failures));
    }

    private static void convert(Song song, Path source, AudioFormat targetFormat, Path ffmpeg)
            throws Exception {
        String fileName = source.getFileName().toString();
        int extensionStart = fileName.lastIndexOf('.');
        String baseName = extensionStart > 0 ? fileName.substring(0, extensionStart) : fileName;
        Path target = source.resolveSibling(baseName + "." + targetFormat.extension());
        if (Files.exists(target)) {
            throw new IOException("Refusing to overwrite existing file: " + target);
        }

        Path temporary = Files.createTempFile(source.getParent(), baseName + ".musmeta-", "." + targetFormat.extension());
        try {
            List<String> command = new ArrayList<>(List.of(
                    ffmpeg.toString(), "-nostdin", "-y", "-v", "error", "-i", source.toString(),
                    "-map_metadata", "0", "-vn", "-c:a", targetFormat.codec()));
            command.addAll(targetFormat.codecArguments());
            command.add(temporary.toString());

            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String output = new String(process.getInputStream().readAllBytes());
            if (process.waitFor() != 0) {
                throw new IOException("FFmpeg failed: " + output);
            }

            AudioFileIO.read(temporary.toFile());
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            var convertedAudioFile = AudioFileIO.read(target.toFile());
            Files.delete(source);
            song.replaceAudioFile(convertedAudioFile);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static Set<String> extensionsIn(Album album) {
        Set<String> formats = new TreeSet<>();
        for (Song song : songsIn(album)) {
            formats.add(extensionOf(song.getAudioFile().getFile().toPath()));
        }
        return formats;
    }

    private static AudioFormat recommendedFormat(Album album, Set<String> formats) {
        Map<String, Integer> counts = new HashMap<>();
        for (Song song : songsIn(album)) {
            counts.merge(extensionOf(song.getAudioFile().getFile().toPath()), 1, Integer::sum);
        }
        String mostCommon = formats.stream()
                .max(Comparator.comparingInt(format -> counts.getOrDefault(format, 0)))
                .orElse("");
        return Arrays.stream(AudioFormat.values())
                .filter(format -> format.extension().equals(mostCommon))
                .findFirst()
                .orElse(AudioFormat.FLAC);
    }

    private static List<Song> songsIn(Album album) {
        return album.getDiscs().stream().map(Disc::getSongs).flatMap(Collection::stream).toList();
    }

    private static String extensionOf(Path file) {
        String name = file.getFileName().toString();
        int extensionStart = name.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == name.length() - 1) {
            return "(no extension)";
        }
        return name.substring(extensionStart + 1).toLowerCase(Locale.ROOT);
    }

    private static String formatDescription(Set<String> formats) {
        return formats.stream().map(format -> format.toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.joining(", "));
    }
}

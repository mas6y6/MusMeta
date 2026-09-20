package com.mas6y6.musmeta.audio;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FFmpegAudioStream implements AudioStream {

    private static final int SAMPLE_RATE = 44_100;
    private static final int SAMPLE_SIZE = 16;
    private static final int CHANNELS = 2;

    private static final Pattern DURATION_PATTERN =
            Pattern.compile("Duration:\\s*(\\d+):(\\d+):(\\d+(?:\\.\\d+)?)");

    private final Path ffmpeg;
    private final Path file;

    private final AudioFormat format = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            SAMPLE_RATE,
            SAMPLE_SIZE,
            CHANNELS,
            CHANNELS * (SAMPLE_SIZE / 8),
            SAMPLE_RATE,
            false
    );

    private Process process;
    private InputStream input;

    private Duration position = Duration.ZERO;
    private volatile Duration duration;
    private boolean closed = false;

    public FFmpegAudioStream(Path ffmpeg, Path file)
            throws IOException {

        this.ffmpeg = ffmpeg;
        this.file = file;

        startProcess(Duration.ZERO);
    }

    private void startProcess(Duration startPosition)
            throws IOException {

        double seconds = startPosition.toNanos() / 1_000_000_000.0;

        process = new ProcessBuilder(
                ffmpeg.toString(),

                "-v", "error",

                "-ss", String.valueOf(seconds),

                "-i", file.toString(),

                "-f", "s16le",
                "-ar", String.valueOf(SAMPLE_RATE),
                "-ac", String.valueOf(CHANNELS),

                "pipe:1"
        )
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();

        input = process.getInputStream();
        position = startPosition;
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public int read(byte[] buffer, int offset, int length)
            throws IOException {

        checkClosed();

        int read = input.read(buffer, offset, length);

        if (read > 0) {
            long frames = read / format.getFrameSize();

            long nanos = Math.round(
                    frames * 1_000_000_000.0
                            / format.getSampleRate()
            );

            position = position.plusNanos(nanos);
        }

        return read;
    }

    @Override
    public void seek(Duration position)
            throws IOException {

        checkClosed();

        if (position.isNegative()) {
            position = Duration.ZERO;
        }

        stopProcess();

        startProcess(position);
    }

    @Override
    public Duration getPosition() {
        return position;
    }

    @Override
    public Duration getDuration() {
        if (duration != null) {
            return duration;
        }

        try {
            Process probe = new ProcessBuilder(
                    ffmpeg.toString(),
                    "-i", file.toString()
            )
                    .redirectErrorStream(true)
                    .start();

            String output = new String(
                    probe.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            probe.waitFor();

            Matcher matcher = DURATION_PATTERN.matcher(output);
            if (matcher.find()) {
                long hours = Long.parseLong(matcher.group(1));
                long minutes = Long.parseLong(matcher.group(2));
                double seconds = Double.parseDouble(matcher.group(3));

                duration = Duration.ofNanos(Math.round(
                        (hours * 3600 + minutes * 60 + seconds)
                                * 1_000_000_000.0
                ));
            }
        } catch (Exception ignored) {
        }

        return duration;
    }

    @Override
    public void close() throws IOException {

        if (closed) {
            return;
        }

        closed = true;

        stopProcess();
    }

    private void stopProcess() throws IOException {

        IOException exception = null;

        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
                exception = e;
            } finally {
                input = null;
            }
        }

        if (process != null) {
            process.destroy();

            try {
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } finally {
                process = null;
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

    private void checkClosed() throws IOException {
        if (closed) {
            throw new IOException("Audio stream is closed");
        }
    }
}
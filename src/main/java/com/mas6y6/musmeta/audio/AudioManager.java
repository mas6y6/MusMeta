package com.mas6y6.musmeta.audio;

import com.mas6y6.musmeta.musicplayer.MusicPlayer;
import com.mas6y6.musmeta.settings.Settings;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class AudioManager {

    public interface Listener {
        default void onStart(AudioStream stream) {}

        default void onPosition(Duration position) {}

        default void onCompletion() {}

        default void onError(Exception error) {}
    }

    private static final int BUFFER_SIZE = 16 * 1024;
    private static final long POSITION_NOTIFY_NANOS = 200_000_000L;

    public static AudioManager instance;

    private final Object lock = new Object();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean playing = new AtomicBoolean(false);

    private volatile AudioStream stream;

    private volatile boolean paused;
    private volatile boolean stopped;
    private volatile Duration pendingSeek;
    private volatile Duration position = Duration.ZERO;
    private volatile Duration duration;

    private volatile SourceDataLine sourceDataLine;
    private volatile int volumePercent = Settings.MUSIC_PLAYER_VOLUME.get();

    private long lastPositionNotify;

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }

        return instance;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    /**
     * Plays the given stream until it ends, stopping whatever is currently
     * playing. Blocks on the calling thread until the stream finishes, is
     * stopped, or reports an error, so it can be used directly inside a
     * playback loop (e.g. MusicPlayer's track queue thread).
     */
    public void play(AudioStream stream) throws IOException {
        play(stream, null);
    }

    /**
     * Plays the given stream until it ends, stopping whatever is currently
     * playing. Blocks on the calling thread until playback is done. The
     * supplied listener is notified for the duration of this playback
     * session (in addition to any globally registered listeners).
     */
    public void play(AudioStream stream, Listener listener) throws IOException {
        Objects.requireNonNull(stream, "stream");

        stop();

        synchronized (lock) {
            this.stream = stream;
            this.stopped = false;
            this.paused = false;
            this.pendingSeek = null;
            this.position = stream.getPosition();
            this.duration = stream.getDuration();
        }

        if (listener != null) {
            listeners.add(listener);
        }

        playbackLoop();
    }

    /**
     * Seeks to the given position. Clamps negative values to the start.
     * Requested while paused is applied and the stream stays paused.
     */
    public void seek(Duration position) {
        Duration target = position != null && !position.isNegative()
                ? position
                : Duration.ZERO;

        synchronized (lock) {
            if (stream == null) {
                return;
            }
            pendingSeek = target;
            lock.notifyAll();
        }
    }

    /** Goes back to the start of the track. */
    public void seekToStart() {
        seek(Duration.ZERO);
    }

    /** Goes back from the current position by the given amount. */
    public void rewind(Duration amount) {
        if (amount == null || amount.isZero()) {
            return;
        }
        seek(position.minus(amount));
    }

    /** Goes forward from the current position by the given amount. */
    public void forward(Duration amount) {
        if (amount == null || amount.isZero()) {
            return;
        }
        seek(position.plus(amount));
    }

    public void pause() {
        if (playing.get()) {
            paused = true;
        }
    }

    public void resume() {
        synchronized (lock) {
            paused = false;
            lock.notifyAll();
        }
    }

    public void togglePause() {
        if (paused) {
            resume();
        } else {
            pause();
        }
    }

    public void stop() {
        synchronized (lock) {
            stopped = true;
            lock.notifyAll();
        }

        closeLine();

        AudioStream current = stream;
        if (current != null) {
            closeQuietly(current);
        }

        synchronized (lock) {
            stream = null;
            paused = false;
            pendingSeek = null;
        }

        playing.set(false);
    }

    public boolean isPlaying() {
        return playing.get();
    }

    public boolean isPaused() {
        return paused;
    }

    public Duration getPosition() {
        return position;
    }

    public Duration getDuration() {
        return duration;
    }

    /** Sets the output volume as a percentage (0 - 100). */
    public void setVolume(int percent) {
        volumePercent = Math.clamp(percent, 0, 100);

        Settings.MUSIC_PLAYER_VOLUME.set(volumePercent);

        SourceDataLine line = sourceDataLine;
        if (line != null && line.isOpen()) {
            applyVolume(line, volumePercent);
        }
    }

    public int getVolume() {
        return volumePercent;
    }

    private void playbackLoop() {
        AudioStream s = stream;
        SourceDataLine line = null;

        try {
            AudioFormat format = s.getFormat();

            line = AudioSystem.getSourceDataLine(format);
            line.open(format);
            sourceDataLine = line;
            applyVolume(line, volumePercent);
            line.start();

            playing.set(true);
            notifyStart(s);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (true) {
                synchronized (lock) {
                    if (stopped) {
                        break;
                    }

                    if (paused) {
                        line.stop();
                        while (paused && !stopped) {
                            lock.wait();
                        }
                        if (stopped) {
                            break;
                        }
                        line.start();
                    }
                }

                Duration pending;
                synchronized (lock) {
                    pending = pendingSeek;
                    pendingSeek = null;
                }

                if (pending != null) {
                    s.seek(pending);
                    line.flush();
                    setPosition(s.getPosition());
                    continue;
                }

                int read = s.read(buffer, 0, buffer.length);
                if (read == -1) {
                    break;
                }
                line.write(buffer, 0, read);
                setPosition(s.getPosition());
            }

            if (!stopped) {
                line.drain();
            }
            if (!stopped) {
                notifyCompletion();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            if (!stopped) {
                notifyError(e);
            }
        } finally {
            closeLine();
            closeQuietly(s);

            playing.set(false);

            synchronized (lock) {
                if (stream == s) {
                    stream = null;
                }
                lock.notifyAll();
            }
        }
    }

    private void setPosition(Duration position) {
        this.position = position;

        long now = System.nanoTime();
        if (now - lastPositionNotify >= POSITION_NOTIFY_NANOS) {
            lastPositionNotify = now;
            notifyPosition(position);
        }
    }

    private void closeLine() {
        SourceDataLine line = sourceDataLine;
        sourceDataLine = null;

        if (line != null) {
            try {
                line.stop();
            } catch (RuntimeException ignored) {
            }
            line.flush();
            line.close();
        }
    }

    private static void closeQuietly(AudioStream stream) {
        try {
            stream.close();
        } catch (IOException | RuntimeException ignored) {
        }
    }

    private static void applyVolume(SourceDataLine line, int percent) {
        try {
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float min = gain.getMinimum();
                gain.setValue(min + (gain.getMaximum() - min) * percent / 100f);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    private void notifyStart(AudioStream stream) {
        for (Listener listener : listeners) {
            listener.onStart(stream);
        }
    }

    private void notifyPosition(Duration position) {
        for (Listener listener : listeners) {
            listener.onPosition(position);
        }
    }

    private void notifyCompletion() {
        for (Listener listener : listeners) {
            listener.onCompletion();
        }
    }

    private void notifyError(Exception error) {
        for (Listener listener : listeners) {
            listener.onError(error);
        }
    }
}
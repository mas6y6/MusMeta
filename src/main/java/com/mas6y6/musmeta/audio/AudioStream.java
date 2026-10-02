package com.mas6y6.musmeta.audio;

import javax.sound.sampled.AudioFormat;
import java.io.IOException;
import java.time.Duration;

public interface AudioStream extends AutoCloseable {

    AudioFormat getFormat();

    int read(byte[] buffer, int offset, int length) throws IOException;

    void seek(Duration position) throws IOException;

    Duration getPosition();

    default Duration getDuration() {
        return null;
    }

    @Override
    void close() throws IOException;
}
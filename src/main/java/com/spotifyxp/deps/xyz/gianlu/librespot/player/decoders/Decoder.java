/*
 * Copyright 2021 devgianlu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spotifyxp.deps.xyz.gianlu.librespot.player.decoders;

import com.spotifyxp.logging.ConsoleLoggingModules;
import org.jetbrains.annotations.NotNull;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.mixing.output.OutputAudioFormat;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @author devgianlu
 */
public abstract class Decoder implements Closeable {
    public static final int BUFFER_SIZE = 2048;
    protected final SeekableInputStream audioIn;
    protected final float normalizationFactor;
    protected final int duration;
    protected volatile boolean closed = false;
    protected int seekZero = 0;
    private OutputAudioFormat format;

    public Decoder(@NotNull SeekableInputStream audioIn, float normalizationFactor, int duration) {
        this.audioIn = audioIn;
        this.duration = duration;
        this.normalizationFactor = normalizationFactor;
    }

    public final int writeSomeTo(@NotNull OutputStream out) throws IOException, DecoderException {
        return readInternal(out);
    }

    protected abstract int readInternal(@NotNull OutputStream out) throws IOException, DecoderException;

    /**
     * @return Time in millis
     * @throws CannotGetTimeException If the codec can't determine the time. This condition is permanent for the entire playback.
     */
    public abstract int time() throws CannotGetTimeException;

    @Override
    public void close() throws IOException {
        closed = true;
        audioIn.close();
    }

    public void seek(int positionMs) {
        if (positionMs < 0) positionMs = 0;

        try {
            audioIn.seek(seekZero);
            if (positionMs > 0) {
                int skip = Math.round(audioIn.available() / (float) duration * positionMs);
                if (skip > audioIn.available()) skip = audioIn.available();

                long skipped = audioIn.skip(skip);
                if (skip != skipped)
                    throw new IOException(String.format("Failed seeking, skip: %d, skipped: %d", skip, skipped));
            }
        } catch (IOException ex) {
            ConsoleLoggingModules.error("Failed seeking!", ex);
        }
    }

    @NotNull
    public final OutputAudioFormat getAudioFormat() {
        if (format == null) throw new IllegalStateException();
        return format;
    }

    protected final void setAudioFormat(@NotNull OutputAudioFormat format) {
        this.format = format;
    }

    protected final int sampleSizeBytes() {
        return getAudioFormat().getSampleSizeInBits() / 8;
    }

    public final int duration() {
        return duration;
    }

    public final int size() {
        return audioIn.size();
    }

    public static class CannotGetTimeException extends Exception {
        public CannotGetTimeException(String message) {
            super(message);
        }

        public CannotGetTimeException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DecoderException extends Exception {
        public DecoderException(String message) {
            super(message);
        }

        public DecoderException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

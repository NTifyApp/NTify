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

package com.spotifyxp.deps.xyz.gianlu.librespot.audio.storage;

import com.spotifyxp.deps.xyz.gianlu.librespot.audio.AbsChunkedInputStream;
import com.spotifyxp.deps.xyz.gianlu.librespot.cache.CacheManager;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.logging.ConsoleLoggingModules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.ByteBuffer;

import static com.spotifyxp.deps.xyz.gianlu.librespot.audio.storage.ChannelManager.CHUNK_SIZE;

/**
 * @author Gianlu
 */
public class AudioFileFetch implements AudioFile {
    public static final byte HEADER_SIZE = 0x3;
    public static final byte HEADER_CDN = 0x4;
    
    private final CacheManager.Handler cache;
    private int size = -1;
    private int chunks = -1;
    private volatile boolean closed = false;
    private AbsChunkedInputStream.ChunkException exception = null;

    AudioFileFetch(@Nullable CacheManager.Handler cache) {
        this.cache = cache;
    }

    @Override
    public void writeChunk(byte[] chunk, int chunkIndex, boolean cached) {
        if (chunkIndex != 0)
            throw new IllegalStateException("chunkIndex not zero: " + chunkIndex);
    }

    @Override
    public synchronized void writeHeader(int id, byte[] bytes, boolean cached) throws IOException {
        if (closed) return;

        if (!cached && cache != null) {
            try {
                cache.setHeader(id, bytes);
            } catch (IOException ex) {
                if (id == HEADER_SIZE) throw new IOException(ex);
                else
                    ConsoleLoggingModules.warning("Failed writing header to cache! {id: {}}", Utils.byteToHex((byte) id));
            }
        }

        if (id == HEADER_SIZE) {
            size = ByteBuffer.wrap(bytes).getInt();
            size *= 4;
            chunks = (size + CHUNK_SIZE - 1) / CHUNK_SIZE;

            exception = null;
            notifyAll();
        } else if (id == HEADER_CDN) {
            exception = new StorageNotAvailable(new String(bytes));
            notifyAll();
        }
    }

    @Override
    public synchronized void streamError(int chunkIndex, short code) {
        ConsoleLoggingModules.error("Stream error, index: {}, code: {}", chunkIndex, code);

        exception = AbsChunkedInputStream.ChunkException.fromStreamError(code);
        notifyAll();
    }

    synchronized void waitChunk() throws AbsChunkedInputStream.ChunkException {
        if (size != -1) return;

        try {
            exception = null;
            wait();

            if (exception != null)
                throw exception;
        } catch (InterruptedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static class StorageNotAvailable extends AbsChunkedInputStream.ChunkException {
        public final String cdnUrl;

        StorageNotAvailable(@NotNull String cdnUrl) {
            this.cdnUrl = cdnUrl;
        }
    }

    public int getSize() {
        if (size == -1) throw new IllegalStateException("Headers not received yet!");
        return size;
    }

    public int getChunks() {
        if (chunks == -1) throw new IllegalStateException("Headers not received yet!");
        return chunks;
    }

    @Override
    public void close() {
        closed = true;
    }
}

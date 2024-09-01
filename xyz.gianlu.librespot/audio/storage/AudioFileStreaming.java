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

import com.google.protobuf.ByteString;
import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.AbsChunkedInputStream;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.DecodedAudioStream;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.HaltListener;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.decrypt.AesAudioDecrypt;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.decrypt.AudioDecrypt;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.format.SuperAudioFormat;
import com.spotifyxp.deps.xyz.gianlu.librespot.cache.CacheManager;
import com.spotifyxp.deps.xyz.gianlu.librespot.cache.JournalHeader;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.NameThreadFactory;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.Session;
import com.spotifyxp.logging.ConsoleLoggingModules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author devgianlu
 */
public class AudioFileStreaming implements AudioFile, DecodedAudioStream {
    
    private final CacheManager.Handler cacheHandler;
    private final Metadata.AudioFile file;
    private final byte[] key;
    private final Session session;
    private final HaltListener haltListener;
    private final ExecutorService executorService = Executors.newCachedThreadPool(new NameThreadFactory(r -> "storage-async-" + r.hashCode()));
    private int chunks = -1;
    private ChunksBuffer chunksBuffer;

    AudioFileStreaming(@NotNull Session session, @NotNull Metadata.AudioFile file, byte[] key, @Nullable HaltListener haltListener) throws IOException {
        this.session = session;
        this.haltListener = haltListener;
        this.cacheHandler = session.cache().getHandler(Utils.bytesToHex(file.getFileId()));
        this.file = file;
        this.key = key;
    }

    @Override
    public @NotNull SuperAudioFormat codec() {
        return SuperAudioFormat.get(file.getFormat());
    }

    @Override
    public @NotNull String describe() {
        return "{fileId: " + Utils.bytesToHex(file.getFileId()) + "}";
    }

    @Override
    public int decryptTimeMs() {
        return chunksBuffer == null ? 0 : chunksBuffer.audioDecrypt.decryptTimeMs();
    }

    @NotNull
    public AbsChunkedInputStream stream() {
        if (chunksBuffer == null) throw new IllegalStateException("Stream not open!");
        return chunksBuffer.stream();
    }

    private void requestChunk(@NotNull ByteString fileId, int index, @NotNull AudioFile file) {
        if (cacheHandler == null || !tryCacheChunk(index)) {
            try {
                session.channel().requestChunk(fileId, index, file);
            } catch (IOException ex) {
                ConsoleLoggingModules.error("Failed requesting chunk from network, index: {}", index, ex);
                chunksBuffer.internalStream.notifyChunkError(index, new AbsChunkedInputStream.ChunkException(ex));
            }
        }
    }

    private boolean tryCacheChunk(int index) {
        try {
            if (!cacheHandler.hasChunk(index)) return false;
            cacheHandler.readChunk(index, this);
            return true;
        } catch (IOException | CacheManager.BadChunkHashException ex) {
            ConsoleLoggingModules.error("Failed requesting chunk from cache, index: {}", index, ex);
            return false;
        }
    }

    private boolean tryCacheHeaders(@NotNull AudioFileFetch fetch) throws IOException {
        List<JournalHeader> headers = cacheHandler.getAllHeaders();
        if (headers.isEmpty())
            return false;

        JournalHeader cdnHeader;
        if ((cdnHeader = JournalHeader.find(headers, AudioFileFetch.HEADER_CDN)) != null)
            throw new AudioFileFetch.StorageNotAvailable(new String(cdnHeader.value));

        for (JournalHeader header : headers)
            fetch.writeHeader(header.id, header.value, true);

        return true;
    }

    @NotNull
    private AudioFileFetch requestHeaders() throws IOException {
        AudioFileFetch fetch = new AudioFileFetch(cacheHandler);
        if (cacheHandler == null || !tryCacheHeaders(fetch))
            requestChunk(file.getFileId(), 0, fetch);

        fetch.waitChunk();
        return fetch;
    }

    void open() throws IOException {
        AudioFileFetch fetch = requestHeaders();
        int size = fetch.getSize();
        chunks = fetch.getChunks();
        chunksBuffer = new ChunksBuffer(size, chunks);
    }

    private void requestChunk(int index) {
        requestChunk(file.getFileId(), index, this);
        chunksBuffer.requested[index] = true; // Just to be sure
    }

    @Override
    public void writeChunk(byte[] buffer, int chunkIndex, boolean cached) throws IOException {
        if (!cached && cacheHandler != null) {
            try {
                cacheHandler.writeChunk(buffer, chunkIndex);
            } catch (IOException ex) {
                ConsoleLoggingModules.warning("Failed writing to cache! {index: {}}", chunkIndex, ex);
            }
        }

        chunksBuffer.writeChunk(buffer, chunkIndex);
        if(PublicValues.disableChunkDebug) return;
        ConsoleLoggingModules.debug("Chunk {}/{} completed, cached: {}, fileId: {}", chunkIndex, chunks, cached, Utils.bytesToHex(file.getFileId()));
    }

    @Override
    public void writeHeader(int id, byte[] bytes, boolean cached) {
        // Not interested
    }

    @Override
    public void streamError(int chunkIndex, short code) {
        ConsoleLoggingModules.error("Stream error, index: {}, code: {}", chunkIndex, code);
        chunksBuffer.internalStream.notifyChunkError(chunkIndex, AbsChunkedInputStream.ChunkException.fromStreamError(code));
    }

    @Override
    public void close() {
        executorService.shutdown();
        if (chunksBuffer != null)
            chunksBuffer.close();

        if (cacheHandler != null) {
            try {
                cacheHandler.close();
            } catch (IOException ignored) {
            }
        }
    }

    @SuppressWarnings("NullableProblems")
    private class ChunksBuffer implements Closeable {
        private final int size;
        private final byte[][] buffer;
        private final boolean[] available;
        private final boolean[] requested;
        private final AudioDecrypt audioDecrypt;
        private final InternalStream internalStream;

        ChunksBuffer(int size, int chunks) {
            this.size = size;
            this.buffer = new byte[chunks][];
            this.available = new boolean[chunks];
            this.requested = new boolean[chunks];
            this.audioDecrypt = new AesAudioDecrypt(key);
            this.internalStream = new InternalStream(session.configuration().retryOnChunkError);
        }

        void writeChunk(@NotNull byte[] chunk, int chunkIndex) throws IOException {
            if (internalStream.isClosed()) return;

            if (chunk.length != buffer[chunkIndex].length)
                throw new IllegalArgumentException(String.format("Buffer size mismatch, required: %d, received: %d, index: %d", buffer[chunkIndex].length, chunk.length, chunkIndex));

            buffer[chunkIndex] = chunk;
            audioDecrypt.decryptChunk(chunkIndex, chunk);
            internalStream.notifyChunkAvailable(chunkIndex);
        }

        @NotNull
        AbsChunkedInputStream stream() {
            return internalStream;
        }

        @Override
        public void close() {
            internalStream.close();
            AudioFileStreaming.this.close();
        }

        private class InternalStream extends AbsChunkedInputStream {

            private InternalStream(boolean retryOnChunkError) {
                super(retryOnChunkError);
            }

            @Override
            protected byte[][] buffer() {
                return buffer;
            }

            @Override
            public int size() {
                return size;
            }

            @Override
            protected boolean[] requestedChunks() {
                return requested;
            }

            @Override
            protected boolean[] availableChunks() {
                return available;
            }

            @Override
            protected int chunks() {
                return chunks;
            }

            @Override
            protected void requestChunkFromStream(int index) {
                executorService.submit(() -> requestChunk(index));
            }

            @Override
            public void streamReadHalted(int chunk, long time) {
                if (haltListener != null) executorService.submit(() -> haltListener.streamReadHalted(chunk, time));
            }

            @Override
            public void streamReadResumed(int chunk, long time) {
                if (haltListener != null) executorService.submit(() -> haltListener.streamReadResumed(chunk, time));
            }
        }
    }
}

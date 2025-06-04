/*
 * Copyright [2023-2025] [Gianluca Beil]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spotifyxp.testing;

import com.spotifyxp.Flags;
import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.configuration.Config;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.AbsChunkedInputStream;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.DecodedAudioStream;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.decoders.VorbisDecoder;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.format.SuperAudioFormat;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.storage.AudioFileStreaming;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.NameThreadFactory;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.PlayerConfiguration;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.decoders.Decoder;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.decoders.SeekableInputStream;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.metrics.PlaybackMetrics;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.mixing.AudioSink;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.mixing.MixingLine;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.mixing.output.OutputAudioFormat;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.ArtistEventView;
import com.spotifyxp.lib.libLanguage;
import com.spotifyxp.logging.ConsoleLoggingModules;
import com.spotifyxp.protogens.Concert;
import com.spotifyxp.support.LinuxSupportModule;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.theming.themes.DarkGreen;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.spotifyxp.deps.xyz.gianlu.librespot.audio.storage.ChannelManager.CHUNK_SIZE;

public class Test {
    private static class FileAudioStream implements DecodedAudioStream {
        private final File file;
        private final RandomAccessFile raf;
        private final byte[][] buffer;
        private final int chunks;
        private final int size;
        private final boolean[] available;
        private final boolean[] requested;
        private final ExecutorService executorService = Executors.newCachedThreadPool(new NameThreadFactory((r) -> "file-async-" + r.hashCode()));

        FileAudioStream(File file) throws IOException {
            this.file = file;
            this.raf = new RandomAccessFile(file, "r");

            this.size = (int) raf.length();
            this.chunks = (size + CHUNK_SIZE - 1) / CHUNK_SIZE;
            this.buffer = new byte[chunks][];
            this.available = new boolean[chunks];
            this.requested = new boolean[chunks];
        }

        @Override
        public @NotNull AbsChunkedInputStream stream() {
            return new AbsChunkedInputStream(false) {
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
                    executorService.submit(() -> {
                        try {
                            raf.seek((long) index * CHUNK_SIZE);
                            raf.read(buffer[index]);
                            notifyChunkAvailable(index);
                        } catch (IOException ex) {
                            notifyChunkError(index, new ChunkException(ex));
                        }
                    });
                }

                @Override
                public void streamReadHalted(int chunk, long time) {
                    ConsoleLoggingModules.warning("Not dispatching stream read halted event {chunk: {}}", chunk);
                }

                @Override
                public void streamReadResumed(int chunk, long time) {
                    ConsoleLoggingModules.warning("Not dispatching stream read resumed event {chunk: {}}", chunk);
                }
            };
        }

        @Override
        public @NotNull SuperAudioFormat codec() {
            return SuperAudioFormat.MP3; // FIXME: Detect codec
        }

        @Override
        public @NotNull String describe() {
            return "{file: " + file.getAbsolutePath() + "}";
        }

        @Override
        public int decryptTimeMs() {
            return 0;
        }
    }

    public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException, InterruptedException, URISyntaxException, ClassNotFoundException, Decoder.DecoderException {

    }
}

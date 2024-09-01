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

package com.spotifyxp.deps.xyz.gianlu.librespot.audio;

import com.google.protobuf.ByteString;
import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.PacketsReceiver;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.Session;
import com.spotifyxp.deps.xyz.gianlu.librespot.crypto.Packet;
import com.spotifyxp.logging.ConsoleLoggingModules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gianlu
 */
@SuppressWarnings("NullableProblems")
public final class AudioKeyManager implements PacketsReceiver {
    private static final byte[] ZERO_SHORT = new byte[]{0, 0};
    private static final long AUDIO_KEY_REQUEST_TIMEOUT = 2000;
    private final AtomicInteger seqHolder = new AtomicInteger(0);
    private final Map<Integer, Callback> callbacks = Collections.synchronizedMap(new HashMap<>());
    private final Session session;

    public AudioKeyManager(@NotNull Session session) {
        this.session = session;
    }

    @NotNull
    public byte[] getAudioKey(@NotNull ByteString gid, @NotNull ByteString fileId) throws IOException {
        return getAudioKey(gid, fileId, true);
    }

    @NotNull
    private byte[] getAudioKey(@NotNull ByteString gid, @NotNull ByteString fileId, boolean retry) throws IOException {
        int seq;
        synchronized (seqHolder) {
            seq = seqHolder.getAndIncrement();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        fileId.writeTo(out);
        gid.writeTo(out);
        out.write(Utils.toByteArray(seq));
        out.write(ZERO_SHORT);

        session.send(Packet.Type.RequestKey, out.toByteArray());

        SyncCallback callback = new SyncCallback();
        callbacks.put(seq, callback);

        byte[] key = callback.waitResponse();
        if (key == null) {
            if (retry) return getAudioKey(gid, fileId, false);
            else {
                PublicValues.spotifyplayer.next();
                throw new AesKeyException(String.format("Failed fetching audio key! {gid: %s, fileId: %s}",
                        Utils.bytesToHex(gid), Utils.bytesToHex(fileId)));
            }
        }

        return key;
    }

    @Override
    public void dispatch(@NotNull Packet packet) {
        ByteBuffer payload = ByteBuffer.wrap(packet.payload);
        int seq = payload.getInt();

        Callback callback = callbacks.remove(seq);
        if (callback == null) {
            ConsoleLoggingModules.warning("Couldn't find callback for seq: " + seq);
            return;
        }

        if (packet.is(Packet.Type.AesKey)) {
            byte[] key = new byte[16];
            payload.get(key);
            callback.key(key);
        } else if (packet.is(Packet.Type.AesKeyError)) {
            short code = payload.getShort();
            callback.error(code);
        } else {
            ConsoleLoggingModules.warning("Couldn't handle packet, cmd: " + packet.type() + ", length: " + packet.payload.length);
        }
    }

    private interface Callback {
        void key(byte[] key);

        void error(short code);
    }

    @SuppressWarnings("NullableProblems")
    private static class SyncCallback implements Callback {
        private final AtomicReference<byte[]> reference = new AtomicReference<>();

        @Override
        public void key(byte[] key) {
            synchronized (reference) {
                reference.set(key);
                reference.notifyAll();
            }
        }

        @Override
        public void error(short code) {
            ConsoleLoggingModules.error("Audio key error, code: " + code);

            synchronized (reference) {
                reference.set(null);
                reference.notifyAll();
            }
        }

        @Nullable
        byte[] waitResponse() throws IOException {
            synchronized (reference) {
                try {
                    reference.wait(AUDIO_KEY_REQUEST_TIMEOUT);
                    return reference.get();
                } catch (InterruptedException ex) {
                    throw new IOException(ex); // Wrapping to avoid cluttering the call stack
                }
            }
        }
    }

    public static class AesKeyException extends IOException {
        AesKeyException(String message) {
            super(message);
        }
    }
}

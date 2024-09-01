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

package com.spotifyxp.deps.xyz.gianlu.librespot.dacp;

import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.logging.ConsoleLoggingModules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Metadata pipe implementation following the Shairport Sync format (https://github.com/mikebrady/shairport-sync-metadata-reader).
 *
 * @author devgianlu
 */
@SuppressWarnings({"SameParameterValue", "NullableProblems", "JavadocLinkAsPlainText"})
public final class DacpMetadataPipe implements Closeable {
    private static final String TYPE_SSNC = "73736e63";
    private static final String TYPE_CORE = "636f7265";
    private static final String CODE_ASAR = "61736172";
    private static final String CODE_ASAL = "6173616c";
    private static final String CODE_MINM = "6d696e6d";
    private static final String CODE_PVOL = "70766f6c";
    private static final String CODE_PRGR = "70726772";
    private static final String CODE_PICT = "50494354";
    private static final String CODE_PFLS = "70666C73";
    
    private final File file;
    private FileOutputStream out;

    public DacpMetadataPipe(@NotNull File file) {
        this.file = file;
    }

    private void safeSend(@NotNull String type, @NotNull String code) {
        safeSend(type, code, (String) null);
    }

    private void safeSend(@NotNull String type, @NotNull String code, @Nullable String payload) {
        safeSend(type, code, payload == null ? null : payload.getBytes(StandardCharsets.UTF_8));
    }

    private void safeSend(@NotNull String type, @NotNull String code, @Nullable byte[] payload) {
        try {
            send(type, code, payload);
        } catch (IOException ex) {
            //ConsoleLoggingModules.error("Failed sending metadata through pipe! ", ex.getMessage());
            //This is normal it should not prevent SpotifyXP from working
        }
    }

    private synchronized void send(@NotNull String type, @NotNull String code, @Nullable byte[] payload) throws IOException {
        if (out == null) out = new FileOutputStream(file);

        if (payload != null && payload.length > 0) {
            out.write(String.format("<item><type>%s</type><code>%s</code><length>%d</length>\n<data encoding=\"base64\">%s</data></item>\n", type, code,
                    payload.length, Utils.toBase64(payload)).getBytes(StandardCharsets.UTF_8));
        } else {
            out.write(String.format("<item><type>%s</type><code>%s</code><length>0</length></item>\n", type, code).getBytes(StandardCharsets.UTF_8));
        }
    }

    public void sendImage(byte[] image) {
        if (image == null) {
            ConsoleLoggingModules.warning("No image found in metadata.");
            return;
        }

        safeSend(TYPE_SSNC, CODE_PICT, image);
    }

    public void sendProgress(float currentTime, float duration, float sampleRate) {
        safeSend(TYPE_SSNC, CODE_PRGR, String.format("1/%.0f/%.0f", currentTime * sampleRate / 1000 + 1, duration * sampleRate / 1000 + 1));
    }

    public void sendTrackInfo(String name, String albumName, String artist) {
        safeSend(TYPE_CORE, CODE_MINM, name);
        safeSend(TYPE_CORE, CODE_ASAL, albumName);
        safeSend(TYPE_CORE, CODE_ASAR, artist);
    }

    public void sendVolume(@Range(from = 0, to = 1) float value) {
        float xmlValue;
        if (value == 0) xmlValue = -144.0f;
        else xmlValue = (value - 1) * 30.0f;
        String volData = String.format("%.2f,0.00,0.00,0.00", xmlValue);
        safeSend(TYPE_SSNC, CODE_PVOL, volData);
    }

    public void sendPipeFlush() {
        safeSend(TYPE_CORE, CODE_PFLS);
    }

    @Override
    public void close() throws IOException {
        if (out != null) out.close();
    }
}

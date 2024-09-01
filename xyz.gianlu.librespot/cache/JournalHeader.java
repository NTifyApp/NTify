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

package com.spotifyxp.deps.xyz.gianlu.librespot.cache;

import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Gianlu
 */
public final class JournalHeader {
    public final int id;
    public final byte[] value;

    JournalHeader(int id, @NotNull String value) {
        this.id = id;
        this.value = Utils.hexToBytes(value);
    }

    @Nullable
    public static JournalHeader find(List<JournalHeader> headers, byte id) {
        for (JournalHeader header : headers)
            if (header.id == id)
                return header;

        return null;
    }

    @Override
    public String toString() {
        return "JournalHeader{" + "id=" + id + ", value=" + Utils.bytesToHex(value) + '}';
    }
}

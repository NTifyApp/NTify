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

package com.spotifyxp.deps.xyz.gianlu.librespot.metadata;

import com.spotifyxp.deps.xyz.gianlu.librespot.common.Base62;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Gianlu
 */
public final class AlbumId implements SpotifyId {
    private static final Pattern PATTERN = Pattern.compile("spotify:album:(.{22})");
    private static final Base62 BASE62 = Base62.createInstanceWithInvertedCharacterSet();
    private final String hexId;

    private AlbumId(@NotNull String hex) {
        this.hexId = hex.toLowerCase();
    }

    @NotNull
    public static AlbumId fromUri(@NotNull String uri) {
        Matcher matcher = PATTERN.matcher(uri);
        if (matcher.find()) {
            String id = matcher.group(1);
            return new AlbumId(Utils.bytesToHex(BASE62.decode(id.getBytes(), 16)));
        } else {
            throw new IllegalArgumentException("Not a Spotify album ID: " + uri);
        }
    }

    @NotNull
    public static AlbumId fromBase62(@NotNull String base62) {
        return new AlbumId(Utils.bytesToHex(BASE62.decode(base62.getBytes(), 16)));
    }

    @NotNull
    public static AlbumId fromHex(@NotNull String hex) {
        return new AlbumId(hex);
    }

    public @NotNull String toMercuryUri() {
        return "hm://metadata/4/album/" + hexId;
    }

    @Override
    public @NotNull String toSpotifyUri() {
        return "spotify:album:" + new String(BASE62.encode(Utils.hexToBytes(hexId)));
    }

    public @NotNull String hexId() {
        return hexId;
    }
}

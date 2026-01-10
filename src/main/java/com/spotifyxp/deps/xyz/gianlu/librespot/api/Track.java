/*
 * Copyright 2021 devgianlu
 * Copyright [2026] [Gianluca Beil]
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

package com.spotifyxp.deps.xyz.gianlu.librespot.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotifyxp.deps.com.spotify.canvaz.CanvazOuterClass;
import com.spotifyxp.deps.com.spotify.collection2.v2.proto.Collection;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.PlayableId;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.TrackId;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class Track {
    private final ApiClient apiClient;

    protected Track(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @NotNull
    public Metadata.Track getMetadata(@NotNull TrackId track) throws IOException, MercuryClient.MercuryException {
        ExtendedMetadata.BatchedExtensionResponse response = apiClient.getExtendedMetadata(ExtensionKindOuterClass.ExtensionKind.TRACK_V4, track);

        apiClient.checkExtendedMetadataResponse(response);

        return Metadata.Track.parseFrom(response.getExtendedMetadata(0).getExtensionData(0).getExtensionData().getValue());
    }

    @NotNull
    public CanvazOuterClass.EntityCanvazResponse getCanvases(@NotNull CanvazOuterClass.EntityCanvazRequest req) throws IOException, MercuryClient.MercuryException {
        try (Response resp = apiClient.send("POST", "/canvaz-cache/v0/canvases", null, ApiClient.protoBody(req))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return CanvazOuterClass.EntityCanvazResponse.parseFrom(body.byteStream());
        }
    }

    @NotNull
    public JsonObject getRadioForTrack(@NotNull PlayableId id) throws IOException, MercuryClient.MercuryException {
        try (Response resp = apiClient.send("GET", "/inspiredby-mix/v2/seed_to_playlist/" + id.toSpotifyUri() + "?response-format=json", null, null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject();
        }
    }

    @NotNull
    public JsonObject getLyrics(@NotNull String uri, boolean vocalRemoval) throws IOException, MercuryClient.MercuryException {
        try (Response resp = apiClient.send("GET", "https://spclient.wg.spotify.com/color-lyrics/v2/track/" + uri.split(":")[2] + "?format=json&vocalRemoval=" + vocalRemoval, new Headers.Builder()
                .add("App-Platform", "Win32")
                .build(), null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject();
        }
    }

    public void like(TrackId trackId) throws IOException, MercuryClient.MercuryException {
        Collection.WriteRequest request = Collection.WriteRequest.newBuilder()
                .setUsername(apiClient.session.username())
                .setSet("collection")
                .addItems(Collection.CollectionItem.newBuilder()
                        .setUri(trackId.toSpotifyUri())
                        .setAddedAt((int) (System.currentTimeMillis() / 1000))
                        .build())
                .setClientUpdateId(String.format("%016x", apiClient.session.secureRandom().nextLong()))
                .build();

        try (Response resp = apiClient.send("POST", "/collection/v2/write", null, ApiClient.protoBody(request))) {
            ApiClient.StatusCodeException.checkStatus(resp);
        }
    }

    public void remove(TrackId trackId) throws IOException, MercuryClient.MercuryException {
        Collection.WriteRequest request = Collection.WriteRequest.newBuilder()
                .setUsername(apiClient.session.username())
                .setSet("collection")
                .addItems(Collection.CollectionItem.newBuilder()
                        .setUri(trackId.toSpotifyUri())
                        .setIsRemoved(true)
                        .build())
                .setClientUpdateId(String.format("%016x", apiClient.session.secureRandom().nextLong()))
                .build();

        try (Response resp = apiClient.send("POST", "/collection/v2/write", null, ApiClient.protoBody(request))) {
            ApiClient.StatusCodeException.checkStatus(resp);
        }
    }
}

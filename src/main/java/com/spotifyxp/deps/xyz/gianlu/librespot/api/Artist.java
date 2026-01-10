/*
 * Copyright 2021 devgianlu
 * Copyright [2026] [Gianluca Beil]
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

package com.spotifyxp.deps.xyz.gianlu.librespot.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.spotifyxp.deps.com.spotify.collection2.v2.proto.Collection;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.ArtistId;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Locale;

public class Artist {
    private final ApiClient apiClient;

    protected Artist(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @NotNull
    public Metadata.Artist getMetadata(@NotNull ArtistId artist) throws IOException, MercuryClient.MercuryException {
        ExtendedMetadata.BatchedExtensionResponse response = apiClient.getExtendedMetadata(ExtensionKindOuterClass.ExtensionKind.ARTIST_V4, artist);

        apiClient.checkExtendedMetadataResponse(response);

        return Metadata.Artist.parseFrom(response.getExtendedMetadata(0).getExtensionData(0).getExtensionData().getValue());
    }

    @NotNull
    public JsonObject getArtistDiscoveredOn(String uri) throws IOException, MercuryClient.MercuryException {
        JsonObject variables = new JsonObject();
        variables.add("uri", new JsonPrimitive(uri));
        String url = apiClient.appendQueryHash("https://api-partner.spotify.com/pathfinder/v1/query?operationName=queryArtistDiscoveredOn", "queryArtistDiscoveredOn", variables);

        try (Response resp = apiClient.send("GET", url, new Headers.Builder()
                .add("App-Platform", "Win32")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .add("Accept", "application/json")
                .build(), null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("artistUnion").getAsJsonObject("relatedContent").getAsJsonObject("discoveredOnV2");
        }
    }

    @NotNull
    public JsonObject getArtistRelatedArtists(String uri) throws IOException, MercuryClient.MercuryException {
        JsonObject variables = new JsonObject();
        variables.add("uri", new JsonPrimitive(uri));
        String url = apiClient.appendQueryHash("https://api-partner.spotify.com/pathfinder/v1/query?operationName=queryArtistRelated", "queryArtistRelated", variables);

        try (Response resp = apiClient.send("GET", url, new Headers.Builder()
                .add("App-Platform", "Win32")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .add("Accept", "application/json")
                .build(), null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("artistUnion").getAsJsonObject("relatedContent").getAsJsonObject("relatedArtists");
        }
    }

    @NotNull
    public JsonObject getArtistHeaderImage(String uri) throws IOException, MercuryClient.MercuryException {
        JsonObject variables = new JsonObject();
        variables.add("uri", new JsonPrimitive(uri));
        String url = apiClient.appendQueryHash("https://api-partner.spotify.com/pathfinder/v1/query?operationName=queryArtistOverview", "queryArtistOverview", variables);

        try (Response resp = apiClient.send("GET", url, new Headers.Builder()
                .add("App-Platform", "Win32")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .add("Accept", "application/json")
                .build(), null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject().getAsJsonObject("data").getAsJsonObject("artistUnion").getAsJsonObject("headerImage");
        }
    }

    public void follow(ArtistId artistId) throws IOException, MercuryClient.MercuryException {
        Collection.WriteRequest request = Collection.WriteRequest.newBuilder()
                .setUsername(apiClient.session.username())
                .setSet("artist")
                .addItems(Collection.CollectionItem.newBuilder()
                        .setUri(artistId.toSpotifyUri())
                        .setAddedAt((int) (System.currentTimeMillis() / 1000))
                        .build())
                .setClientUpdateId(String.format("%016x", apiClient.session.secureRandom().nextLong()))
                .build();

        try (Response resp = apiClient.send("POST", "/collection/v2/write", null, ApiClient.protoBody(request))) {
            ApiClient.StatusCodeException.checkStatus(resp);
        }
    }

    public void unfollow(ArtistId artistId) throws IOException, MercuryClient.MercuryException {
        Collection.WriteRequest request = Collection.WriteRequest.newBuilder()
                .setUsername(apiClient.session.username())
                .setSet("artist")
                .addItems(Collection.CollectionItem.newBuilder()
                        .setUri(artistId.toSpotifyUri())
                        .setIsRemoved(true)
                        .build())
                .setClientUpdateId(String.format("%016x", apiClient.session.secureRandom().nextLong()))
                .build();

        try (Response resp = apiClient.send("POST", "/collection/v2/write", null, ApiClient.protoBody(request))) {
            ApiClient.StatusCodeException.checkStatus(resp);
        }
    }
}

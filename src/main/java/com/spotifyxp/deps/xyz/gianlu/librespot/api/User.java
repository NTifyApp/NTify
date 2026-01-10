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

import com.google.gson.*;
import com.spotifyxp.deps.com.spotify.collection2.v2.proto.Collection;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Locale;

public class User {
    private final ApiClient apiClient;

    protected User(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @NotNull
    public JsonObject getUserProfile(@NotNull String id, @Nullable Integer playlistLimit, @Nullable Integer artistLimit) throws IOException, MercuryClient.MercuryException {
        StringBuilder url = new StringBuilder();
        url.append("/user-profile-view/v3/profile/");
        url.append(id);

        if (playlistLimit != null || artistLimit != null) {
            url.append("?");

            if (playlistLimit != null) {
                url.append("playlist_limit=");
                url.append(playlistLimit);
                if (artistLimit != null)
                    url.append("&");
            }

            if (artistLimit != null) {
                url.append("artist_limit=");
                url.append(artistLimit);
            }
        }

        try (Response resp = apiClient.send("GET", url.toString(), null, null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject();
        }
    }

    @NotNull
    public JsonObject getFollowers(@NotNull String id) throws IOException, MercuryClient.MercuryException {
        try (Response resp = apiClient.send("GET", "/user-profile-view/v3/profile/" + id + "/followers", null, null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject();
        }
    }

    @NotNull
    public JsonObject getHome() throws IOException, MercuryClient.MercuryException {
        JsonObject variables = new JsonObject();
        variables.add("timeZone", new JsonPrimitive(ZoneId.systemDefault().toString()));
        String url = apiClient.appendQueryHash("https://api-partner.spotify.com/pathfinder/v1/query?operationName=home", "home", variables);

        try (Response resp = apiClient.send("GET", url, null, null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject();
        }
    }

    @NotNull
    public JsonObject getLibrary(String[] filters, String[] features, int limit, int offset) throws IOException, MercuryClient.MercuryException {
        if (filters == null) filters = new String[]{};
        if (features == null) features = new String[]{};
        JsonObject variables = new JsonObject();
        Gson gson = new Gson();
        variables.add("filters", gson.toJsonTree(Arrays.asList(filters)));
        variables.add("order", null);
        variables.add("textFilter", new JsonPrimitive(""));
        variables.add("features", gson.toJsonTree(Arrays.asList(features)));
        variables.add("limit", new JsonPrimitive(limit));
        variables.add("offset", new JsonPrimitive(offset));
        variables.add("flatten", new JsonPrimitive(false));
        variables.add("expandedFolders", new JsonArray());
        variables.add("folderUri", null);
        variables.add("includeFoldersWhenFlattening", new JsonPrimitive(true));

        try (Response resp = apiClient.send("POST", "https://api-partner.spotify.com/pathfinder/v1/query", new Headers.Builder()
                .add("App-Platform", "Win32")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .add("Accept", "application/json")
                .add("Content-Encoding", "")
                .build(), RequestBody.create(apiClient.getQuery("libraryV3", variables).toString(), MediaType.get("application/json")))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject();
        }
    }

    @NotNull
    public JsonObject getLibraryTracks(int limit, int offset) throws IOException, MercuryClient.MercuryException {
        JsonObject variables = new JsonObject();
        variables.add("limit", new JsonPrimitive(limit));
        variables.add("offset", new JsonPrimitive(offset));
        try (Response resp = apiClient.send("POST", "https://api-partner.spotify.com/pathfinder/v1/query", new Headers.Builder()
                .add("App-Platform", "Win32")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .add("Accept", "application/json")
                .add("Content-Encoding", "")
                .build(), RequestBody.create(apiClient.getQuery("fetchLibraryTracks", variables).toString(), MediaType.get("application/json")))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject();
        }
    }

    @NotNull
    public JsonObject getUserFollowing(@NotNull String id) throws IOException, MercuryClient.MercuryException {
        try (Response resp = apiClient.send("GET", "/user-profile-view/v3/profile/" + id + "/following", null, null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject();
        }
    }

    public boolean[] isInLibrary(@NotNull String[] uris) throws IOException, MercuryClient.MercuryException {
        JsonObject variables = new JsonObject();
        JsonArray urisJSON = new JsonArray();

        for (String uri : uris) {
            urisJSON.add(new JsonPrimitive(uri));
        }
        variables.add("uris", urisJSON);

        try (Response resp = apiClient.send("POST", "https://api-partner.spotify.com/pathfinder/v2/query", new Headers.Builder()
                .add("App-Platform", "Win32")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .add("Accept", "application/json")
                .add("Content-Encoding", "")
                .build(), RequestBody.create(apiClient.getQuery("areEntitiesInLibrary", variables).toString(), MediaType.get("application/json")))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            JsonArray array = JsonParser.parseReader(body.charStream()).getAsJsonObject().getAsJsonObject("data").getAsJsonArray("lookup");

            boolean[] results = new boolean[array.size()];
            for (int i = 0; i < array.size(); i++) {
                results[i] = array.get(i).getAsJsonObject().getAsJsonObject("data").get("saved").getAsBoolean();
            }
            return results;
        }
    }
}

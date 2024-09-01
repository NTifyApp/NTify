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

package com.spotifyxp.deps.xyz.gianlu.librespot.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.RawMercuryRequest;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author Gianlu
 */
public final class SearchManager {
    private static final String BASE_URL = "hm://searchview/km/v4/search/";
    private final Session session;

    public SearchManager(@NotNull Session session) {
        this.session = session;
    }

    @NotNull
    public JsonObject request(@NotNull SearchRequest req) throws IOException {
        if (req.username.isEmpty()) req.username = session.username();
        if (req.country.isEmpty()) req.country = session.countryCode();
        if (req.locale.isEmpty()) req.locale = session.preferredLocale();

        MercuryClient.Response resp = session.mercury().sendSync(RawMercuryRequest.newBuilder()
                .setMethod("GET").setUri(req.buildUrl()).build());

        if (resp.statusCode != 200) throw new SearchException(resp.statusCode);

        try (Reader reader = new InputStreamReader(resp.payload.stream())) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    public static class SearchException extends IOException {

        SearchException(int statusCode) {
            super(String.format("Search failed with code %d.", statusCode));
        }
    }

    public static class SearchRequest {
        private final String query;
        private int limit = 10;
        private String imageSize = "";
        private String catalogue = "";
        private String country = "";
        private String locale = "";
        private String username = "";

        public SearchRequest(@NotNull String query) {
            this.query = query.trim();

            if (this.query.isEmpty())
                throw new IllegalArgumentException();
        }

        @NotNull
        private String buildUrl() throws UnsupportedEncodingException {
            String url = BASE_URL + URLEncoder.encode(query, "UTF-8");
            url += "?entityVersion=2";
            url += "&limit=" + limit;
            url += "&imageSize=" + URLEncoder.encode(imageSize, "UTF-8");
            url += "&catalogue=" + URLEncoder.encode(catalogue, "UTF-8");
            url += "&country=" + URLEncoder.encode(country, "UTF-8");
            url += "&locale=" + URLEncoder.encode(locale, "UTF-8");
            url += "&username=" + URLEncoder.encode(username, "UTF-8");
            return url;
        }

        @NotNull
        public SearchRequest imageSize(@NotNull String imageSize) {
            this.imageSize = imageSize;
            return this;
        }

        @NotNull
        public SearchRequest catalogue(@NotNull String catalogue) {
            this.catalogue = catalogue;
            return this;
        }

        @NotNull
        public SearchRequest country(@NotNull String country) {
            this.country = country;
            return this;
        }

        @NotNull
        public SearchRequest locale(@NotNull String locale) {
            this.locale = locale;
            return this;
        }

        @NotNull
        public SearchRequest username(@NotNull String username) {
            this.username = username;
            return this;
        }

        @NotNull
        public SearchRequest limit(int limit) {
            this.limit = limit;
            return this;
        }
    }
}

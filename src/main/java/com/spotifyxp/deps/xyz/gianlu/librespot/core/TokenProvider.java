/*
 * Copyright 2022 devgianlu
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.spotifyxp.deps.com.spotify.login5v3.Credentials;
import com.spotifyxp.deps.com.spotify.login5v3.Login5;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.logging.ConsoleLoggingModules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Gianlu
 */
public final class TokenProvider {
    private final static int TOKEN_EXPIRE_THRESHOLD = 10;
    private final Session session;
    private final List<StoredToken> tokens = new ArrayList<>();
    private final String[] availableScopes = new String[] {
            "ugc-image-upload",
            "user-read-playback-state",
            "user-modify-playback-state",
            "user-read-currently-playing",
            "app-remote-control",
            "streaming",
            "playlist-read-private",
            "playlist-read-collaborative",
            "playlist-modify-private",
            "playlist-modify-public",
            "user-follow-modify",
            "user-follow-read",
            "user-read-playback-position",
            "user-top-read",
            "user-read-recently-played",
            "user-library-modify",
            "user-library-read",
            "user-read-email",
            "user-read-private"
    };

    TokenProvider(@NotNull Session session) {
        this.session = session;
    }

    @Nullable
    private StoredToken findTokenWithAllScopes(String[] scopes) {
        for (StoredToken token : tokens)
            if (token.hasScopes(scopes))
                return token;

        return null;
    }

    public synchronized StoredToken someToken() throws IOException, MercuryClient.MercuryException {
        if (!tokens.isEmpty()) {
            return tokens.get(0);
        } else {
            return getToken(availableScopes);
        }
    }

    @NotNull
    public synchronized StoredToken getToken(@NotNull String... scopes) throws IOException, MercuryClient.MercuryException {
        if (scopes.length == 0) scopes = availableScopes;

        StoredToken token = findTokenWithAllScopes(scopes);
        if (token != null) {
            if (token.expired()) tokens.remove(token);
            else return token;
        }

        ConsoleLoggingModules.debug("Token expired or not suitable, requesting again. {scopes: {}, oldToken: {}}", Arrays.asList(scopes), token);
        /*GenericJson resp = session.mercury().sendSync(MercuryRequests.requestToken(session.deviceId(), String.join(",", scopes)));
        token = new StoredToken(resp.obj);*/

        //ToDo: Change MercuryException to NoSuchAlgorithmException when an official fix is available
        try {
            Login5Api api = new Login5Api(session);
            Login5.LoginResponse resp = api.login5(
                    Login5.LoginRequest.newBuilder()
                            .setStoredCredential(Credentials.StoredCredential.newBuilder()
                                    .setUsername(session.username())
                                    .setData(ByteString.copyFrom(session.apWelcome().getReusableAuthCredentials().toByteArray()))
                                    .build())
                            .build()
            );
            if (!resp.hasOk()) throw new IOException("Login5 returned an error: " + resp.getError());
            Login5.LoginOk okResponse = resp.getOk();

            JsonObject tokenBuilder = new JsonObject();
            tokenBuilder.addProperty("accessToken", okResponse.getAccessToken());
            tokenBuilder.addProperty("expiresIn", okResponse.getAccessTokenExpiresIn());
            tokenBuilder.addProperty("tokenType", "Bearer");

            JsonArray scopesArray = new JsonArray();
            for (String scope : scopes)
                scopesArray.add(scope);

            tokenBuilder.add("scope", scopesArray);

            token = new StoredToken(tokenBuilder);
        }catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }

        ConsoleLoggingModules.debug("Updated token successfully! {scopes: {}, newToken: {}}", Arrays.asList(scopes), token);
        tokens.add(token);

        return token;
    }

    @NotNull
    public String get(@NotNull String scope) throws IOException, MercuryClient.MercuryException {
        return getToken(scope).accessToken;
    }

    public static class StoredToken {
        public final int expiresIn;
        public final String accessToken;
        public final String[] scopes;
        public final long timestamp;

        private StoredToken(@NotNull JsonObject obj) {
            timestamp = TimeProvider.currentTimeMillis();
            expiresIn = obj.get("expiresIn").getAsInt();
            accessToken = obj.get("accessToken").getAsString();

            JsonArray scopesArray = obj.getAsJsonArray("scope");
            scopes = new String[scopesArray.size()];
            for (int i = 0; i < scopesArray.size(); i++)
                scopes[i] = scopesArray.get(i).getAsString();
        }

        public boolean expired() {
            return timestamp + (expiresIn - TOKEN_EXPIRE_THRESHOLD) * 1000L < TimeProvider.currentTimeMillis();
        }

        @Override
        public String toString() {
            return "StoredToken{" +
                    "expiresIn=" + expiresIn +
                    ", accessToken='" + Utils.truncateMiddle(accessToken, 12) +
                    "', scopes=" + Arrays.toString(scopes) +
                    ", timestamp=" + timestamp +
                    '}';
        }

        public boolean hasScope(@NotNull String scope) {
            for (String s : scopes)
                if (Objects.equals(s, scope))
                    return true;

            return false;
        }

        public boolean hasScopes(String[] sc) {
            for (String s : sc)
                if (!hasScope(s))
                    return false;

            return true;
        }
    }
}

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

package com.spotifyxp.deps.xyz.gianlu.librespot.api.handlers;

import com.google.gson.JsonObject;
import com.spotifyxp.logging.ConsoleLoggingModules;
import io.undertow.server.HttpServerExchange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.spotifyxp.deps.xyz.gianlu.librespot.api.SessionWrapper;
import com.spotifyxp.deps.xyz.gianlu.librespot.api.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.ProtobufToJson;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.Session;
import com.spotifyxp.deps.xyz.gianlu.librespot.dealer.ApiClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.*;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;

/**
 * @author Gianlu
 */
public final class MetadataHandler extends AbsSessionHandler {
    private final boolean needsType;

    public MetadataHandler(@NotNull SessionWrapper wrapper, boolean needsType) {
        super(wrapper);
        this.needsType = needsType;
    }

    @Override
    public void handleRequest(@NotNull HttpServerExchange exchange, @NotNull Session session) throws Exception {
        exchange.startBlocking();
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        Map<String, Deque<String>> params = Utils.readParameters(exchange);
        String uri = Utils.getFirstString(params, "uri");
        if (uri == null) {
            Utils.invalidParameter(exchange, "uri");
            return;
        }

        MetadataType type;
        String typeStr = Utils.getFirstString(params, "type");
        if (typeStr == null) {
            if (needsType) {
                Utils.invalidParameter(exchange, "type");
                return;
            }

            type = MetadataType.guessTypeFromUri(uri);
        } else {
            type = MetadataType.parse(typeStr);
        }

        if (type == null) {
            Utils.invalidParameter(exchange, "type");
            return;
        }

        try {
            JsonObject obj = handle(session, type, uri);
            exchange.getResponseSender().send(obj.toString());
        } catch (ApiClient.StatusCodeException ex) {
            if (ex.code == 404) {
                Utils.invalidParameter(exchange, "uri", "404: Unknown uri");
                return;
            }

            Utils.internalError(exchange, ex);
            ConsoleLoggingModules.error("Failed handling api request. {type: {}, uri: {}, code: {}}", type, uri, ex.code, ex);
        } catch (IOException | MercuryClient.MercuryException ex) {
            Utils.internalError(exchange, ex);
            ConsoleLoggingModules.error("Failed handling api request. {type: {}, uri: {}}", type, uri, ex);
        } catch (IllegalArgumentException ex) {
            Utils.invalidParameter(exchange, "uri", "Invalid uri for type: " + type);
        }
    }

    @NotNull
    private JsonObject handle(@NotNull Session session, @NotNull MetadataType type, @NotNull String uri) throws IOException, MercuryClient.MercuryException, IllegalArgumentException {
        switch (type) {
            case ALBUM:
                return ProtobufToJson.convert(session.api().getMetadata4Album(AlbumId.fromUri(uri)));
            case ARTIST:
                return ProtobufToJson.convert(session.api().getMetadata4Artist(ArtistId.fromUri(uri)));
            case SHOW:
                return ProtobufToJson.convert(session.api().getMetadata4Show(ShowId.fromUri(uri)));
            case EPISODE:
                return ProtobufToJson.convert(session.api().getMetadata4Episode(EpisodeId.fromUri(uri)));
            case TRACK:
                return ProtobufToJson.convert(session.api().getMetadata4Track(TrackId.fromUri(uri)));
            case PLAYLIST:
                return handlePlaylist(session, uri);
            default:
                throw new IllegalArgumentException(type.name());
        }
    }

    @NotNull
    private JsonObject handlePlaylist(@NotNull Session session, @NotNull String uri) throws IOException, MercuryClient.MercuryException {
        return ProtobufToJson.convert(session.api().getPlaylist(PlaylistId.fromUri(uri)));
    }

    private enum MetadataType {
        EPISODE("episode"), TRACK("track"), ALBUM("album"),
        ARTIST("artist"), SHOW("show"), PLAYLIST("playlist");

        private final String val;

        MetadataType(String val) {
            this.val = val;
        }

        @Nullable
        private static MetadataType parse(@NotNull String val) {
            for (MetadataType type : values())
                if (Objects.equals(type.val, val))
                    return type;

            return null;
        }

        @Nullable
        public static MetadataType guessTypeFromUri(@NotNull String uri) {
            for (MetadataType type : values())
                if (uri.contains(type.val))
                    return type;

            return null;
        }
    }
}

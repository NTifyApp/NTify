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

import com.google.protobuf.ByteString;
import com.spotifyxp.deps.com.spotify.playlist4.Playlist4ApiProto;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.PlaylistId;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.SpotifyId;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class Playlist {
    private final ApiClient apiClient;

    protected Playlist(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @NotNull
    public Playlist4ApiProto.SelectedListContent get(@NotNull PlaylistId id) throws IOException, MercuryClient.MercuryException {
        try (Response resp = apiClient.send("GET", "/playlist/v2/playlist/" + id.id(), null, null)) {
            ApiClient.StatusCodeException.checkStatus(resp);


            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Playlist4ApiProto.SelectedListContent.parseFrom(body.byteStream());
        }
    }

    @NotNull
    public Playlist4ApiProto.Diff remove(String spotifyUsername, String[] uris) {
        apiClient.nonces++;
        Playlist4ApiProto.ListChanges.Builder listChanges = Playlist4ApiProto.ListChanges.newBuilder()
                .setBaseRevision(getRevision(spotifyUsername).getBaseRevision())
                .addDeltas(Playlist4ApiProto.Delta.newBuilder()
                        .addOps(Playlist4ApiProto.Op.newBuilder()
                                .setKind(Playlist4ApiProto.Op.Kind.REM)
                                .setRem(Playlist4ApiProto.Rem.newBuilder()
                                        .build())
                                .build())
                        .setInfo(Playlist4ApiProto.ChangeInfo.newBuilder()
                                .setUser(spotifyUsername)
                                .setTimestamp(System.currentTimeMillis())
                                .build())
                        .build())
                .setWantSyncResult(false)
                .setWantResultingRevisions(false)
                .addNonces(apiClient.nonces);

        for (String uri : uris) {
            listChanges.getDeltas(0).getOps(0).getRem().getItemsList().add(Playlist4ApiProto.Item.newBuilder()
                    .setUri(uri)
                    .build());
        }

        try (Response resp = apiClient.send("POST", "/playlist/v2/user/" + spotifyUsername + "/rootlist/changes", null, ApiClient.protoBody(listChanges.build()))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Playlist4ApiProto.Diff.parseFrom(body.byteStream());
        } catch (IOException | MercuryClient.MercuryException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public Playlist4ApiProto.Diff edit(@NotNull String id, @Nullable String name, @Nullable String description, int isPublic, int isCollaborative, byte[] image) throws IOException, MercuryClient.MercuryException {
        apiClient.nonces++;
        Playlist4ApiProto.ListChanges.Builder listChanges = Playlist4ApiProto.ListChanges.newBuilder()
                .setBaseRevision(ByteString.copyFrom("\u0000\u0000\u0000\u0000root".getBytes()))
                .addDeltas(Playlist4ApiProto.Delta.newBuilder()
                        .addOps(Playlist4ApiProto.Op.newBuilder()
                                .setKind(Playlist4ApiProto.Op.Kind.UPDATE_LIST_ATTRIBUTES)
                                .setUpdateListAttributes(Playlist4ApiProto.UpdateListAttributes.newBuilder()
                                        .setNewAttributes(Playlist4ApiProto.ListAttributesPartialState.newBuilder()
                                                .setValues(Playlist4ApiProto.ListAttributes.newBuilder()
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .setInfo(Playlist4ApiProto.ChangeInfo.newBuilder()
                                .setUser(apiClient.session.username())
                                .setTimestamp(System.currentTimeMillis())
                                .build())
                        .build())
                .setWantResultingRevisions(false)
                .setWantSyncResult(false)
                .addNonces(apiClient.nonces);

        if (name != null && !name.isEmpty()) {
            listChanges.getDeltas(0).getOps(0).getUpdateListAttributes().getNewAttributes().toBuilder()
                    .getValuesBuilder()
                    .setName(name);
        }

        if (description != null && !description.isEmpty()) {
            listChanges.getDeltas(0).getOps(0).getUpdateListAttributes().getNewAttributes().toBuilder()
                    .getValuesBuilder()
                    .setDescription(description);
        }

        if (isCollaborative != -1) {
            listChanges.getDeltas(0).getOps(0).getUpdateListAttributes().getNewAttributes().getValues().toBuilder()
                    .setCollaborative(isCollaborative == 1);
        }

        try (Response resp = apiClient.send("POST", "/playlist/v2/playlist/" + id + "/changes", null, ApiClient.protoBody(listChanges.build()))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            if(isPublic != -1) return Playlist4ApiProto.Diff.parseFrom(body.byteStream());

            setPermission(id, isPublic == 1 ? Playlist4ApiProto.PermissionLevel.VIEWER : Playlist4ApiProto.PermissionLevel.BLOCKED);

            if (image.length != 0) {
                uploadImage(id, image);
            }

            return Playlist4ApiProto.Diff.parseFrom(body.byteStream());
        }
    }

    public void uploadImage(String playlistId, byte[] jpegBytes) throws IOException, MercuryClient.MercuryException {
        String uploadToken = uploadImage(jpegBytes).getUploadToken();
        byte[] imageBytes = registerImage(playlistId, uploadToken).getPicture().toByteArray();
        edit(playlistId, null, null, -1, -1, imageBytes);
    }

    public Playlist4ApiProto.RegisterPlaylistImageResponse registerImage(String playlistId, String uploadToken) throws IOException, MercuryClient.MercuryException {
        Playlist4ApiProto.RegisterPlaylistImageRequest request = Playlist4ApiProto.RegisterPlaylistImageRequest.newBuilder()
                .setUploadToken(uploadToken)
                .build();
        try (Response resp = apiClient.send("POST", "/playlist/v2/playlist/" + playlistId + "/register-image", null, ApiClient.protoBody(request))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Playlist4ApiProto.RegisterPlaylistImageResponse.parseFrom(body.byteStream());
        }
    }

    public Playlist4ApiProto.RegisterPlaylistImageRequest uploadImage(byte[] jpegBytes) throws IOException, MercuryClient.MercuryException {
        try (Response resp = apiClient.send("POST", "/v4/playlist", null, RequestBody.create(jpegBytes, MediaType.parse("image/jpeg")))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Playlist4ApiProto.RegisterPlaylistImageRequest.parseFrom(body.byteStream());
        }
    }

    public Playlist4ApiProto.SetPermissionResponse setPermission(String playlistId, Playlist4ApiProto.PermissionLevel level) throws IOException, MercuryClient.MercuryException {
        Playlist4ApiProto.SetPermissionLevelRequest request = Playlist4ApiProto.SetPermissionLevelRequest.newBuilder()
                .setPermissionLevel(level)
                .build();
        try (Response resp = apiClient.send("POST", "/playlist-permission/v1/playlist/" + playlistId + "/permission/base/level", null, ApiClient.protoBody(request))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Playlist4ApiProto.SetPermissionResponse.parseFrom(body.byteStream());
        }
    }

    public Playlist4ApiProto.Permission getPermission(String playlistId) throws IOException, MercuryClient.MercuryException {
        try (Response resp = apiClient.send("GET", "/playlist-permission/v1/playlist/" + playlistId + "/permission/base", null, null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Playlist4ApiProto.Permission.parseFrom(body.byteStream());
        }
    }

    @NotNull
    public PlaylistCreateResponse create(@NotNull String name, @Nullable String description, boolean isPublic, boolean isCollaborative, byte[] imageData) throws IOException, MercuryClient.MercuryException {
        String playlistId = SpotifyId.generateId(apiClient.session.secureRandom());
        try {
            Playlist4ApiProto.Diff diff = edit(playlistId,  name, description, isPublic ? 1 : 0, isCollaborative ? 1 : 0, imageData);
            PlaylistCreateResponse response = new PlaylistCreateResponse();
            response.diff = diff;
            response.uri = "spotify:playlist:" + playlistId;
            return response;
        }catch (ApiClient.StatusCodeException e) {
            if (e.code != 509) throw e;

            // Playlist id is already taken, try again
            return create(name, description, isPublic, isCollaborative, imageData);
        }
    }

    @NotNull
    public Playlist4ApiProto.ListChanges getRevision(String username) {
        try (Response resp = apiClient.send("GET", "/playlist/v2/user/" + username + "/rootlist?decorate=revision", null, null)) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Playlist4ApiProto.ListChanges.parseFrom(body.byteStream());
        } catch (IOException | MercuryClient.MercuryException e) {
            throw new RuntimeException(e);
        }
    }

    public Playlist4ApiProto.Diff follow(PlaylistId playlistId, boolean isPublic) throws IOException, MercuryClient.MercuryException {
        Playlist4ApiProto.ListChanges listChanges = Playlist4ApiProto.ListChanges.newBuilder()
                .setBaseRevision(getRevision(apiClient.session.username()).getBaseRevision())
                .addDeltas(Playlist4ApiProto.Delta.newBuilder()
                        .addOps(Playlist4ApiProto.Op.newBuilder()
                                .setKind(Playlist4ApiProto.Op.Kind.ADD)
                                .setAdd(Playlist4ApiProto.Add.newBuilder()
                                        .setFromIndex(0)
                                        .addItems(Playlist4ApiProto.Item.newBuilder()
                                                .setUri(playlistId.toSpotifyUri())
                                                .setAttributes(Playlist4ApiProto.ItemAttributes.newBuilder()
                                                        .setTimestamp(System.currentTimeMillis())
                                                        .setPublic(isPublic)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .setInfo(Playlist4ApiProto.ChangeInfo.newBuilder()
                                .setUser(apiClient.session.username())
                                .setTimestamp(System.currentTimeMillis())
                                .build())
                        .build())
                .setWantSyncResult(false)
                .setWantResultingRevisions(false)
                .build();

        try (Response resp = apiClient.send("POST", "/playlist/v2/user/" + apiClient.session.username() + "/rootlist/changes", null, ApiClient.protoBody(listChanges))) {
            ApiClient.StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return Playlist4ApiProto.Diff.parseFrom(body.byteStream());
        }
    }

    public void addItems(PlaylistId playlistId, String[] uris) throws IOException, MercuryClient.MercuryException {
        apiClient.nonces++;
        Playlist4ApiProto.ListChanges.Builder listChanges = Playlist4ApiProto.ListChanges.newBuilder()
                .setBaseRevision(getRevision(apiClient.session.username()).getBaseRevision())
                .addDeltas(Playlist4ApiProto.Delta.newBuilder()
                        .addOps(Playlist4ApiProto.Op.newBuilder()
                                .setKind(Playlist4ApiProto.Op.Kind.ADD)
                                .setAdd(Playlist4ApiProto.Add.newBuilder()
                                        .setFromIndex(0)
                                        .build())
                                .build())
                        .setInfo(Playlist4ApiProto.ChangeInfo.newBuilder()
                                .setUser(apiClient.session.username())
                                .setTimestamp(System.currentTimeMillis())
                                .build())
                        .build())
                .setWantSyncResult(false)
                .setWantResultingRevisions(false)
                .addNonces(apiClient.nonces);

        for (String uri : uris) {
            byte[] opId = new byte[8];
            apiClient.session.secureRandom().nextBytes(opId);
            listChanges.getDeltas(0).getOps(0).getAdd().getItemsList().add(Playlist4ApiProto.Item.newBuilder()
                            .setUri(uri)
                            .setAttributes(Playlist4ApiProto.ItemAttributes.newBuilder()
                                    .setTimestamp(System.currentTimeMillis())
                                    .setItemId(ByteString.copyFrom(opId))
                                    .build())
                    .build());
        }

        try (Response resp = apiClient.send("POST", "/playlist/v2/playlist/" + playlistId.id() + "/changes", null, ApiClient.protoBody(listChanges.build()))) {
            ApiClient.StatusCodeException.checkStatus(resp);
        }
    }

    public void removeItems(PlaylistId playlistId, String[] uris) throws IOException, MercuryClient.MercuryException {
        apiClient.nonces++;
        Playlist4ApiProto.ListChanges.Builder listChanges = Playlist4ApiProto.ListChanges.newBuilder()
                .setBaseRevision(getRevision(apiClient.session.username()).getBaseRevision())
                .addDeltas(Playlist4ApiProto.Delta.newBuilder()
                        .addOps(Playlist4ApiProto.Op.newBuilder()
                                .setKind(Playlist4ApiProto.Op.Kind.REM)
                                .setRem(Playlist4ApiProto.Rem.newBuilder()
                                        .setFromIndex(0)
                                        .build())
                                .build())
                        .setInfo(Playlist4ApiProto.ChangeInfo.newBuilder()
                                .setUser(apiClient.session.username())
                                .setTimestamp(System.currentTimeMillis())
                                .build())
                        .build())
                .setWantSyncResult(false)
                .setWantResultingRevisions(false)
                .addNonces(apiClient.nonces);

        for (String uri : uris) {
            byte[] opId = new byte[8];
            apiClient.session.secureRandom().nextBytes(opId);
            listChanges.getDeltas(0).getOps(0).getAdd().getItemsList().add(Playlist4ApiProto.Item.newBuilder()
                    .setUri(uri)
                    .setAttributes(Playlist4ApiProto.ItemAttributes.newBuilder()
                            .setTimestamp(System.currentTimeMillis())
                            .setItemId(ByteString.copyFrom(opId))
                            .build())
                    .build());
        }

        try (Response resp = apiClient.send("POST", "/playlist/v2/playlist/" + playlistId.id() + "/changes", null, ApiClient.protoBody(listChanges.build()))) {
            ApiClient.StatusCodeException.checkStatus(resp);
        }
    }


    public static class PlaylistCreateResponse {
        public Playlist4ApiProto.Diff diff;
        public String uri;
    }
}

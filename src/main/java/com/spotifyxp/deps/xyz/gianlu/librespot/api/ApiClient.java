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

package com.spotifyxp.deps.xyz.gianlu.librespot.api;

import com.google.gson.*;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.spotifyxp.deps.com.spotify.clienttoken.data.v0.Connectivity;
import com.spotifyxp.deps.com.spotify.clienttoken.http.v0.ClientToken;
import com.spotifyxp.deps.com.spotify.collection2.v2.proto.Collection;
import com.spotifyxp.deps.com.spotify.connectstate.Connect;
import com.spotifyxp.deps.com.spotify.extendedmetadata.EntityExtensionDataOuterClass;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotifyxp.deps.xyz.gianlu.librespot.Version;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.Session;
import com.spotifyxp.deps.xyz.gianlu.librespot.json.StationsWrapper;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryRequests;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.SpotifyId;
import com.spotifyxp.logging.ConsoleLoggingModules;
import com.spotifyxp.protogens.Concert;
import com.spotifyxp.protogens.ConcertsOuterClass;
import com.spotifyxp.utils.ApplicationUtils;
import okhttp3.*;
import okio.BufferedSink;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * @author devgianlu
 */
public final class ApiClient {
    final Session session;
    private final String baseUrl;
    private String clientToken = null;
    // List of hashed graphql queries used by the Spotify Web Player
    private final Map<String, String> operationQueryHashes = new HashMap<String, String>() {{
        put("home", "63c412a34a2071adfd99b804ea2fe1d8e9c5fd7d248e29ca54cc97a7ca06b561");
        put("queryArtistDiscoveredOn", "71c2392e4cecf6b48b9ad1311ae08838cbdabcfd189c6bf0c66c2430b8dcfdb1");
        put("queryArtistRelated", "3d031d6cb22a2aa7c8d203d49b49df731f58b1e2799cc38d9876d58771aa66f3");
        put("queryArtistOverview", "1ac33ddab5d39a3a9c27802774e6d78b9405cc188c6f75aed007df2a32737c72");
        put("libraryV3", "9f4da031f81274d572cfedaf6fc57a737c84b43d572952200b2c36aaa8fec1c6");
        put("fetchLibraryTracks", "087278b20b743578a6262c2b0b4bcd20d879c503cc359a2285baf083ef944240");
        put("searchDesktop", "fcad5a3e0d5af727fb76966f06971c19cfa2275e6ff7671196753e008611873c");
        put("areEntitiesInLibrary", "134337999233cc6fdd6b1e6dbf94841409f04a946c5c7b744b09ba0dfe5a85ed");
    }};
    int nonces = 0;

    // Used via reflection
    @SuppressWarnings("unused")
    private Album album;
    @SuppressWarnings("unused")
    private Artist artist;
    @SuppressWarnings("unused")
    private Episode episode;
    @SuppressWarnings("unused")
    private Playlist playlist;
    @SuppressWarnings("unused")
    private Show show;
    @SuppressWarnings("unused")
    private Track track;
    @SuppressWarnings("unused")
    private User user;
    @SuppressWarnings("unused")
    //-----------------------

    public ApiClient(@NotNull Session session) {
        this.session = session;
        this.baseUrl = "https://" + session.apResolver().getRandomSpclient();
    }

    @NotNull
    public static RequestBody protoBody(@NotNull Message msg) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return MediaType.get("application/x-protobuf");
            }

            @Override
            public void writeTo(@NotNull BufferedSink sink) throws IOException {
                sink.write(msg.toByteArray());
            }
        };
    }

    @NotNull
    private Request buildRequest(@NotNull String method, @NotNull String suffix, @Nullable Headers headers, @Nullable RequestBody body) throws IOException, MercuryClient.MercuryException {
        if (clientToken == null) {
            ClientToken.ClientTokenResponse resp = clientToken();
            clientToken = resp.getGrantedToken().getToken();
            ConsoleLoggingModules.debug("Updated client token: {}", clientToken);
        }

        Request.Builder request = new Request.Builder();
        request.method(method, body);
        if (headers != null) request.headers(headers);
        request.addHeader("Authorization", "Bearer " + session.tokens().get("playlist-read"));
        request.addHeader("client-token", clientToken);
        request.url(!suffix.startsWith("http") ? baseUrl + suffix : suffix);
        return request.build();
    }

    public void sendAsync(@NotNull String method, @NotNull String suffix, @Nullable Headers headers, @Nullable RequestBody body, @NotNull Callback callback) throws IOException, MercuryClient.MercuryException {
        session.client().newCall(buildRequest(method, suffix, headers, body)).enqueue(callback);
    }

    /**
     * Sends a request to the Spotify API.
     *
     * @param method  The request method
     * @param suffix  The suffix to be appended to {@link #baseUrl} also know as path
     * @param headers Additional headers
     * @param body    The request body
     * @param tries   How many times the request should be reattempted (0 = none)
     * @return The response
     * @throws IOException                    The last {@link IOException} thrown by {@link Call#execute()}
     * @throws MercuryClient.MercuryException If the API token couldn't be requested
     */
    @NotNull
    public Response send(@NotNull String method, @NotNull String suffix, @Nullable Headers headers, @Nullable RequestBody body, int tries) throws IOException, MercuryClient.MercuryException {
        IOException lastEx;
        do {
            try {
                Response resp = session.client().newCall(buildRequest(method, suffix, headers, body)).execute();
                if (resp.code() == 503) {
                    lastEx = new StatusCodeException(resp);
                    continue;
                }

                return resp;
            } catch (IOException ex) {
                lastEx = ex;
            }
        } while (tries-- > 1);

        throw lastEx;
    }

    @NotNull
    public Response send(@NotNull String method, @NotNull String suffix, @Nullable Headers headers, @Nullable RequestBody body) throws IOException, MercuryClient.MercuryException {
        return send(method, suffix, headers, body, 1);
    }

    public void checkExtendedMetadataResponse(ExtendedMetadata.BatchedExtensionResponse response) throws IOException {
        if (response.getExtendedMetadataCount() == 0)
            throw new IOException("No metadata in BatchedExtensionResponse");

        if (response.getExtendedMetadata(0).getExtensionDataCount() == 0)
            throw new IOException("No metadata in ExtendedMetadata in BatchedExtensionResponse");

        if (response.getExtendedMetadata(0).getExtensionData(0).getHeader().getStatusCode() != 200)
            throw new IOException("Bad status code for metadata: " + response.getExtendedMetadata(0).getExtensionData(0).getHeader().getStatusCode());
    }

    public void putConnectState(@NotNull String connectionId, @NotNull Connect.PutStateRequest proto) throws IOException, MercuryClient.MercuryException {
        try (Response resp = send("PUT", "/connect-state/v1/devices/" + session.deviceId(), new Headers.Builder()
                .add("X-Spotify-Connection-Id", connectionId).build(), protoBody(proto), 5 /* We want this to succeed */)) {
            if (resp.code() == 413)
                ConsoleLoggingModules.warning("PUT state payload is too large: {} bytes uncompressed.", proto.getSerializedSize());
            else if (resp.code() != 200)
                ConsoleLoggingModules.warning("PUT state returned {}. {headers: {}}", resp.code(), resp.headers());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrInstantiateApiClass(String name) {
        switch (name) {
            case "Album":
                if (album == null) {
                    album = new Album(this);
                }
                return (T) album;
            case "Artist":
                if (artist == null) {
                    artist = new Artist(this);
                }
                return (T) artist;
            case "Episode":
                if (episode == null) {
                    episode = new Episode(this);
                }
                return (T) episode;
            case "Playlist":
                if (playlist == null) {
                    playlist = new Playlist(this);
                }
                return (T) playlist;
            case "Show":
                if (show == null) {
                    show = new Show(this);
                }
                return (T) show;
            case "Track":
                if (track == null) {
                    track = new Track(this);
                }
                return (T) track;
            case "User":
                if (user == null) {
                    user = new User(this);
                }
                return (T) user;
        }

        return null;
    }

    public Album album() {
        return getOrInstantiateApiClass("Album");
    }

    public Artist artist() {
        return getOrInstantiateApiClass("Artist");
    }

    public Episode episode() {
        return getOrInstantiateApiClass("Episode");
    }

    public Playlist playlist() {
        return getOrInstantiateApiClass("Playlist");
    }

    public Show show() {
        return getOrInstantiateApiClass("Show");
    }

    public Track track() {
        return getOrInstantiateApiClass("Track");
    }

    public User user() {
        return getOrInstantiateApiClass("User");
    }

    @NotNull
    public ExtendedMetadata.BatchedExtensionResponse getExtendedMetadata(@NotNull ExtendedMetadata.BatchedEntityRequest req) throws IOException, MercuryClient.MercuryException {
        try (Response resp = send("POST", "/extended-metadata/v0/extended-metadata", null, protoBody(req))) {
            StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return ExtendedMetadata.BatchedExtensionResponse.parseFrom(body.byteStream());
        }
    }

    @NotNull
    public ExtendedMetadata.BatchedExtensionResponse getExtendedMetadata(@NotNull ExtensionKindOuterClass.ExtensionKind extensionKind, @NotNull SpotifyId spotifyId) throws IOException, MercuryClient.MercuryException {
        try (Response resp = send("POST", "/extended-metadata/v0/extended-metadata", null, protoBody(ExtendedMetadata.BatchedEntityRequest.newBuilder()
                .addEntityRequest(ExtendedMetadata.EntityRequest.newBuilder()
                        .setEntityUri(spotifyId.toSpotifyUri())
                        .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                .setExtensionKind(extensionKind)
                                .build())
                        .build())
                .build()))) {
            StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return ExtendedMetadata.BatchedExtensionResponse.parseFrom(body.byteStream());
        }
    }

    public void follow(SpotifyId spotifyId) throws IOException, MercuryClient.MercuryException {
        Collection.WriteRequest request = Collection.WriteRequest.newBuilder()
                .setUsername(session.username())
                .setSet("collection")
                .addItems(Collection.CollectionItem.newBuilder()
                        .setUri(spotifyId.toSpotifyUri())
                        .setAddedAt((int) (System.currentTimeMillis() / 1000))
                        .build())
                .setClientUpdateId(String.format("%016x", session.secureRandom().nextLong()))
                .build();

        try (Response resp = send("POST", "/collection/v2/write", null, ApiClient.protoBody(request))) {
            ApiClient.StatusCodeException.checkStatus(resp);
        }
    }

    public void unfollow(SpotifyId spotifyId) throws IOException, MercuryClient.MercuryException {
        Collection.WriteRequest request = Collection.WriteRequest.newBuilder()
                .setUsername(session.username())
                .setSet("collection")
                .addItems(Collection.CollectionItem.newBuilder()
                        .setUri(spotifyId.toSpotifyUri())
                        .setIsRemoved(true)
                        .build())
                .setClientUpdateId(String.format("%016x", session.secureRandom().nextLong()))
                .build();

        try (Response resp = send("POST", "/collection/v2/write", null, ApiClient.protoBody(request))) {
            ApiClient.StatusCodeException.checkStatus(resp);
        }
    }

    @NotNull
    public JsonObject getSpotifyBrowse() throws IOException, MercuryClient.MercuryException {
        String query = "?platform=android&client-timezone=" + ZoneId.systemDefault().toString().replace("/", "%2F") + "&podcast=true&locale=" + Locale.getDefault();
        try (Response resp = send("GET", "https://spclient.wg.spotify.com/hubview-mobile-v1/browse/" + query, new Headers.Builder()
                .add("App-Platform", "Android")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .add("Accept", "application/json")
                .add("Accept-Encoding", "gzip")
                .add("User-Agent", "Spotify/8.9.96.476 Android/31 (Android SDK built for x86_64) " + ApplicationUtils.getName() + "/" + ApplicationUtils.getVersion())
                .build(), null)) {
            StatusCodeException.checkStatus(resp);

            assert resp.body() != null;
            return JsonParser.parseString(IOUtils.toString(new GZIPInputStream(new ByteArrayInputStream(resp.body().bytes())), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @NotNull
    public JsonObject getSpotifyBrowseSection(String sectionUri) throws IOException, MercuryClient.MercuryException {
        String query = "?platform=android&client-timezone=" + ZoneId.systemDefault().toString().replace("/", "%2F") + "&podcast=true&locale=" + Locale.getDefault();
        try (Response resp = send("GET", "https://spclient.wg.spotify.com/hubview-mobile-v1/browse/" + sectionUri + query, new Headers.Builder()
                .add("App-Platform", "Android")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .add("Accept", "application/json")
                .add("Accept-Encoding", "gzip")
                .add("User-Agent", "Spotify/8.9.96.476 Android/31 (Android SDK built for x86_64) " + ApplicationUtils.getName() + "/" + ApplicationUtils.getVersion())
                .build(), null)) {
            StatusCodeException.checkStatus(resp);

            assert resp.body() != null;
            return JsonParser.parseString(IOUtils.toString(new GZIPInputStream(new ByteArrayInputStream(resp.body().bytes())), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @NotNull
    public ConcertsOuterClass.Concerts getConcerts() throws IOException, MercuryClient.MercuryException {
        try (Response resp = send("POST", "https://spclient.wg.spotify.com/live-events-view/spotify.liveeventsview.v2.LiveEventsFeedService/GetPage", new Headers.Builder()
                .add("App-Platform", "Win32")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .build(), RequestBody.create(Concert.ConcertRequest.newBuilder()
                .setId("ignored")
                .build().toByteArray()))) {
            StatusCodeException.checkStatus(resp);

            assert resp.body() != null;
            return ConcertsOuterClass.Concerts.parseFrom(resp.body().bytes());
        }
    }

    @NotNull
    public Concert.ConcertResponse getConcert(String id) throws IOException, MercuryClient.MercuryException {
        try (Response resp = send("POST", "https://spclient.wg.spotify.com/live-events-view/spotify.liveeventsview.v2.LiveEventsFeedService/EventPage", new Headers.Builder()
                .add("App-Platform", "Win32")
                .build(), RequestBody.create(Concert.ConcertRequest.newBuilder()
                .setId(id)
                .build().toByteArray()))) {
            StatusCodeException.checkStatus(resp);

            assert resp.body() != null;
            return Concert.ConcertResponse.parseFrom(resp.body().bytes());
        }
    }

    @NotNull
    public JsonObject search(String searchTerm, int offset, int limit, int numberOfTopResults, boolean includeAudiobooks, boolean includeArtistHasConcertsField, boolean includePreRelease, boolean includeAuthors) {
        JsonObject variables = new JsonObject();
        variables.add("searchTerm", new JsonPrimitive(searchTerm));
        variables.add("offset", new JsonPrimitive(offset));
        variables.add("limit", new JsonPrimitive(limit));
        variables.add("numberOfTopResults", new JsonPrimitive(numberOfTopResults));
        variables.add("includeAudiobooks", new JsonPrimitive(includeAudiobooks));
        variables.add("includeArtistHasConcertsField", new JsonPrimitive(includeArtistHasConcertsField));
        variables.add("includePreRelease", new JsonPrimitive(includePreRelease));
        variables.add("includeAuthors", new JsonPrimitive(includeAuthors));

        try (Response resp = send("POST", "https://api-partner.spotify.com/pathfinder/v1/query", new Headers.Builder()
                .add("App-Platform", "Win32")
                .add("Accept-Language", Locale.getDefault().toString().replace("_", "-"))
                .add("Accept", "application/json")
                .add("Content-Encoding", "")
                .build(), RequestBody.create(getQuery("searchDesktop", variables).toString(), MediaType.get("application/json")))) {
            StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return JsonParser.parseReader(body.charStream()).getAsJsonObject();
        } catch (IOException | MercuryClient.MercuryException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public StationsWrapper getApolloStation(@NotNull String context, @NotNull List<String> prevTracks, int count, boolean autoplay) throws IOException, MercuryClient.MercuryException {
        StringBuilder prevTracksStr = new StringBuilder();
        for (int i = 0; i < prevTracks.size(); i++) {
            if (i != 0) prevTracksStr.append(",");
            prevTracksStr.append(prevTracks.get(i));
        }

        try (Response resp = send("GET", String.format("/radio-apollo/v3/stations/%s?count=%d&prev_tracks=%s&autoplay=%b", context, count, prevTracksStr, autoplay), null, null)) {
            StatusCodeException.checkStatus(resp);

            ResponseBody body;
            if ((body = resp.body()) == null) throw new IOException();
            return new StationsWrapper(JsonParser.parseReader(body.charStream()).getAsJsonObject());
        }
    }

    public String getClientToken() throws IOException {
        if(clientToken == null) {
            clientToken = clientToken().getGrantedToken().getToken();
        }
        return clientToken;
    }

    @NotNull
    private ClientToken.ClientTokenResponse clientToken() throws IOException {
        ClientToken.ClientTokenRequest protoReq = ClientToken.ClientTokenRequest.newBuilder()
                .setRequestType(ClientToken.ClientTokenRequestType.REQUEST_CLIENT_DATA_REQUEST)
                .setClientData(ClientToken.ClientDataRequest.newBuilder()
                        .setClientId(MercuryRequests.KEYMASTER_CLIENT_ID)
                        .setClientVersion(Version.versionNumber())
                        .setConnectivitySdkData(Connectivity.ConnectivitySdkData.newBuilder()
                                .setDeviceId(session.deviceId())
                                .setPlatformSpecificData(Connectivity.PlatformSpecificData.newBuilder()
                                        .setWindows(Connectivity.NativeWindowsData.newBuilder()
                                                .setSomething1(10)
                                                .setSomething3(21370)
                                                .setSomething4(2)
                                                .setSomething6(9)
                                                .setSomething7(332)
                                                .setSomething8(34404)
                                                .setSomething10(true)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        Request.Builder req = new Request.Builder()
                .url("https://clienttoken.spotify.com/v1/clienttoken")
                .header("Accept", "application/x-protobuf")
                .header("Content-Encoding", "")
                .post(protoBody(protoReq));

        try (Response resp = session.client().newCall(req.build()).execute()) {
            StatusCodeException.checkStatus(resp);

            ResponseBody body = resp.body();
            if (body == null) throw new IOException();
            return ClientToken.ClientTokenResponse.parseFrom(body.byteStream());
        }
    }

    JsonObject getQuery(String operationName, JsonObject variables) {
        JsonObject query = new JsonObject();
        JsonObject extensions = new JsonObject();
        JsonObject persistedQuery = new JsonObject();
        persistedQuery.add("sha256Hash", new JsonPrimitive(operationQueryHashes.get(operationName)));
        persistedQuery.add("version", new JsonPrimitive(1));
        extensions.add("persistedQuery", persistedQuery);
        if (variables != null) query.add("variables", variables);
        query.add("operationName", new JsonPrimitive(operationName));
        query.add("extensions", extensions);
        return query;
    }

    String appendQueryHash(String url, String operationName, JsonObject variables) {
        String queryHash = operationQueryHashes.get(operationName);
        if (queryHash == null) throw new IllegalStateException("No query hash for operation: " + operationName);

        if (url.contains("?")) {
            url += "&";
        } else {
            url += "?";
        }

        url += "extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%22" + queryHash + "%22%7D%7D";

        try {
            if (variables != null)
                url += "&variables=" + URLEncoder.encode(variables.toString(), "UTF-8");
        }catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        return url;
    }

    protected static class RootlistChangeRequest {
        public List<Delta> deltas;

        public RootlistChangeRequest(List<Delta> deltas) {
            this.deltas = deltas;
        }

        protected static class Delta {
            public Info info;
            public List<Operation<?>> ops;

            public Delta(Info info, List<Operation<?>> ops) {
                this.info = info;
                this.ops = ops;
            }
        }

        protected static class Info {
            public Source source;

            public Info(Source source) {
                this.source = source;
            }
        }

        protected static class Source {
            public String client;

            public Source(String client) {
                this.client = client;
            }
        }

        protected static class Operation<Type> {
            public String kind;
            public Type operation;

            public Operation(String kind, Type operation) {
                this.kind = kind;
                this.operation = operation;
            }
        }

        protected static class Item {
            public String uri;

            public Item(String uri) {
                this.uri = uri;
            }
        }

        protected static class RemoveOperation {
            public List<Item> items;
            public boolean itemsAreKeys;

            public RemoveOperation(String[] uris, boolean itemsAreKeys) {
                this.items = new ArrayList<>();
                for (String uri : uris) {
                    this.items.add(new Item(uri));
                }
                this.itemsAreKeys = itemsAreKeys;
            }

            public String getPropertyName() {
                return "rem";
            }
        }

        protected static class CustomPropertySerializer implements JsonSerializer<Operation<?>> {
            @Override
            public JsonElement serialize(Operation<?> src, Type typeOfSrc, JsonSerializationContext context) {
                JsonObject root = new JsonObject();

                Object value = src.operation;

                try {
                    root.add(value.getClass().getMethod("getPropertyName", new Class[]{}).invoke(value).toString(), context.serialize(value));
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                    throw new RuntimeException(e);
                }

                return root;
            }
        }
    }

    public static class BatchedRequestHelper {
        private final ExtendedMetadata.BatchedEntityRequest.Builder builder = ExtendedMetadata.BatchedEntityRequest.newBuilder();
        private final Map<String, RequestResolvedCallback> callbacks = new HashMap<>();

        public interface RequestResolvedCallback {
            void onRequestResolved(Any... data) throws Exception;
        }

        public interface RequestFailedCallback {
            void onRequestFailed(Exception exception, ExtendedMetadata.BatchedExtensionResponse response);
        }

        public void addRequest(ExtendedMetadata.EntityRequest request, RequestResolvedCallback callback) {
            builder.addEntityRequest(request);
            callbacks.put(request.getEntityUri(), callback);
        }

        public void execute(ApiClient apiClient, @Nullable RequestFailedCallback requestFailedCallback) throws IOException, MercuryClient.MercuryException {
            ExtendedMetadata.BatchedExtensionResponse response = apiClient.getExtendedMetadata(builder.build());
            apiClient.checkExtendedMetadataResponse(response);
            Map<String, List<Any>> dataMap = new HashMap<>();
            try {
                for (ExtendedMetadata.EntityExtensionDataArray metadataEntry : response.getExtendedMetadataList()) {
                    for (EntityExtensionDataOuterClass.EntityExtensionData extData : metadataEntry.getExtensionDataList()) {
                        if (extData.getHeader().getStatusCode() != 200) continue;
                        List<Any> data = dataMap.getOrDefault(extData.getEntityUri(), new ArrayList<>());
                        data.add(extData.getExtensionData());
                        dataMap.put(extData.getEntityUri(), data);
                    }
                }
                for (ExtendedMetadata.EntityRequest req : builder.getEntityRequestList()) {
                    String uri = req.getEntityUri();
                    List<Any> data = dataMap.get(uri);
                    if (data == null) continue;
                    callbacks.getOrDefault(uri, ignored -> {
                    }).onRequestResolved(data.toArray(new Any[0]));
                }
            } catch (Exception e) {
                if (requestFailedCallback != null)
                    requestFailedCallback.onRequestFailed(e, response);
            }
        }
    }

    public void setClientToken(@Nullable String clientToken) {
        this.clientToken = clientToken;
    }

    public static class StatusCodeException extends IOException {
        public final int code;

        StatusCodeException(@NotNull Response resp) {
            super(String.format("%d: %s", resp.code(), resp.message()));
            code = resp.code();
        }

        static void checkStatus(@NotNull Response resp) throws StatusCodeException {
            if (resp.code() != 200) throw new StatusCodeException(resp);
        }
    }
}

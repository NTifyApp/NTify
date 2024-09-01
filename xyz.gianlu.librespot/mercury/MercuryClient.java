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

package com.spotifyxp.deps.xyz.gianlu.librespot.mercury;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.spotifyxp.deps.com.spotify.Mercury;
import com.spotifyxp.deps.com.spotify.Pubsub;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.BytesArrayList;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.ProtobufToJson;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.PacketsReceiver;
import com.spotifyxp.deps.xyz.gianlu.librespot.core.Session;
import com.spotifyxp.deps.xyz.gianlu.librespot.crypto.Packet;
import com.spotifyxp.deps.xyz.gianlu.librespot.json.JsonWrapper;
import com.spotifyxp.logging.ConsoleLoggingModules;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Gianlu
 */
public final class MercuryClient implements PacketsReceiver, Closeable {
    private static final int MERCURY_REQUEST_TIMEOUT = 3000;
    private final AtomicInteger seqHolder = new AtomicInteger(1);
    private final Map<Long, Callback> callbacks = Collections.synchronizedMap(new HashMap<>());
    private final Object removeCallbackLock = new Object();
    private final List<InternalSubListener> subscriptions = Collections.synchronizedList(new ArrayList<>());
    private final Map<Long, BytesArrayList> partials = new HashMap<>();
    private final Session session;

    public MercuryClient(@NotNull Session session) {
        this.session = session;
    }

    public void subscribe(@NotNull String uri, @NotNull SubListener listener) throws IOException, PubSubException {
        Response response = sendSync(RawMercuryRequest.sub(uri));
        if (response.statusCode != 200) throw new PubSubException(response);

        if (response.payload.size() > 0) {
            for (byte[] payload : response.payload) {
                Pubsub.Subscription sub = Pubsub.Subscription.parseFrom(payload);
                subscriptions.add(new InternalSubListener(sub.getUri(), listener, true));
            }
        } else {
            subscriptions.add(new InternalSubListener(uri, listener, true));
        }

        ConsoleLoggingModules.debug("Subscribed successfully to " + uri + "!");
    }

    public void unsubscribe(@NotNull String uri) throws IOException, PubSubException {
        Response response = sendSync(RawMercuryRequest.unsub(uri));
        if (response.statusCode != 200) throw new PubSubException(response);

        subscriptions.removeIf(l -> l.matches(uri));
        ConsoleLoggingModules.debug("Unsubscribed successfully from " + uri + "!");
    }

    @NotNull
    public Response sendSync(@NotNull RawMercuryRequest request) throws IOException {
        SyncCallback callback = new SyncCallback();
        int seq = send(request, callback);

        try {
            Response resp = callback.waitResponse();
            if (resp == null)
                throw new IOException(String.format("Request timeout out, %d passed, yet no response. {seq: %d}", MERCURY_REQUEST_TIMEOUT, seq));

            return resp;
        } catch (InterruptedException ex) {
            throw new IOException(ex); // Wrapping to avoid having to dispatch yet another exception down the call stack
        }
    }

    @NotNull
    public <W extends JsonWrapper> W sendSync(@NotNull JsonMercuryRequest<W> request) throws IOException, MercuryException {
        Response resp = sendSync(request.request);
        if (resp.statusCode >= 200 && resp.statusCode < 300) return request.instantiate(resp);
        else throw new MercuryException(resp);
    }

    public <W extends JsonWrapper> void send(@NotNull JsonMercuryRequest<W> request, @NotNull JsonCallback<W> callback) {
        try {
            send(request.request, resp -> {
                if (resp.statusCode >= 200 && resp.statusCode < 300) callback.response(request.instantiate(resp));
                else callback.exception(new MercuryException(resp));
            });
        } catch (IOException ex) {
            callback.exception(ex);
        }
    }

    public <P extends Message> void send(@NotNull ProtobufMercuryRequest<P> request, @NotNull ProtoCallback<P> callback) {
        try {
            send(request.request, resp -> {
                if (resp.statusCode >= 200 && resp.statusCode < 300) {
                    try {
                        callback.response(new ProtoWrapperResponse<>(request.parser.parseFrom(resp.payload.stream())));
                    } catch (InvalidProtocolBufferException ex) {
                        callback.exception(ex);
                    }
                } else {
                    callback.exception(new MercuryException(resp));
                }
            });
        } catch (IOException ex) {
            callback.exception(ex);
        }
    }

    public int send(@NotNull RawMercuryRequest request, @NotNull Callback callback) throws IOException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytesOut);

        int seq;
        synchronized (seqHolder) {
            seq = seqHolder.getAndIncrement();
        }

        ConsoleLoggingModules.debug("Send Mercury request, seq: " + seq + ", uri: " + request.header.getUri() + ", method: " + request.header.getMethod());

        out.writeShort((short) 4); // Seq length
        out.writeInt(seq); // Seq

        out.writeByte(1); // Flags
        out.writeShort(1 + request.payload.length); // Parts count

        byte[] headerBytes = request.header.toByteArray();
        out.writeShort(headerBytes.length); // Header length
        out.write(headerBytes); // Header

        for (byte[] part : request.payload) { // Parts
            out.writeShort(part.length);
            out.write(part);
        }

        Packet.Type cmd = Packet.Type.forMethod(request.header.getMethod());
        session.send(cmd, bytesOut.toByteArray());

        callbacks.put((long) seq, callback);
        return seq;
    }

    @Override
    public void dispatch(@NotNull Packet packet) {
        ByteBuffer payload = ByteBuffer.wrap(packet.payload);
        int seqLength = payload.getShort();
        long seq;
        if (seqLength == 2) seq = payload.getShort();
        else if (seqLength == 4) seq = payload.getInt();
        else if (seqLength == 8) seq = payload.getLong();
        else throw new IllegalArgumentException("Unknown seq length: " + seqLength);

        byte flags = payload.get();
        short parts = payload.getShort();

        BytesArrayList partial = partials.get(seq);
        if (partial == null || flags == 0) {
            partial = new BytesArrayList();
            partials.put(seq, partial);
        }

        ConsoleLoggingModules.debug("Handling packet, cmd: " + packet.type() + ", seq: " + seq + ", flags: " + flags + ", parts: " + parts);

        for (int i = 0; i < parts; i++) {
            short size = payload.getShort();
            byte[] buffer = new byte[size];
            payload.get(buffer);
            partial.add(buffer);
        }

        if (flags != 1) return;

        partials.remove(seq);

        Mercury.Header header;
        try {
            header = Mercury.Header.parseFrom(partial.get(0));
        } catch (InvalidProtocolBufferException ex) {
            ConsoleLoggingModules.error("Couldn't parse header! {bytes: " + Utils.bytesToHex(partial.get(0)) + "}");
            return;
        }

        Response resp = new Response(header, partial);

        if (packet.is(Packet.Type.MercuryEvent)) {
            boolean dispatched = false;
            synchronized (subscriptions) {
                for (InternalSubListener sub : subscriptions) {
                    if (sub.matches(header.getUri())) {
                        sub.dispatch(resp);
                        dispatched = true;
                    }
                }
            }

            if (!dispatched)
                ConsoleLoggingModules.debug("Couldn't dispatch Mercury event {seq: " + seq + ", uri: " + header.getUri() + ", code: " + header.getStatusCode() + ", payload: " + resp.payload.toHex() + "}");
        } else if (packet.is(Packet.Type.MercuryReq) || packet.is(Packet.Type.MercurySub) || packet.is(Packet.Type.MercuryUnsub)) {
            Callback callback = callbacks.remove(seq);
            if (callback != null)
                callback.response(resp);
            else
                ConsoleLoggingModules.warning("Skipped Mercury response, seq: " + seq + ", uri: " + header.getUri() + ", code: " + header.getStatusCode());

            synchronized (removeCallbackLock) {
                removeCallbackLock.notifyAll();
            }
        } else {
            ConsoleLoggingModules.warning("Couldn't handle packet, seq: " + seq + ", uri: " + header.getUri() + ", code: " + header.getStatusCode());
        }
    }

    public void interestedIn(@NotNull SubListener listener, @NotNull String uri) {
        subscriptions.add(new InternalSubListener(uri, listener, false));
    }

    public void notInterested(@NotNull SubListener listener) {
        subscriptions.removeIf(internalSubListener -> internalSubListener.listener == listener);
    }

    @Override
    public void close() {
        if (!subscriptions.isEmpty()) {
            for (InternalSubListener listener : new ArrayList<>(subscriptions)) {
                try {
                    if (listener.isSub) unsubscribe(listener.uri);
                    else notInterested(listener.listener);
                } catch (IOException | MercuryException ex) {
                    ConsoleLoggingModules.error("Failed unsubscribing. " + ex.getMessage());
                }
            }
        }

        if (!callbacks.isEmpty()) {
            synchronized (removeCallbackLock) {
                try {
                    removeCallbackLock.wait(MERCURY_REQUEST_TIMEOUT + 100);
                } catch (InterruptedException ignored) {
                }
            }
        }

        callbacks.clear();
    }

    public interface JsonCallback<W extends JsonWrapper> {
        void response(@NotNull W json);

        void exception(@NotNull Exception ex);
    }

    public interface ProtoCallback<M extends Message> {
        void response(@NotNull ProtoWrapperResponse<M> proto);

        void exception(@NotNull Exception ex);
    }

    public interface Callback {
        void response(@NotNull Response response);
    }

    private static class SyncCallback implements Callback {
        private final AtomicReference<Response> reference = new AtomicReference<>();

        @Override
        public void response(@NotNull Response response) {
            synchronized (reference) {
                reference.set(response);
                reference.notifyAll();
            }
        }

        @Nullable
        Response waitResponse() throws InterruptedException {
            synchronized (reference) {
                reference.wait(MERCURY_REQUEST_TIMEOUT);
                return reference.get();
            }
        }
    }

    public static class ProtoWrapperResponse<P extends Message> {
        private final P proto;
        private JsonElement json;

        ProtoWrapperResponse(@NotNull P proto) {
            this.proto = proto;
        }

        @NotNull
        public P proto() {
            return proto;
        }

        @NotNull
        public JsonObject json() {
            if (json == null) json = ProtobufToJson.convert(proto);
            return json.getAsJsonObject();
        }
    }

    public static class PubSubException extends MercuryException {
        private PubSubException(Response response) {
            super(response);
        }
    }

    private static class InternalSubListener {
        private final String uri;
        private final SubListener listener;
        private final boolean isSub;

        InternalSubListener(@NotNull String uri, @NotNull SubListener listener, boolean isSub) {
            this.uri = uri;
            this.listener = listener;
            this.isSub = isSub;
        }

        boolean matches(String uri) {
            return uri.startsWith(this.uri);
        }

        void dispatch(@NotNull Response resp) {
            listener.event(resp);
        }
    }

    public static class MercuryException extends Exception {
        public final int code;

        private MercuryException(@NotNull Response response) {
            super(String.format("status: %d", response.statusCode));
            this.code = response.statusCode;
        }
    }

    public static class Response {
        public final String uri;
        public final BytesArrayList payload;
        public final int statusCode;

        private Response(@NotNull Mercury.Header header, @NotNull BytesArrayList payload) {
            this.uri = header.getUri();
            this.statusCode = header.getStatusCode();
            this.payload = payload.copyOfRange(1, payload.size());
        }
    }
}

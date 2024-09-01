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

import com.spotifyxp.deps.xyz.gianlu.librespot.common.AsyncWorker;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.RawMercuryRequest;
import com.spotifyxp.logging.ConsoleLoggingModules;
import com.spotifyxp.utils.GraphicalMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * @author Gianlu
 */
public final class EventService implements Closeable {
    private AsyncWorker<EventBuilder> asyncWorker;

    int passes = 0;
    EventService(@NotNull Session session) {
        this.asyncWorker = new AsyncWorker<>("event-service-sender", eventBuilder -> {
            try {
                byte[] body = eventBuilder.toArray();
                MercuryClient.Response resp = session.mercury().sendSync(RawMercuryRequest.newBuilder()
                        .setUri("hm://event-service/v1/events").setMethod("POST")
                        .addUserField("Accept-Language", "en")
                        .addUserField("X-ClientTimeStamp", String.valueOf(TimeProvider.currentTimeMillis()))
                        .addPayloadPart(body)
                        .build());

                ConsoleLoggingModules.debug("Event sent. {body: {}, result: {}}", EventBuilder.toString(body), resp.statusCode);
                passes = 0;
            } catch (IOException ex) {
                if(passes>10) {
                    GraphicalMessage.sorryError();
                }
                //GraphicalMessage.openException(ex); Shut up
                ConsoleLoggingModules.error("Failed sending event: " + eventBuilder, ex);
                passes++;
                retry(session);
            }
        });
    }
    void retry(Session session) {
        this.asyncWorker = new AsyncWorker<>("event-service-sender", eventBuilder -> {
            try {
                byte[] body = eventBuilder.toArray();
                MercuryClient.Response resp = session.mercury().sendSync(RawMercuryRequest.newBuilder()
                        .setUri("hm://event-service/v1/events").setMethod("POST")
                        .addUserField("Accept-Language", "en")
                        .addUserField("X-ClientTimeStamp", String.valueOf(TimeProvider.currentTimeMillis()))
                        .addPayloadPart(body)
                        .build());

                ConsoleLoggingModules.debug("Event sent. {body: {}, result: {}}", EventBuilder.toString(body), resp.statusCode);
                passes = 0;
            } catch (IOException ex) {
                if(passes>10) {
                    GraphicalMessage.sorryError();
                }
                //GraphicalMessage.openException(ex); Shut up
                ConsoleLoggingModules.error("Failed sending event: " + eventBuilder, ex);
                passes++;
                retry(session);
            }
        });
    }

    public void sendEvent(@NotNull GenericEvent event) {
        sendEvent(event.build());
    }

    public void sendEvent(@NotNull EventBuilder builder) {
        asyncWorker.submit(builder);
    }

    @Override
    public void close() {
        asyncWorker.close();

        try {
            asyncWorker.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public enum Type {
        NEW_SESSION_ID("557", "3"), NEW_PLAYBACK_ID("558", "1"), TRACK_TRANSITION("12", "38");

        private final String id;
        private final String unknown;

        Type(@NotNull String id, @NotNull String unknown) {
            this.id = id;
            this.unknown = unknown;
        }
    }

    public interface GenericEvent {
        @NotNull
        EventBuilder build();
    }

    @SuppressWarnings("NullableProblems")
    public static class EventBuilder {
        private final ByteArrayOutputStream body = new ByteArrayOutputStream(256);

        public EventBuilder(@NotNull Type type) {
            appendNoDelimiter(type.id);
            append(type.unknown);
        }

        @NotNull
        static String toString(@NotNull byte[] body) {
            StringBuilder result = new StringBuilder();
            for (byte b : body) {
                if (b == 0x09) result.append('|');
                else result.append((char) b);
            }

            return result.toString();
        }

        private void appendNoDelimiter(@Nullable String str) {
            if (str == null) str = "";

            try {
                body.write(str.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }

        @NotNull
        public EventBuilder append(char c) {
            body.write(0x09);
            body.write(c);
            return this;
        }

        @NotNull
        public EventBuilder append(@Nullable String str) {
            body.write(0x09);
            appendNoDelimiter(str);
            return this;
        }

        @Override
        public String toString() {
            return "EventBuilder{" + toString(toArray()) + '}';
        }

        @NotNull
        byte[] toArray() {
            return body.toByteArray();
        }
    }
}

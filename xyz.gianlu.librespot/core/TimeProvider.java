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
import com.spotifyxp.logging.ConsoleLoggingModules;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Gianlu
 */
public final class TimeProvider {
    private static final AtomicLong offset = new AtomicLong(0);
    
    private static Method method = Method.NTP;

    private TimeProvider() {
    }

    @SuppressWarnings("DataFlowIssue")
    public static void init(@NotNull Session.Configuration conf) {
        method = Method.MANUAL;
        //switch (method = conf.timeSynchronizationMethod) {
        switch (method) {
            case NTP:
                try {
                    updateWithNtp();
                } catch (IOException ex) {
                    ConsoleLoggingModules.warning("Failed updating time!", ex);
                }
                break;
            case MANUAL:
                synchronized (offset) {
                    offset.set(conf.timeManualCorrection);
                }
                break;
            default:
            case PING:
            case MELODY:
                break;
        }
    }

    public static void init(@NotNull Session session) {
        if (method != Method.MELODY) return;

        updateMelody(session);
    }

    public static long currentTimeMillis() {
        synchronized (offset) {
            return System.currentTimeMillis() + offset.get();
        }
    }

    private static void updateWithNtp() throws IOException {
        try {
            synchronized (offset) {
                NTPUDPClient client = new NTPUDPClient();
                client.open();
                client.setSoTimeout((int) TimeUnit.SECONDS.toMillis(10));
                TimeInfo info = client.getTime(InetAddress.getByName("time.google.com"));
                info.computeDetails();
                Long offsetValue = info.getOffset();
                ConsoleLoggingModules.debug("Loaded time offset from NTP: {}ms", offsetValue);
                offset.set(offsetValue == null ? 0 : offsetValue);
            }
        } catch (SocketTimeoutException ex) {
            updateWithNtp();
        }
    }

    private static void updateMelody(@NotNull Session session) {
        try (Response resp = session.api().send("OPTIONS", "/melody/v1/time", null, null)) {
            if (resp.code() != 200) {
                ConsoleLoggingModules.error("Failed notifying server of time request! {code: {}, msg: {}}", resp.code(), resp.message());
                return;
            }
        } catch (IOException | MercuryClient.MercuryException ex) {
            ConsoleLoggingModules.error("Failed notifying server of time request!", ex);
            return;
        }

        try (Response resp = session.api().send("GET", "/melody/v1/time", null, null)) {
            if (resp.code() != 200) {
                ConsoleLoggingModules.error("Failed requesting time! {code: {}, msg: {}}", resp.code(), resp.message());
                return;
            }

            ResponseBody body = resp.body();
            if (body == null) throw new IllegalStateException();

            JsonObject obj = JsonParser.parseString(body.string()).getAsJsonObject();
            long diff = obj.get("timestamp").getAsLong() - System.currentTimeMillis();
            synchronized (offset) {
                offset.set(diff);
            }

            ConsoleLoggingModules.info("Loaded time offset from melody: {}ms", diff);
        } catch (IOException | MercuryClient.MercuryException ex) {
            ConsoleLoggingModules.error("Failed requesting time!", ex);
        }
    }

    public static void updateWithPing(byte[] pingPayload) {
        if (method != Method.PING) return;

        synchronized (offset) {
            long diff = ByteBuffer.wrap(pingPayload).getInt() * 1000L - System.currentTimeMillis();
            offset.set(diff);

            ConsoleLoggingModules.debug("Loaded time offset from ping: {}ms", diff);
        }
    }

    public enum Method {
        NTP, PING, MELODY, MANUAL
    }
}

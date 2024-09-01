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

import com.spotifyxp.logging.ConsoleLoggingModules;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import org.jetbrains.annotations.NotNull;
import com.spotifyxp.deps.xyz.gianlu.librespot.api.handlers.*;

public class ApiServer {
    protected final RoutingHandler handler;
    protected final EventsHandler events = new EventsHandler();
    private final SessionWrapper wrapper;
    private final int port;
    private final String host;
    private Undertow undertow = null;

    public ApiServer(int port, @NotNull String host, @NotNull SessionWrapper wrapper) {
        AbsSessionHandler instanceHandler = InstanceHandler.forSession(this, wrapper);

        this.port = port;
        this.host = host;
        this.wrapper = wrapper;
        this.handler = new RoutingHandler()
                .post("/metadata/{type}/{uri}", new MetadataHandler(wrapper, true))
                .post("/metadata/{uri}", new MetadataHandler(wrapper, false))
                .post("/search/{query}", new SearchHandler(wrapper))
                .post("/token/{scope}", new TokensHandler(wrapper))
                .post("/profile/{user_id}/{action}", new ProfileHandler(wrapper))
                .post("/web-api/{endpoint}", new WebApiHandler(wrapper))
                .get("/instance", instanceHandler)
                .post("/instance/{action}", instanceHandler)
                .post("/discovery/{action}", new DiscoveryHandler())
                .get("/events", events)
                .setFallbackHandler(new PathHandler(ResponseCodeHandler.HANDLE_404)
                        .addPrefixPath("/web-api", new WebApiHandler(wrapper)));

        wrapper.setListener(events);
    }

    public void start() {
        if (undertow != null) throw new IllegalStateException("Already started!");

        undertow = Undertow.builder().addHttpListener(port, host, new CorsHandler(handler)).build();
        undertow.start();
        ConsoleLoggingModules.info("Server started on port {}!", port);
    }

    public void stop() {
        wrapper.clear();

        if (undertow != null) {
            undertow.stop();
            undertow = null;

            ConsoleLoggingModules.info("Server stopped!");
        }
    }
}

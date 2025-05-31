/*
 * Copyright [2023-2025] [Gianluca Beil]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.spotifyxp.api;

import com.spotifyxp.PublicValues;
import com.spotifyxp.listeners.PlayerListener;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;

public class Player {
    com.spotifyxp.deps.xyz.gianlu.librespot.player.Player player;
    SpotifyAPI api;

    /**
     * Retries building a working librespot-player instance
     */
    public void retry() {
        player = InstanceManager.getPlayerUtils().buildPlayer();
        ConsoleLogging.info(PublicValues.language.translate("debug.connection.ready"));
        player.addEventsListener(new PlayerListener(this));
    }

    /**
     * Destroys the librespot-player instance
     */
    public void destroy() {
        InstanceManager.getSpotifyPlayer().close();
        InstanceManager.setPlayer(null);
    }

    public Player(SpotifyAPI a) {
        api = a;
        player = InstanceManager.getPlayerUtils().buildPlayer();
        ConsoleLogging.info(PublicValues.language.translate("debug.connection.ready"));
        player.addEventsListener(new PlayerListener(this));
    }

    /**
     * Returns an instance of librespot-player
     *
     * @return an instance of librespot-player
     */
    public com.spotifyxp.deps.xyz.gianlu.librespot.player.Player getPlayer() {
        return player;
    }
}

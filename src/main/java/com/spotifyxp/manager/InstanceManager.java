/*
 * Copyright [2024-2025] [Gianluca Beil]
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
package com.spotifyxp.manager;

import com.spotifyxp.api.Player;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.utils.PlayerUtils;

/**
 * This class is a manager
 *
 * <br> Get Example: getUnofficialSpotifyApi()
 * <br> Set Example: setUnofficialSpotifyApi( [instance of UnofficialSpotifyAPI] )
 */
public class InstanceManager {
    static Player player;
    static UnofficialSpotifyAPI unofficialSpotifyAPI;
    static PlayerUtils playerUtils;

    public static Player getPlayer() {
        if (player == null) {
            player = new Player();
        }
        return player;
    }

    public static com.spotifyxp.deps.xyz.gianlu.librespot.player.Player getSpotifyPlayer() {
        if (player == null) {
            player = new Player();
        }
        return player.getPlayer();
    }

    public static void setPlayer(Player p) {
        player = p;
    }

    public static UnofficialSpotifyAPI getUnofficialSpotifyApi() {
        if (unofficialSpotifyAPI == null) {
            unofficialSpotifyAPI = new UnofficialSpotifyAPI();
        }
        return unofficialSpotifyAPI;
    }

    public static void setUnofficialSpotifyAPI(UnofficialSpotifyAPI api) {
        unofficialSpotifyAPI = api;
    }

    public static PlayerUtils getPlayerUtils() {
        if (playerUtils == null) {
            playerUtils = new PlayerUtils();
        }
        return playerUtils;
    }

    public static void setPlayerUtils(PlayerUtils utils) {
        playerUtils = utils;
    }

    public static void destroy() {
        player = null;
        unofficialSpotifyAPI = null;
        playerUtils = null;
    }
}

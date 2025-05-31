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

import com.spotifyxp.api.OAuthPKCE;
import com.spotifyxp.api.Player;
import com.spotifyxp.api.SpotifyAPI;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.deps.se.michaelthelin.spotify.SpotifyApi;
import com.spotifyxp.utils.PlayerUtils;

/**
 * This class is a manager
 *
 * <br> Get Example: getUnofficialSpotifyApi()
 * <br> Set Example: setUnofficialSpotifyApi( [instance of UnofficialSpotifyAPI] )
 */
public class InstanceManager {
    static SpotifyAPI api;
    static SpotifyApi sapi;
    static OAuthPKCE pkce;
    static Player player;
    static UnofficialSpotifyAPI unofficialSpotifyAPI;
    static PlayerUtils playerUtils;

    public static Player getPlayer() {
        if (player == null) {
            player = new Player(api);
        }
        return player;
    }

    public static com.spotifyxp.deps.xyz.gianlu.librespot.player.Player getSpotifyPlayer() {
        if (player == null) {
            player = new Player(api);
        }
        return player.getPlayer();
    }

    public static void setPlayer(Player p) {
        player = p;
    }

    public static OAuthPKCE getPkce() {
        if (pkce == null) {
            pkce = new OAuthPKCE();
        }
        return pkce;
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

    public static void setPkce(OAuthPKCE pk) {
        pkce = pk;
    }

    public static SpotifyAPI getSpotifyAPI() {
        if (api == null) {
            api = new SpotifyAPI();
        }
        return api;
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

    public static void setSpotifyAPI(SpotifyAPI a) {
        api = a;
    }

    public static void setSpotifyApi(SpotifyApi a) {
        sapi = a;
    }

    public static SpotifyApi getSpotifyApi() {
        if (sapi == null) {
            sapi = SpotifyApi.builder().setAccessToken(pkce.getToken()).build();
        }
        return sapi;
    }

    public static void destroy() {
        api = null;
        sapi = null;
        pkce = null;
        player = null;
        unofficialSpotifyAPI = null;
        playerUtils = null;
    }
}

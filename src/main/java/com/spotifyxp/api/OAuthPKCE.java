package com.spotifyxp.api;

import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.Token;

public class OAuthPKCE {
    private String token;

    public OAuthPKCE() {
        token = Token.getDefaultToken();
    }

    /**
     * Returns the Spotify api token that was generated from the spotify server
     *
     * @return API Token
     */
    public String getToken() {
        return token;
    }


    /**
     * Refreshes the Spotify api token
     */
    public void refresh() {
        token = Token.getDefaultToken();
        InstanceManager.getSpotifyApi().setAccessToken(token);
    }
}

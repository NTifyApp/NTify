package com.spotifyxp.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.spotifyxp.utils.ConnectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class GitHubAPI {

    public static class Release {
        public String name;
        public String tag_name;
        public List<Asset> assets;
    }

    public static class Asset {
        public String url;
        public String name;
        public int size;
    }

    public static List<Release> getReleases() throws IOException {
        return new Gson().fromJson(ConnectionUtils.makeGet("https://api.github.com/repos/SpotifyXP/SpotifyXP/releases", new HashMap<>()), new TypeToken<List<Release>>(){}.getType());
    }
}

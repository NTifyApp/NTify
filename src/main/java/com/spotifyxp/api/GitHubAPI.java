package com.spotifyxp.api;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.spotifyxp.utils.ConnectionUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

public class GitHubAPI {

    public static class Release implements Serializable {
        public String name;
        public String tag_name;
        public List<Asset> assets;
    }

    public static class Asset implements Serializable {
        public String url;
        public String name;
        public int size;
    }

    public static List<Release> getReleases() throws IOException {
        List<Release> release = new Gson().fromJson(ConnectionUtils.makeGet("https://api.github.com/repos/NTifyApp/NTify/releases", new HashMap<>()), new TypeToken<List<Release>>(){}.getType());
        for (Release r : release) {
            if(r.assets.isEmpty()) continue;
            r.assets.get(0).url = "https://github.com/NTifyApp/NTify/releases/download/" + r.tag_name + "/NTify.jar";
        }
        return release;
    }
}

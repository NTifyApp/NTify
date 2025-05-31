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

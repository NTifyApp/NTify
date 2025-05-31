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
package com.spotifyxp.testing;

import com.spotifyxp.Flags;
import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.configuration.Config;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.ArtistEventView;
import com.spotifyxp.lib.libLanguage;
import com.spotifyxp.protogens.Concert;
import com.spotifyxp.support.LinuxSupportModule;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.theming.themes.DarkGreen;
import okhttp3.OkHttpClient;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

public class Test {
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, IOException, InterruptedException, URISyntaxException, ClassNotFoundException {
        //new CustomSaveDir().runArgument(new File("data").getAbsolutePath()).run();

        /*for(SpotifyXPEvents events : SpotifyXPEvents.values()) {
            Events.register(events.getName(), true);
        }

        new LinuxSupportModule().run();

        PublicValues.config = new Config();
        PublicValues.language = new libLanguage(Initiator.class);
        PublicValues.language.setNoAutoFindLanguage("en");
        PublicValues.language.setLanguageFolder("lang");*/

        PublicValues.theme = new DarkGreen();
        PublicValues.theme.initTheme();
        //PublicValues.defaultHttpClient = new OkHttpClient();

        UnofficialSpotifyAPI.SpotifyBrowse browse = UnofficialSpotifyAPI.SpotifyBrowse.fromJSON(
                IOUtils.toString(new FileInputStream("homedump.json"))
        );
    }
}

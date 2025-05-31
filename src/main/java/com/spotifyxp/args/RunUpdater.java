/*
 * Copyright [2025] [Gianluca Beil]
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
package com.spotifyxp.args;

import com.spotifyxp.Initiator;
import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.se.michaelthelin.spotify.Base64;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.lib.libLanguage;
import com.spotifyxp.support.SupportModuleLoader;
import com.spotifyxp.updater.Updater;
import com.spotifyxp.updater.UpdaterUI;
import okhttp3.OkHttpClient;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

public class RunUpdater implements Argument {
    @Override
    public Runnable runArgument(String commands) {
        return new Runnable() {
            @Override
            public void run() {
                for(SpotifyXPEvents event : SpotifyXPEvents.values()) {
                    Events.register(event.getName(), true);
                }
                PublicValues.osType = libDetect.getDetectedOS();
                new SupportModuleLoader().loadModules();
                PublicValues.language = new libLanguage(Initiator.class);
                PublicValues.language.setLanguageFolder("lang");
                PublicValues.defaultHttpClient = new OkHttpClient();
                try {
                    ByteArrayInputStream stream = new ByteArrayInputStream(Base64.decode(commands));
                    new UpdaterUI().open((Updater.UpdateInfo) new ObjectInputStream(stream).readObject());
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
                while(true) {
                }
            }
        };
    }

    @Override
    public String getName() {
        return "run-updater";
    }

    @Override
    public String getDescription() {
        return "Run the updater";
    }

    @Override
    public boolean hasParameter() {
        return true;
    }
}

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
package com.spotifyxp.utils;

import com.spotifyxp.deps.com.spotify.context.ContextTrackOuterClass;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;

import java.util.ArrayList;
import java.util.Collections;

public class Shuffle {
    public static ArrayList<String> before = new ArrayList<>();

    public static void makeShuffle() {
        try {
            ArrayList<String> mixed = new ArrayList<>();
            for (ContextTrackOuterClass.ContextTrack t : InstanceManager.getSpotifyPlayer().tracks(true).next) {
                mixed.add(t.getUri());
            }
            before = mixed;
            Collections.shuffle(mixed);
            InstanceManager.getSpotifyPlayer().tracks(true).next.clear();
            for (String s : mixed) {
                InstanceManager.getSpotifyPlayer().addToQueue(s);
            }
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.openException(e);
        }
    }
}

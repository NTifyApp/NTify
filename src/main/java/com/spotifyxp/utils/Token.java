/*
 * Copyright [2023-2024] [Gianluca Beil]
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

import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.logging.ConsoleLogging;

import java.io.IOException;

public class Token {
    public static String getDefaultToken() {
        try {
            return PublicValues.session.tokens().getToken("ugc-image-upload user-read-playback-state user-modify-playback-state user-read-currently-playing app-remote-control streaming playlist-read-private playlist-read-collaborative playlist-modify-private playlist-modify-public user-follow-modify user-follow-read user-read-playback-position user-top-read user-read-recently-played user-library-modify user-library-read user-read-email user-read-private".split(" ")).accessToken;
        } catch (IOException | MercuryClient.MercuryException e) {
            ConsoleLogging.Throwable(e);
            return getToken(0, "ugc-image-upload user-read-playback-state user-modify-playback-state user-read-currently-playing app-remote-control streaming playlist-read-private playlist-read-collaborative playlist-modify-private playlist-modify-public user-follow-modify user-follow-read user-read-playback-position user-top-read user-read-recently-played user-library-modify user-library-read user-read-email user-read-private".split(" "));
        }
    }

    private static String getToken(int times, String... scopes) {
        if (times > 5) {
            GraphicalMessage.sorryErrorExit("Couldn't get token. Tried it 5 times tho");
        }
        int newTimes = times;
        newTimes++;
        try {
            return PublicValues.session.tokens().getToken(scopes).accessToken;
        } catch (IOException | MercuryClient.MercuryException e) {
            ConsoleLogging.Throwable(e);
            return getToken(newTimes, scopes);
        }
    }

    public static String getToken(String... scopes) {
        try {
            return PublicValues.session.tokens().getToken(scopes).accessToken;
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
            return getToken(0, scopes);
        }
    }
}

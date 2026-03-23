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
import com.spotifyxp.logging.ConsoleLogging;
import xyz.gianlu.librespot.core.TokenProvider;

import java.io.IOException;

public class Token {

    private static String getToken(int times) {
        if (times > 5) {
            GraphicalMessage.sorryErrorExit("Couldn't get token. Tried it 5 times tho");
        }
        int newTimes = times;
        newTimes++;
        try {
            return PublicValues.session.tokens().getToken().accessToken;
        } catch (IOException | TokenProvider.TokenException e) {
            ConsoleLogging.Throwable(e);
            return getToken(newTimes);
        }
    }

    public static String getToken() {
        try {
            return PublicValues.session.tokens().getToken().accessToken;
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
            return getToken(0);
        }
    }
}

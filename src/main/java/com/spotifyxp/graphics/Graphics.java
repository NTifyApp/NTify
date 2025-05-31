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
package com.spotifyxp.graphics;

import com.spotifyxp.PublicValues;
import com.spotifyxp.utils.Resources;

import java.io.InputStream;

public enum Graphics {
    DOTS("dots"),
    HEART("heart"),
    HEARTFILLED("heartfilled"),
    HISTORY("history"),
    HISTORYSELECTED("historyselected"),
    MICROPHONE("microphone"),
    MICROPHONESELECTED("microphoneselected"),
    NOTHINGPLAYING("nothingplaying"),
    PLAYERPAUSE("playerpause"),
    PLAYERPlAY("playerplay"),
    PLAYERPLAYNEXT("playerplaynext"),
    PLAYERPLAYPREVIOUS("playerplayprevious"),
    REPEAT("repeat"),
    REPEATSELECTED("repeatselected"),
    SETTINGS("settings"),
    SHUFFLE("shuffle"),
    SHUFFLESELECTED("shuffleselected"),
    USER("user"),
    VOLUMEFULL("volumefull"),
    VOLUMEHALF("volumehalf"),
    VOLUMEMUTE("volumemute"),
    ACCOUNT("account"),
    TRACK("track"),
    VIDEO("video"),
    VIDEOSELECTED("videoselected"),
    ALBUM("album"),
    PLAYLIST("playlist"),
    SHOW("podcast"),
    MVERTICAL("mvertical"),
    MVERTICALSELECTED("mverticalselected"),
    CLOSE("close"),
    REFRESH("refresh");
    final String path;

    Graphics(String resourcePath) {
        String fullPath = "/icons/" + resourcePath;
        switch (resourcePath) {
            case "historyselected":
            case "heartfilled":
            case "microphonefilled":
            case "microphoneselected":
            case "repeatselected":
            case "shuffleselected":
            case "nothingplaying":
            case "videoselected":
            case "heart":
                path = fullPath + ".svg";
                break;
            default:
                if (PublicValues.theme.isLight()) {
                    path = fullPath + "dark.svg";
                } else {
                    path = fullPath + "white.svg";
                }
                break;
        }
    }

    public String getPath() {
        return path;
    }

    public InputStream getInputStream() {
        return new Resources().readToInputStream(path);
    }
}
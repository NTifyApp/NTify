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
package com.spotifyxp.events;

public enum SpotifyXPEvents {
    queueUpdate("queueUpdate", "Fires when the player queue updates"),
    queueAdvance("queueAdvance", "Fires when the player queue advances"),
    queueRegress("queueRegress", "Fires when the player queue regresses"),
    addtoqueue("addToQueue", "Fires when a track should be added to the queue"),

    playerLockRelease("playerLockRelease", "Fires when the player finished processing the metadata"),
    playerSeekedForwards("playerseekedforwards", "Fires when the user seeked the track forwards"),
    playerSeekedBackwards("playerseekedbackwards", "Fires when the user seeked the track backwards"),
    playerresume("playerresume", "Fires when the player resumes"),
    playerpause("playerpause", "Fires when the player is paused"),
    trackNext("trackNext", "Fires when next track plays"),
    trackLoad("trackLoad", "Fires when the next track loads"),
    trackLoadFinished("trackLoadFinished", "Fires when the track loading is finished"),

    onFrameReady("frameReady", "Fires when the main JFrame finished building itself (before opening)"),
    onFrameVisible("frameVisible", "Fires when the main JFrame is visible"),

    internetConnectionDropped("internetConDrop", "Fires when the internet connection drops"),
    internetConnectionReconnected("internetConRec", "Fires when the internet gets reconnected"),

    apikeyrefresh("apikeyrefresh", "Fires when the api key refreshes"),
    injectorAPIReady("injectorAPIReady", "Fires when the injector api class has finished initializing"),
    recalculateSizes("recalcSizes", "Fires when the sizes of the JComponent's should be recalculated"),
    librarychange("librarychange", "Fires when something in the user's library changes");

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    private final String name;
    private final String description;

    SpotifyXPEvents(String name, String description) {
        this.name = name;
        this.description = description;
    }
}

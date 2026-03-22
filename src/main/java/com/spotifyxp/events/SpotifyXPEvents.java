/*
 * Copyright [2023-2026] [Gianluca Beil]
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

import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.PlayableId;

public class SpotifyXPEvents {
    /**
     * Fires when the player queue updates
     * <p>
     * Payload:
     * </p>
     * <ul>
     *   <li><b>Type:</b> {@link String}</li>
     *   <li><b>Can be null: </b> yes</li>
     *   <li><b>Description:</b> Spotify URI of the updated queue item</li>
     * </ul>
     */
    public static Event<String> queueUpdate = new Event<>();

    /**
     * Fires when the player queue advances
     */
    public static Event<Object> queueAdvance = new Event<>();

    /**
     * Fires when the player queue regresses
     */
    public static Event<Object> queueRegress = new Event<>();

    /**
     * Fires when a track should be added to the queue
     * <p>
     * Payload:
     * </p>
     * <ul>
     *   <li><b>Type:</b> {@link String}</li>
     *   <li><b>Description:</b> Spotify URI of the new queue item</li>
     * </ul>
     */
    public static Event<String> addToQueue = new Event<>();

    /**
     * Fires when the user seeked the track forwards
     */
    public static Event<Object> playerSeekedForwards = new Event<>();

    /**
     * Fires when the user seeked the track backwards
     */
    public static Event<Object> playerSeekedBackwards = new Event<>();

    /**
     * Fires when the playback resumes
     */
    public static Event<Object> playerResume = new Event<>();

    /**
     * Fires when the playback is paused
     */
    public static Event<Object> playerPause = new Event<>();

    /**
     * Fires when a new track plays
     */
    public static Event<Playable> trackNext = new Event<>();

    /**
     * Fires when a new track loads
     */
    public static Event<Object> trackLoad = new Event<>();

    /**
     * Fires when the track load finishes
     */
    public static Event<Object> trackLoadFinished = new Event<>();

    /**
     * Fires when the NTify window is populated
     */
    public static Event<Object> onFrameReady = new Event<>();

    /**
     * Fires when the NTify window is visible
     */
    public static Event<Object> onFrameVisible = new Event<>();

    /**
     * Fires after the plugin injector finished injecting all plugins
     */
    public static Event<Object> pluginsInjected = new Event<>();

    /**
     * Fires when the size of JComponents should be recalculated
     */
    public static Event<Object> recalcSizes = new Event<>();

    /**
     * Fires when something in the user's library changed
     * <p>
     * Payload:
     * </p>
     * <ul>
     *   <li><b>Type:</b> {@link LibraryChange}</li>
     *   <li><b>Description:</b> Info what changed in the library</li>
     * </ul>
     */
    public static Event<LibraryChange> libraryChange = new Event<>();
}

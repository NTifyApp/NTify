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
package com.spotifyxp.video;

import java.awt.*;

/** Because of hardware limitations, SpotifyXP can only have one video player.
 To let other classes that use the player know that another class wants to use it, the new class must
 call 'init' after 'isVideoPlaybackEnabled'. <b>!!! Call 'release' when you don't need video playback anymore !!!</b>
**/
public interface VLCPlayer {
    boolean isVideoPlaybackEnabled();
    Container getComponent();

    /**
     * @param onTakeover - Will be called when a new class requests video playback
     */
    void init(Runnable onTakeover);
    void play(String uriOrFile);
    void pause();
    void stop();
    void setLooping(boolean looping);
    boolean isLooping();
    boolean isPlaying();
    boolean wasReleased();
    void resume();
    void removeOnTakeOver();

    /**
     * Releases system resources needed for vlc
     */
    void release();
}

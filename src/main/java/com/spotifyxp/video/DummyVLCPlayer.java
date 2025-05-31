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

public class DummyVLCPlayer implements VLCPlayer {
    @Override
    public boolean isVideoPlaybackEnabled() {
        return false;
    }

    @Override
    public Container getComponent() {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public void init(Runnable onTakeover) {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public void play(String uriOrFile) {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public void pause() {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public void setLooping(boolean looping) {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public boolean isLooping() {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public boolean isPlaying() {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public boolean wasReleased() {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public void resume() {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public void removeOnTakeOver() {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }

    @Override
    public void release() {
        throw new UnsupportedOperationException("This is a dummy VLC player");
    }
}

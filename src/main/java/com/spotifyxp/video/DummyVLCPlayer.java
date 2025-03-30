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

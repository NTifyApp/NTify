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

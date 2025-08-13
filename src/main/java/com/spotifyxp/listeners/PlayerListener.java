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
package com.spotifyxp.listeners;

import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Episode;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Track;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.MetadataWrapper;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.PlayableId;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.Player;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.graphics.Graphics;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.PlayerArea;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.SVGUtils;
import com.spotifyxp.utils.SpotifyUtils;
import com.spotifyxp.utils.TrackUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.json.JSONException;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This class is the binding between the UI and librespot
 */
@SuppressWarnings("CanBeFinal")
public class PlayerListener implements Player.EventsListener {
    private final com.spotifyxp.api.Player pl;
    public static boolean pauseTimer = false;
    public static boolean locked = true;

    class PlayerThread extends TimerTask {
        public void run() {
            if (!pauseTimer) {
                if (!InstanceManager.getSpotifyPlayer().isPaused()) {
                    try {
                        PlayerArea.playerCurrentTime.setMaximum(TrackUtils.getSecondsFromMS(Objects.requireNonNull(pl.getPlayer().currentMetadata()).duration()));
                        PlayerArea.playerCurrentTime.setValue(TrackUtils.getSecondsFromMS(pl.getPlayer().time()));
                    } catch (NullPointerException ex) {
                        //No song is playing
                    }
                }
            }
        }
    }

    public static Timer timer = new Timer();

    public PlayerListener(com.spotifyxp.api.Player p) {
        pl = p;
        timer.schedule(new PlayerThread(), 0, 500);
    }

    @Override
    public void onContextChanged(@NotNull Player player, @NotNull String s) {

    }

    @Override
    public void onTrackChanged(@NotNull Player player, @NotNull PlayableId playableId, @Nullable MetadataWrapper metadataWrapper, boolean b) {
        try {
            Events.triggerEvent(SpotifyXPEvents.queueUpdate.getName(), playableId.toSpotifyUri());
            if (!TrackUtils.isTrackLiked(playableId.toSpotifyUri().split(":")[2])) {
                PlayerArea.heart.isFilled = false;
                PlayerArea.heart.setImage(Graphics.HEART.getPath());
            } else {
                PlayerArea.heart.isFilled = true;
                PlayerArea.heart.setImage(Graphics.HEARTFILLED.getPath());
            }
            if (PlayerArea.playerAreaLyricsButton.isFilled) {
                PublicValues.lyricsDialog.open(playableId.toSpotifyUri());
            }
            locked = false;
            Events.triggerEvent(SpotifyXPEvents.playerLockRelease.getName());
        }catch (Exception e) {
            ConsoleLogging.Throwable(e);
        }
    }

    @Override
    public void onPlaybackEnded(@NotNull Player player) {

    }

    @Override
    public void onPlaybackPaused(@NotNull Player player, long l) {
        PlayerArea.playerPlayPauseButton.setImage(Graphics.PLAYERPlAY.getPath());
        Events.triggerEvent(SpotifyXPEvents.playerpause.getName());
    }

    @Override
    public void onPlaybackResumed(@NotNull Player player, long l) {
        PlayerArea.playerPlayPauseButton.setImage(Graphics.PLAYERPAUSE.getPath());
        Events.triggerEvent(SpotifyXPEvents.playerresume.getName());
    }

    @Override
    public void onPlaybackFailed(@NotNull Player player, @NotNull Exception e) {
        if (e instanceof ArrayIndexOutOfBoundsException) {
            ConsoleLogging.warning("Invalid mp3 file");
            return;
        }
        ConsoleLogging.error("Player failed! retry");
        ConsoleLogging.Throwable(e);
    }

    @Override
    public void onTrackSeeked(@NotNull Player player, long l) {
        if (PlayerArea.playerCurrentTime.getValue() < TrackUtils.getSecondsFromMS(InstanceManager.getPlayer().getPlayer().time())) {
            //Backwards
            Events.triggerEvent(SpotifyXPEvents.playerSeekedBackwards.getName());
        } else {
            //Forwards
            Events.triggerEvent(SpotifyXPEvents.playerSeekedForwards.getName());
        }
        PlayerArea.playerCurrentTime.setValue(TrackUtils.getSecondsFromMS(l));
        locked = false;
        Events.triggerEvent(SpotifyXPEvents.playerLockRelease.getName());
    }

    @Override
    public void onMetadataAvailable(@NotNull Player player, @NotNull MetadataWrapper metadataWrapper) {
        if (metadataWrapper.isTrack()) {
            Metadata.Track track = metadataWrapper.track;
            StringBuilder artists = new StringBuilder();
            Events.triggerEvent(SpotifyXPEvents.trackNext.getName(), track);
            PlayerArea.playerPlayTimeTotal.setText(TrackUtils.getHHMMSSOfTrack(track.getDuration()));
            PlayerArea.playerTitle.setText(track.getName());
            for (Metadata.Artist artist : track.getArtistList()) {
                if (artists.toString().isEmpty()) {
                    artists.append(artist.getName());
                } else {
                    artists.append(", ").append(artist.getName());
                }
            }
            PlayerArea.playerDescription.setText(artists.toString());
            try {
                PlayerArea.playerImage.setImage(new URL(
                        "https://i.scdn.co/image/" +
                                Utils.bytesToHex(SpotifyUtils.getImageForSystem(track.getAlbum().getCoverGroup().getImageList()).getFileId()).toLowerCase()
                ).openStream());
            } catch (Exception e) {
                e.printStackTrace();
                ConsoleLogging.warning("Failed to load cover for track");
                PlayerArea.playerImage.setImage(SVGUtils.svgToImageInputStreamSameSize(Graphics.NOTHINGPLAYING.getInputStream(), PlayerArea.playerImage.getSize()));
            }
        } else if (metadataWrapper.isEpisode()) {
            Metadata.Episode episode = metadataWrapper.episode;
            Events.triggerEvent(SpotifyXPEvents.trackNext.getName(), episode);
            PlayerArea.playerPlayTimeTotal.setText(TrackUtils.getHHMMSSOfTrack(episode.getDuration()));
            PlayerArea.playerTitle.setText(episode.getName());
            PlayerArea.playerDescription.setText(episode.getShow().getPublisher());
            try {
                PlayerArea.playerImage.setImage(new URL(
                        "https://i.scdn.co/image/" +
                                Utils.bytesToHex(SpotifyUtils.getImageForSystem(episode.getCoverImage().getImageList()).getFileId()).toLowerCase()
                ).openStream());
            } catch (Exception e) {
                ConsoleLogging.warning("Failed to load cover for episode");
                PlayerArea.playerImage.setImage(SVGUtils.svgToImageInputStreamSameSize(Graphics.NOTHINGPLAYING.getInputStream(), PlayerArea.playerImage.getSize()));
            }
        } else {
            Events.triggerEvent(SpotifyXPEvents.trackNext.getName());
            PlayerArea.playerTitle.setText(metadataWrapper.getName());
            PlayerArea.playerDescription.setText(metadataWrapper.getArtist());
        }
    }

    @Override
    public void onPlaybackHaltStateChanged(@NotNull Player player, boolean b, long l) {

    }

    @Override
    public void onInactiveSession(@NotNull Player player, boolean b) {

    }

    @Override
    public void onVolumeChanged(@NotNull Player player, @Range(from = 0L, to = 1L) float v) {
        try {
            PlayerArea.playerAreaVolumeSlider.setValue(TrackUtils.roundVolumeToNormal(v));
        } catch (NullPointerException ex) {
            //ContentPanel is not visible yet
        }
    }


    @Override
    public void onPanicState(@NotNull Player player) {
        GraphicalMessage.openException(new UnknownError("PanicState in PlayerListener"));
        PublicValues.blockLoading = false;
        pl.retry();
    }

    @Override
    public void onStartedLoading(@NotNull Player player) {
        PublicValues.blockLoading = true;
    }

    @Override
    public void onFinishedLoading(@NotNull Player player) {
        Events.triggerEvent(SpotifyXPEvents.trackLoadFinished.getName());
    }
}

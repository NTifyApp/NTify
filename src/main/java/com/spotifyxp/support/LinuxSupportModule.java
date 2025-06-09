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
package com.spotifyxp.support;

import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.org.mpris.MPRIS;
import com.spotifyxp.deps.org.mpris.MPRISBuilder;
import com.spotifyxp.deps.org.mpris.Metadata;
import com.spotifyxp.deps.org.mpris.TypeRunnable;
import com.spotifyxp.deps.org.mpris.mpris.PlaybackStatus;
import com.spotifyxp.deps.se.michaelthelin.spotify.exceptions.detailed.NotFoundException;
import com.spotifyxp.deps.xyz.gianlu.librespot.audio.MetadataWrapper;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.PlayableId;
import com.spotifyxp.deps.xyz.gianlu.librespot.player.Player;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.utils.ApplicationUtils;
import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.exceptions.DBusException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * !!Warning!! This class will be stripped from the final executable if the flag linuxSupport is false
 */
public class LinuxSupportModule implements SupportModule {
    public static MPRIS mpris;

    @Override
    public String getOSName() {
        return "Linux";
    }

    @Override
    public void run() {
        PublicValues.enableMediaControl = false;
        if (!PublicValues.customSaveDir) {
            PublicValues.fileslocation = System.getProperty("user.home") + "/.local/share/" + ApplicationUtils.getName();
            PublicValues.appLocation = PublicValues.fileslocation;
            PublicValues.configfilepath = PublicValues.fileslocation + "/config.json";
            PublicValues.tempPath = System.getProperty("java.io.tmpdir");
        }
        try {
            mpris = new MPRISBuilder()
                    .setOnQuit(new Runnable() {
                        @Override
                        public void run() {
                            System.exit(0);
                        }
                    })
                    .setOnRaise(new Runnable() {
                        @Override
                        public void run() {
                            ContentPanel.frame.toFront();
                        }
                    })
                    .setSupportedUriSchemes("spotify:")
                    .setCanQuit(true)
                    .setCanRaise(true)
                    .setIdentity(ApplicationUtils.getName())
                    .setDesktopEntry(ApplicationUtils.getName())
                    .setOnOpenURI(new TypeRunnable<String>() {
                        @Override
                        public void run(String value) {
                            if(value.split(":").length == 2) {
                                // URI
                                InstanceManager.getSpotifyPlayer().load(value, true, PublicValues.shuffle);
                            }
                        }
                    })
                    .setCanControl(true)
                    .setCanPlay(true)
                    .setCanPause(true)
                    .setCanGoNext(true)
                    .setCanGoPrevious(true)
                    .setOnPlayPause(new Runnable() {
                        @Override
                        public void run() {
                            InstanceManager.getPlayer().getPlayer().playPause();
                        }
                    })
                    .setOnPlay(new Runnable() {
                        @Override
                        public void run() {
                            InstanceManager.getPlayer().getPlayer().play();
                        }
                    })
                    .setOnPause(new Runnable() {
                        @Override
                        public void run() {
                            InstanceManager.getPlayer().getPlayer().pause();
                        }
                    })
                    .setOnNext(new Runnable() {
                        @Override
                        public void run() {
                            InstanceManager.getPlayer().getPlayer().next();
                        }
                    })
                    .setOnPrevious(new Runnable() {
                        @Override
                        public void run() {
                            InstanceManager.getPlayer().getPlayer().previous();
                        }
                    })
                    .build(ApplicationUtils.getName());
        } catch (DBusException e) {
            ConsoleLogging.warning("Failed to initialize MPRIS support");
        }
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(!InstanceManager.getSpotifyPlayer().isPaused()) {
                    if(InstanceManager.getSpotifyPlayer().time() == -1) return;
                    mpris.setPosition((int) TimeUnit.MILLISECONDS.toMicros(InstanceManager.getSpotifyPlayer().time()));
                }
            }
        };
        Timer timer = new Timer();
        Events.subscribe(SpotifyXPEvents.injectorAPIReady.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                InstanceManager.getPlayer().getPlayer().addEventsListener(new Player.EventsListener() {
                    @Override
                    public void onContextChanged(@NotNull Player player, @NotNull String newUri) {

                    }

                    @Override
                    public void onTrackChanged(@NotNull Player player, @NotNull PlayableId id, @Nullable MetadataWrapper metadata, boolean userInitiated) {
                        mpris.setPosition(0);
                    }

                    @Override
                    public void onPlaybackEnded(@NotNull Player player) {
                        try {
                            mpris.setPlaybackStatus(PlaybackStatus.STOPPED);
                        } catch (DBusException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onPlaybackPaused(@NotNull Player player, long trackTime) {
                        try {
                            mpris.setPlaybackStatus(PlaybackStatus.PAUSED);
                        } catch (DBusException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onPlaybackResumed(@NotNull Player player, long trackTime) {
                        try {
                            mpris.setPlaybackStatus(PlaybackStatus.PLAYING);
                        } catch (DBusException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onPlaybackFailed(@NotNull Player player, @NotNull Exception e) {
                        try {
                            mpris.setPlaybackStatus(PlaybackStatus.STOPPED);
                        } catch (DBusException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    @Override
                    public void onTrackSeeked(@NotNull Player player, long trackTime) {
                        try {
                            mpris.emitSeeked((int) TimeUnit.MILLISECONDS.toMicros(trackTime));
                        } catch (DBusException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onMetadataAvailable(@NotNull Player player, @NotNull MetadataWrapper metadata) {
                        try {
                            assert metadata.id != null;
                            mpris.setMetadata(new Metadata.Builder()
                                    .setTrackID(new DBusPath("/"))
                                    .setTitle(metadata.getName())
                                    .setArtURL(URI.create(InstanceManager.getSpotifyApi().getTrack(metadata.id.toSpotifyUri().split(":")[2]).build().execute().getAlbum().getImages()[0].getUrl()))
                                    .setLength((int) TimeUnit.MILLISECONDS.toMicros(metadata.duration()))
                                    .setArtists(Collections.singletonList(metadata.getArtist()))
                                    .setAlbumName(metadata.getAlbumName())
                                    .build());
                        } catch (NotFoundException e) {
                            ConsoleLogging.warning("Resource not found in onMetadataAvailable");
                        } catch (DBusException | IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    @Override
                    public void onPlaybackHaltStateChanged(@NotNull Player player, boolean halted, long trackTime) {
                        if (halted) {
                            try {
                                mpris.setPlaybackStatus(PlaybackStatus.STOPPED);
                            } catch (DBusException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    }

                    @Override
                    public void onInactiveSession(@NotNull Player player, boolean timeout) {

                    }

                    @Override
                    public void onVolumeChanged(@NotNull Player player, @Range(from = 0, to = 1) float volume) {

                    }

                    @Override
                    public void onPanicState(@NotNull Player player) {
                        try {
                            mpris.setPlaybackStatus(PlaybackStatus.STOPPED);
                        } catch (DBusException ex) {
                            throw new RuntimeException(ex);
                        }
                    }

                    @Override
                    public void onStartedLoading(@NotNull Player player) {

                    }

                    @Override
                    public void onFinishedLoading(@NotNull Player player) {
                        if(player.isPaused()) {
                            try {
                                mpris.setPlaybackStatus(PlaybackStatus.PAUSED);
                            } catch (DBusException e) {
                                throw new RuntimeException(e);
                            }
                        }else if (player.isActive()) {
                            try {
                                mpris.setPlaybackStatus(PlaybackStatus.PLAYING);
                            } catch (DBusException e) {
                                throw new RuntimeException(e);
                            }
                        }else {
                            try {
                                mpris.setPlaybackStatus(PlaybackStatus.STOPPED);
                            } catch (DBusException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                });
                if(mpris != null) timer.schedule(task, 0, 1000);
            }
        });
    }
}
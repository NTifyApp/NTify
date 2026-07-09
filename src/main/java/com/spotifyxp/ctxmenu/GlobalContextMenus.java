/*
 * Copyright [2025-2026] [Gianluca Beil]
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
package com.spotifyxp.ctxmenu;

import com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotify.metadata.Metadata;
import com.spotifyxp.PublicValues;
import com.spotifyxp.dialogs.FollowPlaylist;
import com.spotifyxp.dialogs.SelectPlaylist;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.Queue;
import com.spotifyxp.utils.ClipboardUtil;
import com.spotifyxp.utils.TrackUtils;
import org.jetbrains.annotations.Nullable;
import xyz.gianlu.librespot.api.ApiClient;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.*;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public enum GlobalContextMenus {
    COPYURI(new ContextMenu.GlobalContextMenuItem() {
        @Override
        public Runnable toRun(JComponent component, @Nullable ArrayList<String> uris) {
            return new Runnable() {
                @Override
                public void run() {
                    JTable table = (JTable) component;
                    if(table.getSelectedRow() == -1) return;
                    ClipboardUtil.set(uris.get(table.getSelectedRow()));
                }
            };
        }

        @Override
        public String name() {
            return PublicValues.language.translate("general.copy_uri");
        }

        @Override
        public boolean shouldBeAdded(JComponent component, Class<?> containingClass) {
            return component instanceof JTable;
        }

        @Override
        public boolean showItem(JComponent component, ArrayList<String> uris) {
            return true;
        }
    }),
    ADDTOLIBRARY(new ContextMenu.GlobalContextMenuItem() {
        @Override
        public Runnable toRun(JComponent component, @Nullable ArrayList<String> uris) {
            return new Runnable() {
                @Override
                public void run() {
                    JTable table = (JTable) component;
                    if(table.getSelectedRow() == -1) return;
                    LibraryChange.Type libraryChangeType = LibraryChange.Type.TRACK;
                    switch (uris.get(table.getSelectedRow()).toLowerCase(Locale.ENGLISH).split(":")[1]) {
                        case "playlist":
                            try {
                                FollowPlaylist playlist = new FollowPlaylist(new FollowPlaylist.OnOptionSelected() {
                                    @Override
                                    public void optionSelected(boolean isPublic) {
                                        new Thread(() -> {
                                            try {
                                                PublicValues.session.api().playlist().follow(PlaylistId.fromUri(
                                                        uris.get(table.getSelectedRow())
                                                ), isPublic);
                                            }catch (IOException | TokenProvider.TokenException e) {
                                                ConsoleLogging.Throwable(e);
                                            }
                                        }, "Follow playlist").start();
                                    }
                                });
                                playlist.open();
                                libraryChangeType = LibraryChange.Type.PLAYLIST;
                            }catch (IOException e) {
                                ConsoleLogging.Throwable(e);
                            }
                            break;
                        case "show":
                            new Thread(() -> {
                                try {
                                    PublicValues.session.api().show().follow(ShowId.fromUri(
                                            uris.get(table.getSelectedRow()).split(":")[2]
                                    ));
                                }catch (IOException | TokenProvider.TokenException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }, "Save album").start();
                            libraryChangeType = LibraryChange.Type.SHOW;
                            break;
                        case "artist":
                            new Thread(() -> {
                                try {
                                    PublicValues.session.api().artist().follow(
                                            ArtistId.fromUri(uris.get(table.getSelectedRow()))
                                    );
                                }catch (IOException | TokenProvider.TokenException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }, "Save Artist").start();
                            libraryChangeType = LibraryChange.Type.ARTIST;
                            break;
                        case "track":
                            new Thread(() -> {
                                try {
                                    PublicValues.session.api().track().like(
                                            TrackId.fromUri(uris.get(table.getSelectedRow()))
                                    );
                                }catch (IOException | TokenProvider.TokenException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }, "Save track").start();
                            libraryChangeType = LibraryChange.Type.TRACK;
                            break;
                        case "episode":
                            new Thread(() -> {
                                /*try {
                                    InstanceManager.getSpotifyApi().saveEpisodesForCurrentUser(
                                            uris.get(table.getSelectedRow()).split(":")[2]
                                    ).build().execute();
                                }catch (IOException e) {
                                    ConsoleLogging.Throwable(e);
                                }*/
                                //ToDo: Reverse engineer episode saving
                                //Problem is that I don't know how the spotify client knows the playlist id when the user
                                //never saved an episode before
                                JOptionPane.showMessageDialog(ContentPanel.frame, "Not implemented yet");
                            }, "Save episode").start();
                            libraryChangeType = LibraryChange.Type.EPISODE;
                            break;
                        case "album":
                            new Thread(() -> {
                                try {
                                    PublicValues.session.api().album().add(
                                            AlbumId.fromUri(uris.get(table.getSelectedRow()))
                                    );
                                }catch (IOException | TokenProvider.TokenException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }, "Save album").start();
                            libraryChangeType = LibraryChange.Type.ALBUM;
                            break;
                    }
                    SpotifyXPEvents.libraryChange.trigger(new LibraryChange(
                            uris.get(table.getSelectedRow()).toLowerCase(Locale.ENGLISH),
                            libraryChangeType,
                            LibraryChange.Action.ADD
                    ));
                }
            };
        }

        @Override
        public String name() {
            return PublicValues.language.translate("general.add_to_library");
        }

        @Override
        public boolean shouldBeAdded(JComponent component, Class<?> containingClass) {
            return component instanceof JTable && !containingClass.getSimpleName().startsWith("Library");
        }

        @Override
        public boolean showItem(JComponent component, ArrayList<String> uris) {
            return true;
        }
    }),
    ADDTOPLAYLIST(new ContextMenu.GlobalContextMenuItem() {
        @Override
        public Runnable toRun(JComponent component, @Nullable ArrayList<String> uris) {
            return new Runnable() {
                @Override
                public void run() {
                    JTable table = (JTable) component;
                    if (table.getSelectedRow() == -1) return;
                    try {
                        SelectPlaylist playlist = new SelectPlaylist(new SelectPlaylist.onPlaylistSelected() {
                            @Override
                            public void playlistSelected(String uri) {
                                try {
                                    ArrayList<String> urisToBeAdded = new ArrayList<>();
                                    switch (uris.get(table.getSelectedRow()).toLowerCase(Locale.ENGLISH).split(":")[1]) {
                                        case "episode":
                                        case "track":
                                            urisToBeAdded.add(uris.get(table.getSelectedRow()));
                                            break;
                                        case "album":
                                            Metadata.Album album = PublicValues.session.api().album().getMetadata(AlbumId.fromUri(
                                                    uris.get(table.getSelectedRow())
                                            ));
                                            for (Metadata.Disc disc : album.getDiscList())
                                                for (Metadata.Track track : disc.getTrackList())
                                                    urisToBeAdded.add(TrackId.fromHex(Utils.bytesToHex(track.getGid())).toSpotifyUri());
                                    }

                                    PublicValues.session.api().playlist().addItems(
                                            PlaylistId.fromUri(uri),
                                            urisToBeAdded.toArray(new String[0])
                                    );
                                }catch (IOException | TokenProvider.TokenException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }
                        });
                        playlist.open();
                    }catch (IOException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }
            };
        }

        @Override
        public String name() {
            return PublicValues.language.translate("general.add_to_playlist");
        }

        @Override
        public boolean shouldBeAdded(JComponent component, Class<?> containingClass) {
            return component instanceof JTable;
        }

        @Override
        public boolean showItem(JComponent component, ArrayList<String> uris) {
            String idType = uris.get(((JTable) component).getSelectedRow()).split(":")[1];
            return idType.equalsIgnoreCase("episode")
                    || idType.equalsIgnoreCase("track") || idType.equalsIgnoreCase("album");
        }
    }),
    ALLTOQUEUE(new ContextMenu.GlobalContextMenuItem() {
        @Override
        public Runnable toRun(JComponent component, @Nullable ArrayList<String> uris) {
            return new Runnable() {
                @Override
                public void run() {
                    //Add to the player's queue per-item (cheap - in-memory mutation, the resulting
                    //state sync to Spotify Connect is debounced by librespot regardless of call rate)
                    //but fetch all metadata for the UI list in one batch instead of the N sequential
                    //blocking fetches that SpotifyXPEvents.addToQueue's per-item subscriber would do.
                    for(String s : uris) {
                        InstanceManager.getSpotifyPlayer().addToQueue(s);
                    }
                    //Match SpotifyXPEvents.addToQueue's subscriber in Queue.java: only append to
                    //the local UI cache if it's already populated, otherwise leave it for the next
                    //full rebuild (queueUpdate/makeVisible) so the view doesn't go out of sync with
                    //the player's real queue.
                    if (!Queue.queueUriCache.isEmpty()) {
                        try {
                            ApiClient.BatchedRequestHelper helper = new ApiClient.BatchedRequestHelper();
                            for(String s : uris) {
                                helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                        .setEntityUri(s)
                                        .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                                .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.TRACK_V4)
                                                .build())
                                        .build(), data -> {
                                    Metadata.Track track = Metadata.Track.parseFrom(data[0].getValue());
                                    Queue.queueUriCache.add(s);
                                    String a = TrackUtils.getArtists(track.getArtistList());
                                    Queue.queueListModel.addElement(track.getName() + " - " + a);
                                });
                            }
                            helper.execute(PublicValues.session.api(), (exception, response) -> ConsoleLogging.Throwable(exception));
                        } catch (Exception e) {
                            ConsoleLogging.Throwable(e);
                        }
                    }
                }
            };
        }

        @Override
        public String name() {
            return PublicValues.language.translate("general.add_all_to_queue");
        }

        @Override
        public boolean shouldBeAdded(JComponent component, Class<?> containingClass) {
            return !(containingClass.isAssignableFrom(Queue.class));
        }

        @Override
        public boolean showItem(JComponent component, ArrayList<String> uris) {
            if(uris == null) return false;
            boolean containsOtherThanTrackOrEpisode = false;
            for(String uri : uris) {
                if(!uri.split(":")[1].equalsIgnoreCase("track")
                && !uri.split(":")[1].equalsIgnoreCase("episode")) {
                    containsOtherThanTrackOrEpisode = true;
                    break;
                }
            }
            return !containsOtherThanTrackOrEpisode;
        }
    }),
    ADDTOQUEUE(new ContextMenu.GlobalContextMenuItem() {
        @Override
        public Runnable toRun(JComponent component, @Nullable ArrayList<String> uris) {
            return new Runnable() {
                @Override
                public void run() {
                    if(((JTable) component).getSelectedRow() == -1) return;
                    SpotifyXPEvents.addToQueue.trigger(uris.get(((JTable) component).getSelectedRow()));
                }
            };
        }

        @Override
        public String name() {
            return PublicValues.language.translate("general.add_to_queue");
        }

        @Override
        public boolean shouldBeAdded(JComponent component, Class<?> containingClass) {
            return component instanceof JTable;
        }

        @Override
        public boolean showItem(JComponent component, ArrayList<String> uris) {
            boolean containsOtherThanTrackOrEpisode = false;
            for(String uri : uris) {
                if(!uri.split(":")[1].equalsIgnoreCase("track")
                        && !uri.split(":")[1].equalsIgnoreCase("episode")) {
                    containsOtherThanTrackOrEpisode = true;
                    break;
                }
            }
            return !containsOtherThanTrackOrEpisode;
        }
    });

    private ContextMenu.GlobalContextMenuItem globalContextMenuItem;
    GlobalContextMenus(ContextMenu.GlobalContextMenuItem item) {
        this.globalContextMenuItem = item;
    }
    public ContextMenu.GlobalContextMenuItem getGlobalContextMenuItem() { return globalContextMenuItem; }
}

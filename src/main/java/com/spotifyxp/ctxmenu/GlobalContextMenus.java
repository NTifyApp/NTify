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
package com.spotifyxp.ctxmenu;

import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.se.michaelthelin.spotify.enums.ModelObjectType;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Paging;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import com.spotifyxp.dialogs.FollowPlaylist;
import com.spotifyxp.dialogs.SelectPlaylist;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.Queue;
import com.spotifyxp.utils.ClipboardUtil;
import org.jetbrains.annotations.Nullable;

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
            return PublicValues.language.translate("ui.general.copyuri");
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
                                                InstanceManager.getSpotifyApi().followPlaylist(
                                                        uris.get(table.getSelectedRow()).split(":")[2],
                                                        isPublic
                                                ).build().execute();
                                            }catch (IOException e) {
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
                                    InstanceManager.getSpotifyApi().saveShowsForCurrentUser(
                                            uris.get(table.getSelectedRow()).split(":")[2]
                                    ).build().execute();
                                }catch (IOException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }, "Save album").start();
                            libraryChangeType = LibraryChange.Type.SHOW;
                            break;
                        case "artist":
                            new Thread(() -> {
                                try {
                                    InstanceManager.getSpotifyApi().followArtistsOrUsers(
                                            ModelObjectType.ARTIST,
                                            new String[] {uris.get(table.getSelectedRow()).split(":")[2]}
                                    ).build().execute();
                                }catch (IOException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }, "Save Artist").start();
                            libraryChangeType = LibraryChange.Type.ARTIST;
                            break;
                        case "track":
                            new Thread(() -> {
                                try {
                                    InstanceManager.getSpotifyApi().saveTracksForUser(
                                            uris.get(table.getSelectedRow()).split(":")[2]
                                    ).build().execute();
                                }catch (IOException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }, "Save track").start();
                            libraryChangeType = LibraryChange.Type.TRACK;
                            break;
                        case "episode":
                            new Thread(() -> {
                                try {
                                    InstanceManager.getSpotifyApi().saveEpisodesForCurrentUser(
                                            uris.get(table.getSelectedRow()).split(":")[2]
                                    ).build().execute();
                                }catch (IOException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }, "Save episode").start();
                            libraryChangeType = LibraryChange.Type.EPISODE;
                            break;
                        case "album":
                            new Thread(() -> {
                                try {
                                    InstanceManager.getSpotifyApi().saveAlbumsForCurrentUser(
                                            uris.get(table.getSelectedRow()).split(":")[2]
                                    ).build().execute();
                                }catch (IOException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }, "Save album").start();
                            libraryChangeType = LibraryChange.Type.ALBUM;
                            break;
                    }
                    Events.triggerEvent(SpotifyXPEvents.librarychange.getName(), new LibraryChange(
                            uris.get(table.getSelectedRow()).toLowerCase(Locale.ENGLISH),
                            libraryChangeType,
                            LibraryChange.Action.ADD
                    ));
                }
            };
        }

        @Override
        public String name() {
            return PublicValues.language.translate("ui.general.addtolibrary");
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
                                int limit;
                                int offset;
                                int total;
                                try {
                                    ArrayList<String> urisToBeAdded = new ArrayList<>();
                                    switch (uris.get(table.getSelectedRow()).toLowerCase(Locale.ENGLISH).split(":")[1]) {
                                        case "episode":
                                        case "track":
                                            urisToBeAdded.add(uris.get(table.getSelectedRow()));
                                            break;
                                        case "album":
                                            limit = 50;
                                            Paging<TrackSimplified> albumTracks = InstanceManager.getSpotifyApi().getAlbumsTracks(uris.get(table.getSelectedRow()).split(":")[2])
                                                    .build().execute();
                                            total = albumTracks.getTotal();
                                            offset = 0;
                                            while(offset < total) {
                                                for(TrackSimplified albumTrack : albumTracks.getItems()) {
                                                    urisToBeAdded.add(albumTrack.getUri());
                                                    offset++;
                                                }
                                                InstanceManager.getSpotifyApi().getAlbumsTracks(uris.get(table.getSelectedRow()).split(":")[2])
                                                        .limit(limit)
                                                        .offset(offset)
                                                        .build().execute();
                                            }
                                    }
                                    offset = 0;
                                    while(offset < urisToBeAdded.size()) {
                                        InstanceManager.getSpotifyApi().addItemsToPlaylist(uri.split(":")[2],
                                                urisToBeAdded.subList(offset, Math.min(offset + 100, urisToBeAdded.size())).toArray(new String[0])
                                        ).build().execute();
                                        offset += 100;
                                    }
                                }catch (IOException e) {
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
            return PublicValues.language.translate("ui.general.addtoplaylist");
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
                    for(String s : uris) {
                        Events.triggerEvent(SpotifyXPEvents.addtoqueue.getName(), s);
                    }
                }
            };
        }

        @Override
        public String name() {
            return PublicValues.language.translate("ui.general.addalltoqueue");
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
                    Events.triggerEvent(SpotifyXPEvents.addtoqueue.getName(), uris.get(((JTable) component).getSelectedRow()));
                }
            };
        }

        @Override
        public String name() {
            return PublicValues.language.translate("ui.general.addtoqueue");
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

package com.spotifyxp.ctxmenu;

import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.se.michaelthelin.spotify.enums.ModelObjectType;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Paging;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import com.spotifyxp.dialogs.FollowPlaylist;
import com.spotifyxp.dialogs.SelectPlaylist;
import com.spotifyxp.events.Events;
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
                            break;
                    }
                }
            };
        }

        @Override
        public String name() {
            return PublicValues.language.translate("ui.general.addtolibrary");
        }

        @Override
        public boolean shouldBeAdded(JComponent component, Class<?> containingClass) {
            return component instanceof JTable;
        }

        @Override
        public boolean showItem(JComponent component, ArrayList<String> uris) {
            return !uris.get(((JTable) component).getSelectedRow()).split(":")[1].equalsIgnoreCase("artist");
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
                    if(uris.get(table.getSelectedRow()).split(":")[1].equalsIgnoreCase("artist")
                            || uris.get(table.getSelectedRow()).split(":")[1].equalsIgnoreCase("show")) {
                        JOptionPane.showMessageDialog(ContentPanel.frame, "Artists can't be added to a playlist");
                        return;
                    }
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
                                        case "playlist":
                                            limit = 50;
                                            Paging<PlaylistTrack> playlistTracks = InstanceManager.getSpotifyApi().getPlaylistsItems(uri.split(":")[2])
                                                    .limit(limit)
                                                    .build().execute();
                                            total = playlistTracks.getTotal();
                                            offset = 0;
                                            while(offset < total) {
                                                for(PlaylistTrack playlistTrack : playlistTracks.getItems()) {
                                                    urisToBeAdded.add(playlistTrack.getTrack().getUri());
                                                    offset++;
                                                }
                                                playlistTracks = InstanceManager.getSpotifyApi().getPlaylistsItems(uri.split(":")[2])
                                                        .limit(limit)
                                                        .offset(offset)
                                                        .build().execute();
                                            }
                                            break;
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
            return idType.equalsIgnoreCase("playlist") || idType.equalsIgnoreCase("episode")
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

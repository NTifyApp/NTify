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
package com.spotifyxp.panels;

import com.spotify.metadata.Metadata;
import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.AsyncMouseListener;
import com.spotifyxp.utils.TrackUtils;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.TrackId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class LibraryTracks extends JScrollPane implements View {
    public static DefTable librarySongList;
    public static final ArrayList<String> libraryUriCache = new ArrayList<>();
    public static ContextMenu contextMenu;
    public static Thread libraryThread;

    public LibraryTracks() {
        setVisible(false);

        librarySongList = new DefTable();
        librarySongList.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("ui.library.songlist.songname"), PublicValues.language.translate("ui.library.songlist.filesize"), PublicValues.language.translate("ui.library.songlist.bitrate"), PublicValues.language.translate("ui.library.songlist.length")}));
        librarySongList.getTableHeader().setForeground(PublicValues.globalFontColor);
        librarySongList.setForeground(PublicValues.globalFontColor);
        librarySongList.getColumnModel().getColumn(0).setPreferredWidth(347);
        librarySongList.getColumnModel().getColumn(3).setPreferredWidth(51);
        librarySongList.setFillsViewportHeight(true);
        librarySongList.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    InstanceManager.getPlayer().getPlayer().load(libraryUriCache.get(librarySongList.getSelectedRow()), true, PublicValues.shuffle);
                    Thread thread1 = new Thread(() -> TrackUtils.addAllToQueue(libraryUriCache, librarySongList), "Library add to queue");
                    thread1.start();
                }
            }
        }));
        setViewportView(librarySongList);

        SpotifyXPEvents.libraryChange.subscribe((change) -> {
            if(libraryUriCache.isEmpty()) return;
            if(change.getType() != LibraryChange.Type.TRACK) return;
            if(change.getAction() == LibraryChange.Action.ADD) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Metadata.Track track = PublicValues.session.api().track().getMetadata(TrackId.fromUri(change.getUri()));
                            libraryUriCache.add(0, change.getUri());
                            String a = TrackUtils.getArtists(track.getArtistList());
                            librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).insertRow(0, new Object[]{track.getName() + " - " + a, TrackUtils.calculateFileSizeKb(track.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDuration())}));
                        }catch (IOException | TokenProvider.TokenException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, "Library add track").start();
            }else {
                ((DefaultTableModel) librarySongList.getModel()).removeRow(libraryUriCache.indexOf(change.getUri()));
                libraryUriCache.remove(change.getUri());
            }
        });

        createcontextMenu();
    }

    public void loadLibrary() {
        libraryThread = new Thread(new Runnable() {
            public void run() {
                try {
                    UnofficialSpotifyAPI.LibraryTracksResponse response = UnofficialSpotifyAPI.getLibraryTracks(5000, 0);

                    for (UnofficialSpotifyAPI.UserLibraryTrackResponse item : response.data.me.library.tracks.items) {
                        libraryUriCache.add(item.track.uri);
                        StringBuilder artists = new StringBuilder();
                        for (int i = 0; i < item.track.data.artists.items.size(); i++)
                            artists.append(item.track.data.artists.items.get(i).data.profile.name).append(", ");
                        if (artists.length() > 0)
                            artists = new StringBuilder(artists.substring(0, artists.length() - 2));
                        StringBuilder finalArtists = artists;
                        librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).addRow(new Object[]{item.track.data.name + " - " + finalArtists, TrackUtils.calculateFileSizeKb(item.track.data.duration.totalMilliseconds), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(item.track.data.duration.totalMilliseconds)}));
                    }

                    if (response.data.me.library.tracks.totalCount > 5000) {
                        int loaded = 5000;
                        while (loaded < response.data.me.library.tracks.totalCount) {
                            UnofficialSpotifyAPI.LibraryTracksResponse pagedResponse = UnofficialSpotifyAPI.getLibraryTracks(5000, loaded);
                            for (UnofficialSpotifyAPI.UserLibraryTrackResponse item : pagedResponse.data.me.library.tracks.items) {
                                libraryUriCache.add(item.track.uri);
                                StringBuilder artists = new StringBuilder();
                                for (int i = 0; i < item.track.data.artists.items.size(); i++)
                                    artists.append(item.track.data.artists.items.get(i).data.profile.name).append(", ");
                                if (artists.length() > 0)
                                    artists = new StringBuilder(artists.substring(0, artists.length() - 2));
                                StringBuilder finalArtists = artists;
                                librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).addRow(new Object[]{item.track.data.name + " - " + finalArtists, TrackUtils.calculateFileSizeKb(item.track.data.duration.totalMilliseconds), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(item.track.data.duration.totalMilliseconds)}));
                            }
                            loaded += pagedResponse.data.me.library.tracks.items.size();
                        }
                    }
                } catch (Exception e) {
                    ConsoleLogging.error("Error loading users library! Library now locked");
                    throw new RuntimeException(e);
                }
            }
        }, "Library thread");
        libraryThread.start();
    }

    void createcontextMenu() {
        contextMenu = new ContextMenu(librarySongList, libraryUriCache, getClass());
        contextMenu.addItem(PublicValues.language.translate("ui.general.refresh"), () -> {
            libraryUriCache.clear();
            ((DefaultTableModel) librarySongList.getModel()).setRowCount(0);
            loadLibrary();
        });
        contextMenu.addItem("Add to queue", () -> {
            if(librarySongList.getSelectedRow() == -1) return;
            SpotifyXPEvents.addToQueue.trigger(libraryUriCache.get(librarySongList.getSelectedRow()));
        });
        contextMenu.addItem(PublicValues.language.translate("ui.general.remove"), () -> {
            try {
                PublicValues.session.api().track().remove(TrackId.fromUri(
                        libraryUriCache.get(librarySongList.getSelectedRow())
                ));

                SpotifyXPEvents.libraryChange.trigger(new LibraryChange(
                        libraryUriCache.get(librarySongList.getSelectedRow()),
                        LibraryChange.Type.TRACK,
                        LibraryChange.Action.REMOVE
                ));
            } catch (IOException | TokenProvider.TokenException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public void makeVisible() {
        setVisible(true);
    }

    @Override
    public void makeInvisible() {
        setVisible(false);
    }
}

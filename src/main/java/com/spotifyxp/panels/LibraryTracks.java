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
import com.spotifyxp.utils.AsyncUtils;
import com.spotifyxp.utils.TrackUtils;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.TrackId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LibraryTracks extends JScrollPane implements View {
    private static final String CACHE_ID = "tracks";

    public static DefTable librarySongList;
    public static final ArrayList<String> libraryUriCache = new ArrayList<>();
    public static ContextMenu contextMenu;
    public static Thread libraryThread;

    private static class TrackRow {
        String uri;
        String display;
        String filesize;
        String bitrate;
        String length;

        TrackRow(String uri, String display, String filesize, String bitrate, String length) {
            this.uri = uri;
            this.display = display;
            this.filesize = filesize;
            this.bitrate = bitrate;
            this.length = length;
        }
    }

    public LibraryTracks() {
        setVisible(false);

        librarySongList = new DefTable();
        librarySongList.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("general.name"), PublicValues.language.translate("general.filesize"), PublicValues.language.translate("general.bitrate"), PublicValues.language.translate("general.length")}));
        librarySongList.getTableHeader().setForeground(PublicValues.globalFontColor);
        librarySongList.setForeground(PublicValues.globalFontColor);
        librarySongList.getColumnModel().getColumn(0).setPreferredWidth(347);
        librarySongList.getColumnModel().getColumn(3).setPreferredWidth(51);
        librarySongList.setFillsViewportHeight(true);
        librarySongList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    AsyncUtils.run(() -> {
                        InstanceManager.getPlayer().getPlayer().load(libraryUriCache.get(librarySongList.getSelectedRow()), true, PublicValues.shuffle);
                        TrackUtils.addAllToQueue(libraryUriCache, librarySongList);
                    });
                }
            }
        });
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
                int removeIndex = libraryUriCache.indexOf(change.getUri());
                librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).removeRow(removeIndex));
                libraryUriCache.remove(change.getUri());
            }
        });

        createcontextMenu();
    }

    public void loadLibrary() {
        if (PublicValues.cache.namespace("LibraryTracks").has(CACHE_ID)) {
            loadFromCache();
            return;
        }

        libraryThread = new Thread(new Runnable() {
            public void run() {
                try {
                    ArrayList<TrackRow> rows = new ArrayList<>();
                    UnofficialSpotifyAPI.LibraryTracksResponse response = UnofficialSpotifyAPI.getLibraryTracks(5000, 0);

                    for (UnofficialSpotifyAPI.UserLibraryTrackResponse item : response.data.me.library.tracks.items) {
                        addTrackRow(rows, item);
                    }

                    if (response.data.me.library.tracks.totalCount > 5000) {
                        int loaded = 5000;
                        while (loaded < response.data.me.library.tracks.totalCount) {
                            UnofficialSpotifyAPI.LibraryTracksResponse pagedResponse = UnofficialSpotifyAPI.getLibraryTracks(5000, loaded);
                            for (UnofficialSpotifyAPI.UserLibraryTrackResponse item : pagedResponse.data.me.library.tracks.items) {
                                addTrackRow(rows, item);
                            }
                            loaded += pagedResponse.data.me.library.tracks.items.size();
                        }
                    }

                    PublicValues.cache.namespace("LibraryTracks").put(CACHE_ID, rows);
                } catch (Exception e) {
                    ConsoleLogging.error("Error loading users library! Library now locked");
                    throw new RuntimeException(e);
                }
            }
        }, "Library thread");
        libraryThread.start();
    }

    private void addTrackRow(ArrayList<TrackRow> rows, UnofficialSpotifyAPI.UserLibraryTrackResponse item) {
        libraryUriCache.add(item.track.uri);
        StringBuilder artists = new StringBuilder();
        for (int i = 0; i < item.track.data.artists.items.size(); i++)
            artists.append(item.track.data.artists.items.get(i).profile.name).append(", ");
        if (artists.length() > 0)
            artists = new StringBuilder(artists.substring(0, artists.length() - 2));

        String display = item.track.data.name + " - " + artists;
        String filesize = TrackUtils.calculateFileSizeKb(item.track.data.duration.totalMilliseconds);
        String bitrate = TrackUtils.getBitrate();
        String length = TrackUtils.getHHMMSSOfTrack(item.track.data.duration.totalMilliseconds);

        rows.add(new TrackRow(item.track.uri, display, filesize, bitrate, length));
        librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).addRow(new Object[]{display, filesize, bitrate, length}));
    }

    private void loadFromCache() {
        try {
            TrackRow[] rows = PublicValues.cache.namespace("LibraryTracks").get(CACHE_ID, TrackRow[].class);
            for (TrackRow row : rows) {
                libraryUriCache.add(row.uri);
                librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).addRow(new Object[]{row.display, row.filesize, row.bitrate, row.length}));
            }
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    private void saveToCache() {
        List<TrackRow> rows = new ArrayList<>();
        DefaultTableModel model = (DefaultTableModel) librarySongList.getModel();
        for (int i = 0; i < model.getRowCount() && i < libraryUriCache.size(); i++) {
            rows.add(new TrackRow(
                    libraryUriCache.get(i),
                    (String) model.getValueAt(i, 0),
                    (String) model.getValueAt(i, 1),
                    (String) model.getValueAt(i, 2),
                    (String) model.getValueAt(i, 3)
            ));
        }
        try {
            PublicValues.cache.namespace("LibraryTracks").put(CACHE_ID, rows);
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    void createcontextMenu() {
        contextMenu = new ContextMenu(librarySongList, libraryUriCache, getClass());
        contextMenu.addItem(PublicValues.language.translate("general.refresh"), () -> {
            libraryUriCache.clear();
            librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).setRowCount(0));
            try {
                PublicValues.cache.namespace("LibraryTracks").remove(CACHE_ID);
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            loadLibrary();
        });
        contextMenu.addItem("Add to queue", () -> {
            if(librarySongList.getSelectedRow() == -1) return;
            SpotifyXPEvents.addToQueue.trigger(libraryUriCache.get(librarySongList.getSelectedRow()));
        });
        contextMenu.addItem(PublicValues.language.translate("general.remove"), () -> {
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
        if (libraryUriCache.isEmpty()) {
            loadLibrary();
        }
    }

    @Override
    public void makeInvisible() {
        setVisible(false);
        if (!libraryUriCache.isEmpty()) {
            saveToCache();
            libraryUriCache.clear();
            librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).setRowCount(0));
        }
    }
}

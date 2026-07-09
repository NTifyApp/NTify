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

import com.google.gson.Gson;
import com.spotify.metadata.Metadata;
import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.TrackUtils;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.AlbumId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class LibraryAlbums extends JScrollPane{
    private static final String CACHE_ID = "albums";

    public static DefTable albumsTable;
    public static ArrayList<String> albumsUris;
    public static ContextMenu contextMenu;

    private static class AlbumRow {
        String uri;
        String name;
        String artists;

        AlbumRow(String uri, String name, String artists) {
            this.uri = uri;
            this.name = name;
            this.artists = artists;
        }
    }

    public LibraryAlbums() {
        albumsUris = new ArrayList<>();

        albumsTable = new DefTable();
        albumsTable.setModel(new DefaultTableModel(new Object[][] {}, new String[] {
                PublicValues.language.translate("general.name"),
                PublicValues.language.translate("general.artist")
        }));
        albumsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        albumsTable.setForeground(PublicValues.globalFontColor);
        albumsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    ContentPanel.trackPanel.open(albumsUris.get(albumsTable.getSelectedRow()), HomePanel.ContentTypes.album);
                }
            }
        });

        contextMenu = new ContextMenu(albumsTable, albumsUris, getClass());
        contextMenu.addItem(PublicValues.language.translate("general.refresh"), new Runnable() {
            @Override
            public void run() {
                albumsTable.addModifyAction(() -> ((DefaultTableModel) albumsTable.getModel()).setRowCount(0));
                albumsUris.clear();
                try {
                    PublicValues.cache.namespace("LibraryAlbums").remove(CACHE_ID);
                } catch (IOException e) {
                    ConsoleLogging.Throwable(e);
                }
                new Thread(() -> fetch()).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("general.remove"), new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        PublicValues.session.api().album().add(AlbumId.fromUri(
                                albumsUris.get(albumsTable.getSelectedRow())
                        ));
                        SpotifyXPEvents.libraryChange.trigger(new LibraryChange(
                                albumsUris.get(albumsTable.getSelectedRow()),
                                LibraryChange.Type.ALBUM,
                                LibraryChange.Action.REMOVE
                        ));
                    }catch (IOException | TokenProvider.TokenException e) {
                        throw new RuntimeException(e);
                    }
                }, "Remove from albums").start();
            }
        });

        SpotifyXPEvents.libraryChange.subscribe((change) -> {
            if(albumsUris.isEmpty()) return;
            if(change.getType() != LibraryChange.Type.ALBUM) return;
            if(change.getAction() == LibraryChange.Action.ADD) {
                new Thread(() -> {
                    try {
                        Metadata.Album album = PublicValues.session.api().album().getMetadata(AlbumId.fromUri(change.getUri()));
                        albumsUris.add(0, AlbumId.fromHex(Utils.bytesToHex(album.getGid())).toSpotifyUri());
                        albumsTable.addModifyAction(() -> {
                            ((DefaultTableModel) albumsTable.getModel()).insertRow(0, new Object[] {
                                    album.getName(),
                                    TrackUtils.getArtists(album.getArtistList())
                            });
                        });
                    }catch (IOException | TokenProvider.TokenException e) {
                        throw new RuntimeException(e);
                    }
                }, "Library add album").start();
            }else {
                for(int uri = 0; uri < albumsUris.size(); uri++) {
                    if(albumsUris.get(uri).equals(change.getUri())) {
                        albumsUris.remove(uri);
                        int finalUri = uri;
                        albumsTable.addModifyAction(() -> {
                            ((DefaultTableModel) albumsTable.getModel()).removeRow(finalUri);
                        });
                        return;
                    }
                }
            }
        });

        setViewportView(albumsTable);
    }

    private void fetch() {
        if (PublicValues.cache.namespace("LibraryAlbums").has(CACHE_ID)) {
            try {
                AlbumRow[] rows = PublicValues.cache.namespace("LibraryAlbums").get(CACHE_ID, AlbumRow[].class);
                for (AlbumRow row : rows) {
                    albumsUris.add(row.uri);
                    albumsTable.addModifyAction(() -> ((DefaultTableModel) albumsTable.getModel()).addRow(new Object[]{row.name, row.artists}));
                }
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            return;
        }

        try {
            ArrayList<AlbumRow> cacheRows = new ArrayList<>();
            int limit = 50;
            UnofficialSpotifyAPI.LibraryResponse response = UnofficialSpotifyAPI.getLibraryPage(new String[] {"Albums"}, null, limit, 0);
            UnofficialSpotifyAPI.LibraryPage libraryV3 = response.data.me.libraryV3;
            int total = libraryV3.totalCount;
            int offset = 0;
            Gson gson = new Gson();
            while(offset < total) {
                for(UnofficialSpotifyAPI.LibraryItemEntry albumItem : libraryV3.items) {
                    UnofficialSpotifyAPI.AlbumOfTrack album = gson.fromJson(albumItem.item.data.toString(), UnofficialSpotifyAPI.AlbumOfTrack.class);
                    albumsUris.add(album.uri);
                    StringBuilder artists = new StringBuilder();
                    for(UnofficialSpotifyAPI.ArtistItem artist : album.artists.items) {
                        artists.append(artist.profile.name).append(", ");
                    }
                    if (artists.length() != 0)
                        artists.delete(artists.length() - 2, artists.length());
                    String artistsStr = artists.toString();
                    cacheRows.add(new AlbumRow(album.uri, album.name, artistsStr));
                    albumsTable.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            ((DefaultTableModel) albumsTable.getModel()).addRow(new Object[]{
                                    album.name,
                                    artistsStr,
                            });
                        }
                    });
                    offset++;
                }
                response = UnofficialSpotifyAPI.getLibraryPage(new String[] {"Albums"}, null, limit, offset);
                libraryV3 = response.data.me.libraryV3;
            }
            PublicValues.cache.namespace("LibraryAlbums").put(CACHE_ID, cacheRows);
        }catch (IOException | TokenProvider.TokenException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    public void fill() {
        new Thread(() -> {
            fetch();
        }).start();
    }

    public static void evict() {
        if (albumsUris == null || albumsUris.isEmpty()) return;

        DefaultTableModel model = (DefaultTableModel) albumsTable.getModel();
        ArrayList<AlbumRow> rows = new ArrayList<>();
        for (int i = 0; i < model.getRowCount() && i < albumsUris.size(); i++) {
            rows.add(new AlbumRow(albumsUris.get(i), (String) model.getValueAt(i, 0), (String) model.getValueAt(i, 1)));
        }
        try {
            PublicValues.cache.namespace("LibraryAlbums").put(CACHE_ID, rows);
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }

        albumsUris.clear();
        albumsTable.addModifyAction(() -> model.setRowCount(0));
    }
}

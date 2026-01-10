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
import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.AlbumId;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.TrackUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class LibraryAlbums extends JScrollPane{
    public static DefTable albumsTable;
    public static ArrayList<String> albumsUris;
    public static ContextMenu contextMenu;

    public LibraryAlbums() {
        albumsUris = new ArrayList<>();

        albumsTable = new DefTable();
        albumsTable.setModel(new DefaultTableModel(new Object[][] {}, new String[] {
                PublicValues.language.translate("ui.general.name"),
                PublicValues.language.translate("ui.general.artist")
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
        contextMenu.addItem(PublicValues.language.translate("ui.general.refresh"), new Runnable() {
            @Override
            public void run() {
                ((DefaultTableModel) albumsTable.getModel()).setRowCount(0);
                albumsUris.clear();
                new Thread(() -> fetch()).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("ui.general.remove"), new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        PublicValues.session.api().album().add(AlbumId.fromUri(
                                albumsUris.get(albumsTable.getSelectedRow())
                        ));
                        Events.triggerEvent(SpotifyXPEvents.librarychange.getName(), new LibraryChange(
                                albumsUris.get(albumsTable.getSelectedRow()),
                                LibraryChange.Type.ALBUM,
                                LibraryChange.Action.REMOVE
                        ));
                    }catch (IOException | MercuryClient.MercuryException e) {
                        throw new RuntimeException(e);
                    }
                }, "Remove from albums").start();
            }
        });

        Events.subscribe(SpotifyXPEvents.librarychange.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                LibraryChange change = (LibraryChange) data[0];
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
                        }catch (IOException | MercuryClient.MercuryException e) {
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
            }
        });

        setViewportView(albumsTable);
    }

    private void fetch() {
        try {
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
                    albumsTable.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            String artists = "";
                            for(UnofficialSpotifyAPI.ArtistItem artist : album.artists.items) {
                                artists += artist.data.profile.name + ", ";
                            }
                            if (!artists.isEmpty())
                                artists = artists.substring(0, artists.length() - 2);
                            ((DefaultTableModel) albumsTable.getModel()).addRow(new Object[]{
                                    album.name,
                                    artists,
                            });
                        }
                    });
                    offset++;
                }
                response = UnofficialSpotifyAPI.getLibraryPage(new String[] {"Albums"}, null, limit, offset);
                libraryV3 = response.data.me.libraryV3;
            }
        }catch (IOException | MercuryClient.MercuryException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    public void fill() {
        new Thread(() -> {
            fetch();
        }).start();
    }
}

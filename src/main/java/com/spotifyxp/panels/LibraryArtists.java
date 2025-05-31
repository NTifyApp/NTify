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
package com.spotifyxp.panels;

import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.deps.se.michaelthelin.spotify.enums.ModelObjectType;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Artist;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.PagingCursorbased;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class LibraryArtists extends JScrollPane {
    public static DefTable artistsTable;
    public static ArrayList<String> artistsUris;
    public static ContextMenu contextMenu;

    public LibraryArtists() {
        artistsUris = new ArrayList<>();

        artistsTable = new DefTable();
        artistsTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{
                PublicValues.language.translate("ui.navigation.library.artists.table.column1"),
                PublicValues.language.translate("ui.navigation.library.artists.table.column2"),
                PublicValues.language.translate("ui.navigation.library.artists.table.column3")
        }));
        artistsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        artistsTable.setForeground(PublicValues.globalFontColor);
        artistsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    ContentPanel.showArtistPanel(artistsUris.get(artistsTable.getSelectedRow()));
                }
            }
        });

        contextMenu = new ContextMenu(artistsTable, artistsUris, getClass());
        contextMenu.addItem(PublicValues.language.translate("ui.general.refresh"), new Runnable() {
            @Override
            public void run() {
                ((DefaultTableModel) artistsTable.getModel()).setRowCount(0);
                artistsUris.clear();
                new Thread(() -> fetch()).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("ui.library.tabs.artists.ctxmenu.remove"), new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        InstanceManager.getSpotifyApi().unfollowArtistsOrUsers(
                                ModelObjectType.ARTIST,
                                new String[]{
                                        artistsUris.get(artistsTable.getSelectedRow()).split(":")[2]
                                }
                        ).build().execute();
                    } catch (IOException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }).start();
            }
        });

        setViewportView(artistsTable);
    }

    private void fetch() {
        try {
            int limit = 50;
            PagingCursorbased<Artist> artists = InstanceManager.getSpotifyApi().getUsersFollowedArtists(
                    ModelObjectType.ARTIST
            ).build().execute();
            int total = artists.getTotal();
            int offset = 0;
            while(offset < total) {
                String lastArtist = "";
                for(Artist artist : artists.getItems()) {
                    artistsUris.add(artist.getUri());
                    artistsTable.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            ((DefaultTableModel) artistsTable.getModel()).addRow(new Object[]{
                                    artist.getName(),
                                    artist.getFollowers().getTotal(),
                                    String.join(", ", artist.getGenres())
                            });
                        }
                    });
                    lastArtist = artist.getId();
                    offset++;
                }
                artists = InstanceManager.getSpotifyApi().getUsersFollowedArtists(
                        ModelObjectType.ARTIST
                ).limit(limit).after(lastArtist).build().execute();
            }
        }catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    public void fill() {
        new Thread(() -> {
            fetch();
        }).start();
    }
}

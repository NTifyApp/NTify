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
import com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotify.metadata.Metadata;
import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.utils.SpotifyUtils;
import xyz.gianlu.librespot.api.ApiClient;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.ArtistId;

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
                        PublicValues.session.api().artist().unfollow(
                                ArtistId.fromUri(artistsUris.get(artistsTable.getSelectedRow()))
                        );
                        SpotifyXPEvents.libraryChange.trigger(new LibraryChange(
                                artistsUris.get(artistsTable.getSelectedRow()),
                                LibraryChange.Type.ARTIST,
                                LibraryChange.Action.REMOVE
                        ));
                    } catch (IOException | TokenProvider.TokenException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }).start();
            }
        });

        SpotifyXPEvents.libraryChange.subscribe((change) -> {
            if(artistsUris.isEmpty()) return;
            if(change.getType() != LibraryChange.Type.ARTIST) return;
            if(change.getAction() == LibraryChange.Action.ADD) {
                new Thread(() -> {
                    try {
                        ExtendedMetadata.BatchedExtensionResponse response = PublicValues.session.api().getExtendedMetadata(ExtendedMetadata.BatchedEntityRequest.newBuilder()
                                .addEntityRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                        .setEntityUri(change.getUri())
                                        .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                                .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ARTIST_V4)
                                                .build())
                                        .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                                .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ON_PLATFORM_REPUTATION_TRAIT)
                                                .build())
                                        .build())
                                .build());
                        Metadata.Artist artist = Metadata.Artist.parseFrom(response.getExtendedMetadata(0).getExtensionData(1).getExtensionData().getValue());
                        ExtendedMetadata.OnPlatformReputationTrait reputationTrait = ExtendedMetadata.OnPlatformReputationTrait.parseFrom(response.getExtendedMetadata(0).getExtensionData(0).getExtensionData().getValue());
                        artistsUris.add(0, change.getUri());
                        artistsTable.addModifyAction(new Runnable() {
                            @Override
                            public void run() {
                                ((DefaultTableModel) artistsTable.getModel()).insertRow(0, new Object[]{
                                        artist.getName(),
                                        SpotifyUtils.formatMonthlyListeners(reputationTrait.getMonthlyListeners()),
                                        String.join(", ", artist.getGenreList())
                                });
                            }
                        });
                    } catch (IOException | TokenProvider.TokenException e) {
                        throw new RuntimeException(e);
                    }
                }, "Library add artist").start();
            }else{
                for(int uri = 0; uri < artistsUris.size(); uri++) {
                    if(artistsUris.get(uri).equals(change.getUri())) {
                        artistsUris.remove(uri);
                        int finalUri = uri;
                        artistsTable.addModifyAction(new Runnable() {
                            @Override
                            public void run() {
                                ((DefaultTableModel) artistsTable.getModel()).removeRow(finalUri);
                            }
                        });
                        return;
                    }
                }
            }
        });

        setViewportView(artistsTable);
    }



    private void fetch() {
        try {
            int limit = 50;
            UnofficialSpotifyAPI.LibraryResponse response = UnofficialSpotifyAPI.getLibraryPage(new String[] {"Artists"}, null, limit, 0);
            UnofficialSpotifyAPI.LibraryPage libraryV3 = response.data.me.libraryV3;
            int total = libraryV3.totalCount;
            int offset = 0;
            Gson gson = new Gson();
            while(offset < total) {
                ApiClient.BatchedRequestHelper helper = new ApiClient.BatchedRequestHelper();
                for(UnofficialSpotifyAPI.LibraryItemEntry artistItem : libraryV3.items) {
                    UnofficialSpotifyAPI.ArtistItem artistItemData = gson.fromJson(artistItem.item.data, UnofficialSpotifyAPI.ArtistItem.class);
                    helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                    .setEntityUri(artistItem.item.uri)
                                    .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                            .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ARTIST_V4)
                                            .build())
                                    .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                            .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ON_PLATFORM_REPUTATION_TRAIT)
                                            .build())
                            .build(), data -> {
                        Metadata.Artist artist = Metadata.Artist.parseFrom(data[0].getValue());
                        ExtendedMetadata.OnPlatformReputationTrait reputationTrait = ExtendedMetadata.OnPlatformReputationTrait.parseFrom(data[1].getValue());
                        artistsUris.add(artistItemData.uri);
                        artistsTable.addModifyAction(new Runnable() {
                            @Override
                            public void run() {
                                ((DefaultTableModel) artistsTable.getModel()).addRow(new Object[]{
                                        artist.getName(),
                                        SpotifyUtils.formatMonthlyListeners(reputationTrait.getMonthlyListeners()),
                                        String.join(", ", artist.getGenreList())
                                });
                            }
                        });
                    });
                    offset++;
                }
                helper.execute(PublicValues.session.api(), (exception, resp) -> {
                    ConsoleLogging.Throwable(exception);
                });
                response = UnofficialSpotifyAPI.getLibraryPage(new String[] {"Artists"}, null, limit, offset);
                libraryV3 = response.data.me.libraryV3;
            }
        }catch (IOException | TokenProvider.TokenException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    public void fill() {
        new Thread(this::fetch).start();
    }
}

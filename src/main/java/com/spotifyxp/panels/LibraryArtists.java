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

import com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotify.metadata.Metadata;
import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.spotapi.pojos.LibraryResponse;
import com.spotifyxp.spotapi.requests.collection.CollectionSet;
import com.spotifyxp.utils.AsyncUtils;
import com.spotifyxp.utils.SpotifyUtils;
import xyz.gianlu.librespot.dealer.ApiClient;
import xyz.gianlu.librespot.core.TokenProvider;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class LibraryArtists extends JScrollPane {
    private static final String CACHE_ID = "artists";

    public static DefTable artistsTable;
    public static ArrayList<String> artistsUris;
    public static ContextMenu contextMenu;

    private static class ArtistRow {
        String uri;
        String name;
        String monthlyListeners;
        String genres;

        ArtistRow(String uri, String name, String monthlyListeners, String genres) {
            this.uri = uri;
            this.name = name;
            this.monthlyListeners = monthlyListeners;
            this.genres = genres;
        }
    }

    public LibraryArtists() {
        artistsUris = new ArrayList<>();

        artistsTable = new DefTable();
        artistsTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{
                PublicValues.language.translate("general.name"),
                PublicValues.language.translate("general.monthly_listeners"),
                PublicValues.language.translate("general.genres")
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
        contextMenu.addItem(PublicValues.language.translate("general.refresh"), new Runnable() {
            @Override
            public void run() {
                artistsTable.addModifyAction(() -> ((DefaultTableModel) artistsTable.getModel()).setRowCount(0));
                artistsUris.clear();
                try {
                    PublicValues.cache.namespace("LibraryArtists").remove(CACHE_ID);
                } catch (IOException e) {
                    ConsoleLogging.Throwable(e);
                }
                new Thread(() -> fetch()).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("library.artists.context_menu.unfollow"), new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        PublicValues.spotAPI.collection().write()
                                .setSet(CollectionSet.ARTIST)
                                .removeUris(artistsUris.get(artistsTable.getSelectedRow()))
                                .execute();
                        SpotifyXPEvents.libraryChange.trigger(new LibraryChange(
                                artistsUris.get(artistsTable.getSelectedRow()),
                                LibraryChange.Type.ARTIST,
                                LibraryChange.Action.REMOVE
                        ));
                    } catch (IOException e) {
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
        if (PublicValues.cache.namespace("LibraryArtists").has(CACHE_ID)) {
            try {
                ArtistRow[] rows = PublicValues.cache.namespace("LibraryArtists").get(CACHE_ID, ArtistRow[].class);
                for (ArtistRow row : rows) {
                    artistsUris.add(row.uri);
                    artistsTable.addModifyAction(() -> ((DefaultTableModel) artistsTable.getModel()).addRow(new Object[]{row.name, row.monthlyListeners, row.genres}));
                }
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            return;
        }

        try {
            ArrayList<ArtistRow> cacheRows = new ArrayList<>();
            int limit = 50;
            LibraryResponse response = PublicValues.spotAPI.library().get().setFilters("Artists").setLimit(limit).setOffset(0).execute();
            int total = response.getTotalCount();
            int offset = 0;
            while(offset < total) {
                int nextOffset = offset + response.getItems().size();
                //Start fetching the next page's listing while this page's metadata batch is
                //still executing, instead of waiting for the batch to finish first - the two
                //were previously fully serialized even though they're independent requests.
                int finalOffset = nextOffset;
                Future<LibraryResponse> nextPageFuture = nextOffset < total
                        ? AsyncUtils.submit(() -> PublicValues.spotAPI.library().get().setFilters("Artists").setLimit(limit).setOffset(finalOffset).execute())
                        : null;

                ApiClient.BatchedRequestHelper helper = new ApiClient.BatchedRequestHelper();
                for(LibraryResponse.LibraryRow artistItem : response.getItems()) {
                    LibraryResponse.ArtistData artistItemData = artistItem.getItem().asArtist();
                    if (artistItemData == null) continue;
                    helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                    .setEntityUri(artistItemData.getUri())
                                    .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                            .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ARTIST_V4)
                                            .build())
                                    .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                            .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ON_PLATFORM_REPUTATION_TRAIT)
                                            .build())
                            .build(), data -> {
                        Metadata.Artist artist = Metadata.Artist.parseFrom(data[0].getValue());
                        ExtendedMetadata.OnPlatformReputationTrait reputationTrait = ExtendedMetadata.OnPlatformReputationTrait.parseFrom(data[1].getValue());
                        artistsUris.add(artistItemData.getUri());
                        String monthlyListeners = SpotifyUtils.formatMonthlyListeners(reputationTrait.getMonthlyListeners());
                        String genres = String.join(", ", artist.getGenreList());
                        cacheRows.add(new ArtistRow(artistItemData.getUri(), artist.getName(), monthlyListeners, genres));
                        artistsTable.addModifyAction(new Runnable() {
                            @Override
                            public void run() {
                                ((DefaultTableModel) artistsTable.getModel()).addRow(new Object[]{
                                        artist.getName(),
                                        monthlyListeners,
                                        genres
                                });
                            }
                        });
                    });
                }
                helper.execute(PublicValues.session.api(), (exception, resp) -> {
                    ConsoleLogging.Throwable(exception);
                });
                offset = nextOffset;
                if (nextPageFuture == null) break;
                response = nextPageFuture.get();
            }
            PublicValues.cache.namespace("LibraryArtists").put(CACHE_ID, cacheRows);
        }catch (IOException | TokenProvider.TokenException | InterruptedException | ExecutionException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    public void fill() {
        new Thread(this::fetch).start();
    }

    public static void evict() {
        if (artistsUris == null || artistsUris.isEmpty()) return;

        DefaultTableModel model = (DefaultTableModel) artistsTable.getModel();
        ArrayList<ArtistRow> rows = new ArrayList<>();
        for (int i = 0; i < model.getRowCount() && i < artistsUris.size(); i++) {
            rows.add(new ArtistRow(artistsUris.get(i), (String) model.getValueAt(i, 0), (String) model.getValueAt(i, 1), (String) model.getValueAt(i, 2)));
        }
        try {
            PublicValues.cache.namespace("LibraryArtists").put(CACHE_ID, rows);
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }

        artistsUris.clear();
        artistsTable.addModifyAction(() -> model.setRowCount(0));
    }
}

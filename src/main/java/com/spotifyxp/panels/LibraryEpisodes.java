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
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.com.spotify.playlist4.Playlist4ApiProto;
import com.spotifyxp.deps.xyz.gianlu.librespot.api.ApiClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.EpisodeId;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.PlaylistId;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.swingextension.JDialog;
import com.spotifyxp.utils.TrackUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class LibraryEpisodes extends JScrollPane {
    public static DefTable episodesTable;
    public static ArrayList<String> episodesUris;
    public static ContextMenu contextMenu;

    public LibraryEpisodes() {
        episodesUris = new ArrayList<>();

        episodesTable = new DefTable();
        episodesTable.setModel(new DefaultTableModel(new Object[][]{}, new Object[]{
                PublicValues.language.translate("ui.navigation.library.episodes.table.column1"),
                PublicValues.language.translate("ui.navigation.library.episodes.table.column2"),
                PublicValues.language.translate("ui.navigation.library.episodes.table.column3"),
                PublicValues.language.translate("ui.navigation.library.episodes.table.column4")
        }));
        episodesTable.setForeground(PublicValues.globalFontColor);
        episodesTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        episodesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    InstanceManager.getSpotifyPlayer().load(
                            episodesUris.get(episodesTable.getSelectedRow()),
                            true,
                            PublicValues.shuffle
                    );
                }
            }
        });

        contextMenu = new ContextMenu(episodesTable, episodesUris, getClass());
        contextMenu.addItem(PublicValues.language.translate("ui.general.refresh"), new Runnable() {
            @Override
            public void run() {
                ((DefaultTableModel) episodesTable.getModel()).setRowCount(0);
                episodesUris.clear();
                new Thread(() -> fetch()).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("ui.library.tabs.episodes.ctxmenu.remove"), new Runnable() {
            @Override
            public void run() {
                if(episodesTable.getSelectedRow() == -1) return;
                new Thread(() -> {
                    /*try {
                        InstanceManager.getSpotifyApi().removeUsersSavedEpisodes(
                                episodesUris.get(episodesTable.getSelectedRow()).split(":")[2]
                        ).build().execute();
                        Events.triggerEvent(SpotifyXPEvents.librarychange.getName(), new LibraryChange(
                                episodesUris.get(episodesTable.getSelectedRow()),
                                LibraryChange.Type.EPISODE,
                                LibraryChange.Action.REMOVE
                        ));
                    }catch (IOException e) {
                        ConsoleLogging.Throwable(e);
                    }*/
                    JOptionPane.showMessageDialog(ContentPanel.frame, "Not implemented yet");
                }).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("ui.library.tabs.episodes.ctxmenu.getdescep"), new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        Metadata.Episode episode = PublicValues.session.api().episode().getMetadata(EpisodeId.fromUri(episodesUris.get(episodesTable.getSelectedRow())));
                        openDialog(
                                String.format(PublicValues.language.translate("ui.library.tabs.episodes.epdescdialog.title"), episode.getName()),
                                episode.getDescription()
                        );
                    }catch (IOException | MercuryClient.MercuryException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("ui.library.tabs.episodes.ctxmenu.getdescshow"), new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        Metadata.Episode episode = PublicValues.session.api().episode().getMetadata(EpisodeId.fromUri(episodesUris.get(episodesTable.getSelectedRow())));
                        openDialog(
                                String.format(PublicValues.language.translate("ui.library.tabs.episodes.showdescdialog.title"), episode.getShow().getName()),
                                episode.getShow().getDescription()
                        );
                    }catch (IOException | MercuryClient.MercuryException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }).start();
            }
        });

        SpotifyXPEvents.libraryChange.subscribe((change) -> {
            if(episodesUris.isEmpty()) return;
            if(change.getType() != LibraryChange.Type.EPISODE) return;
            if(change.getAction() == LibraryChange.Action.ADD) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Metadata.Episode episode = PublicValues.session.api().episode().getMetadata(EpisodeId.fromUri(change.getUri()));
                            episodesUris.add(0, change.getUri());
                            episodesTable.addModifyAction(new Runnable() {
                                @Override
                                public void run() {
                                    ((DefaultTableModel) episodesTable.getModel()).insertRow(0, new Object[]{
                                            episode.getName(),
                                            episode.getShow().getName(),
                                            TrackUtils.calculateFileSizeKb(episode.getDuration()),
                                            TrackUtils.getHHMMSSOfTrack(episode.getDuration())
                                    });
                                }
                            });
                        }catch (IOException | MercuryClient.MercuryException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, "Library add episode").start();
            }else {
                for(int uri = 0; uri < episodesUris.size(); uri++) {
                    if(episodesUris.get(uri).equals(change.getUri())) {
                        episodesUris.remove(uri);
                        ((DefaultTableModel) episodesTable.getModel()).removeRow(uri);
                        return;
                    }
                }
            }
        });

        setViewportView(episodesTable);
    }

    private void fetch() {
        try {
            UnofficialSpotifyAPI.LibraryResponse userLibraryResponse = UnofficialSpotifyAPI.getLibraryPage(new String[] {"Playlists"}, new String[] {"YOUR_EPISODES_V2"}, 10, 0);
            String episodePlaylistUri = null;
            for(UnofficialSpotifyAPI.LibraryItemEntry item : userLibraryResponse.data.me.libraryV3.items) {
                UnofficialSpotifyAPI.PlaylistItem playlistItem = new Gson().fromJson(item.item.data.toString(), UnofficialSpotifyAPI.PlaylistItem.class);
                if(playlistItem.format != null && playlistItem.format.equals("listen-later")) {
                    episodePlaylistUri = item.item.uri;
                    break;
                }
            }

            if (episodePlaylistUri == null) {
                ConsoleLogging.warning("No episodes playlist found in user library.");
                return;
            }

            Playlist4ApiProto.SelectedListContent listContent = PublicValues.session.api().playlist().get(PlaylistId.fromUri(episodePlaylistUri));
            ApiClient.BatchedRequestHelper requestHelper = new ApiClient.BatchedRequestHelper();
            for (Playlist4ApiProto.Item episodeItem : listContent.getContents().getItemsList()) {
                requestHelper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                        .setEntityUri(episodeItem.getUri())
                        .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.EPISODE_V4)
                                .build())
                        .build(), data -> {
                    Metadata.Episode episode = Metadata.Episode.parseFrom(data[0].getValue());
                    episodesUris.add(episodeItem.getUri());
                    episodesTable.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            ((DefaultTableModel) episodesTable.getModel()).addRow(new Object[]{
                                    episode.getName(),
                                    episode.getShow().getName(),
                                    TrackUtils.calculateFileSizeKb(episode.getDuration()),
                                    TrackUtils.getHHMMSSOfTrack(episode.getDuration())
                            });
                        }
                    });
                });
            }
            requestHelper.execute(PublicValues.session.api(), (exception, response) -> ConsoleLogging.Throwable(exception));
        } catch (IOException | MercuryClient.MercuryException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    private void openDialog(
            String title,
            String text
    ) throws IOException {
        JDialog dialog = new JDialog();
        JTextArea area = new JTextArea(text);
        JScrollPane pane = new JScrollPane(area);
        area.setForeground(PublicValues.globalFontColor);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        dialog.setContentPane(pane);
        dialog.setTitle(title);
        dialog.pack();
        dialog.setVisible(true);
        Dimension dimension = dialog.getSize();
        dimension.width = PublicValues.applicationWidth / 2;
        dialog.setSize(dimension);
    }

    public void fill() {
        new Thread(() -> fetch()).start();
    }
}

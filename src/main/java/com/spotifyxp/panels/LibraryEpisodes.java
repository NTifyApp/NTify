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
import com.spotify.playlist4.Playlist4ApiProto;
import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.spotapi.pojos.LibraryResponse;
import com.spotifyxp.swingextension.JDialog;
import com.spotifyxp.utils.TrackUtils;
import xyz.gianlu.librespot.dealer.ApiClient;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.EpisodeId;
import xyz.gianlu.librespot.metadata.PlaylistId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class LibraryEpisodes extends JScrollPane {
    private static final String CACHE_ID = "episodes";

    public static DefTable episodesTable;
    public static ArrayList<String> episodesUris;
    public static ContextMenu contextMenu;

    private static class EpisodeRow {
        String uri;
        String episodeName;
        String showName;
        String filesize;
        String length;

        EpisodeRow(String uri, String episodeName, String showName, String filesize, String length) {
            this.uri = uri;
            this.episodeName = episodeName;
            this.showName = showName;
            this.filesize = filesize;
            this.length = length;
        }
    }

    public LibraryEpisodes() {
        episodesUris = new ArrayList<>();

        episodesTable = new DefTable();
        episodesTable.setModel(new DefaultTableModel(new Object[][]{}, new Object[]{
                PublicValues.language.translate("library.episodes.table.episode_name"),
                PublicValues.language.translate("library.general.show_name"),
                PublicValues.language.translate("general.filesize"),
                PublicValues.language.translate("general.length")
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
        contextMenu.addItem(PublicValues.language.translate("general.refresh"), new Runnable() {
            @Override
            public void run() {
                episodesTable.addModifyAction(() -> ((DefaultTableModel) episodesTable.getModel()).setRowCount(0));
                episodesUris.clear();
                try {
                    PublicValues.cache.namespace("LibraryEpisodes").remove(CACHE_ID);
                } catch (IOException e) {
                    ConsoleLogging.Throwable(e);
                }
                new Thread(() -> fetch()).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("library.episodes.context_menu.remove"), new Runnable() {
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
        contextMenu.addItem(PublicValues.language.translate("library.episodes.context_menu.view_description"), new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        Metadata.Episode episode = PublicValues.session.api().getMetadata4Episode(EpisodeId.fromUri(episodesUris.get(episodesTable.getSelectedRow())));
                        openDialog(
                                String.format(PublicValues.language.translate("dialogs.library.episode_description.title"), episode.getName()),
                                episode.getDescription()
                        );
                    }catch (IOException | TokenProvider.TokenException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("library.episodes.context_menu.view_show_description"), new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        Metadata.Episode episode = PublicValues.session.api().getMetadata4Episode(EpisodeId.fromUri(episodesUris.get(episodesTable.getSelectedRow())));
                        openDialog(
                                String.format(PublicValues.language.translate("dialogs.library.show_description.title"), episode.getShow().getName()),
                                episode.getShow().getDescription()
                        );
                    }catch (IOException | TokenProvider.TokenException e) {
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
                            Metadata.Episode episode = PublicValues.session.api().getMetadata4Episode(EpisodeId.fromUri(change.getUri()));
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
                        }catch (IOException | TokenProvider.TokenException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, "Library add episode").start();
            }else {
                for(int uri = 0; uri < episodesUris.size(); uri++) {
                    if(episodesUris.get(uri).equals(change.getUri())) {
                        episodesUris.remove(uri);
                        int removeIndex = uri;
                        episodesTable.addModifyAction(() -> ((DefaultTableModel) episodesTable.getModel()).removeRow(removeIndex));
                        return;
                    }
                }
            }
        });

        setViewportView(episodesTable);
    }

    private void fetch() {
        if (PublicValues.cache.namespace("LibraryEpisodes").has(CACHE_ID)) {
            try {
                EpisodeRow[] rows = PublicValues.cache.namespace("LibraryEpisodes").get(CACHE_ID, EpisodeRow[].class);
                for (EpisodeRow row : rows) {
                    episodesUris.add(row.uri);
                    episodesTable.addModifyAction(() -> ((DefaultTableModel) episodesTable.getModel()).addRow(new Object[]{row.episodeName, row.showName, row.filesize, row.length}));
                }
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            return;
        }

        try {
            ArrayList<EpisodeRow> cacheRows = new ArrayList<>();
            LibraryResponse userLibraryResponse = PublicValues.spotAPI.library().get().setFilters("Playlists").setFeatures("YOUR_EPISODES_V2").setLimit(10).setOffset(0).execute();
            String episodePlaylistUri = null;
            for(LibraryResponse.LibraryRow item : userLibraryResponse.getItems()) {
                LibraryResponse.PlaylistData playlistItem = item.getItem().asPlaylist();
                if(playlistItem != null && "listen-later".equals(playlistItem.getFormat())) {
                    episodePlaylistUri = item.getItem().getUri();
                    break;
                }
            }

            if (episodePlaylistUri == null) {
                ConsoleLogging.warning("No episodes playlist found in user library.");
                return;
            }

            Playlist4ApiProto.SelectedListContent listContent = PublicValues.session.api().getPlaylist(PlaylistId.fromUri(episodePlaylistUri));
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
                    String filesize = TrackUtils.calculateFileSizeKb(episode.getDuration());
                    String length = TrackUtils.getHHMMSSOfTrack(episode.getDuration());
                    cacheRows.add(new EpisodeRow(episodeItem.getUri(), episode.getName(), episode.getShow().getName(), filesize, length));
                    episodesTable.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            ((DefaultTableModel) episodesTable.getModel()).addRow(new Object[]{
                                    episode.getName(),
                                    episode.getShow().getName(),
                                    filesize,
                                    length
                            });
                        }
                    });
                });
            }
            requestHelper.execute(PublicValues.session.api(), (exception, response) -> ConsoleLogging.Throwable(exception));
            PublicValues.cache.namespace("LibraryEpisodes").put(CACHE_ID, cacheRows);
        } catch (IOException | TokenProvider.TokenException e) {
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

    public static void evict() {
        if (episodesUris == null || episodesUris.isEmpty()) return;

        DefaultTableModel model = (DefaultTableModel) episodesTable.getModel();
        ArrayList<EpisodeRow> rows = new ArrayList<>();
        for (int i = 0; i < model.getRowCount() && i < episodesUris.size(); i++) {
            rows.add(new EpisodeRow(episodesUris.get(i), (String) model.getValueAt(i, 0), (String) model.getValueAt(i, 1), (String) model.getValueAt(i, 2), (String) model.getValueAt(i, 3)));
        }
        try {
            PublicValues.cache.namespace("LibraryEpisodes").put(CACHE_ID, rows);
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }

        episodesUris.clear();
        episodesTable.addModifyAction(() -> model.setRowCount(0));
    }
}

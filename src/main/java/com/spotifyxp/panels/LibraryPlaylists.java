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
import com.spotify.playlist4.Playlist4ApiProto;
import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.dialogs.AddPlaylistDialog;
import com.spotifyxp.dialogs.ChangePlaylistDialog;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.AsyncUtils;
import com.spotifyxp.utils.ReentryGuard;
import com.spotifyxp.utils.TrackUtils;
import xyz.gianlu.librespot.api.ApiClient;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.EpisodeId;
import xyz.gianlu.librespot.metadata.PlaylistId;
import xyz.gianlu.librespot.metadata.TrackId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

public class LibraryPlaylists extends JSplitPane {
    private static final String CACHE_ID = "playlists";

    private static class PlaylistRow {
        String uri;
        String name;

        PlaylistRow(String uri, String name) {
            this.uri = uri;
            this.name = name;
        }
    }

    private static final ReentryGuard songsLoadGuard = new ReentryGuard();

    public static JScrollPane playlistsPlaylistsScrollPane;
    public static JScrollPane playlistsSongsScrollPane;
    public static DefTable playlistsPlaylistsTable;
    public static DefTable playlistsSongTable;
    public static final ArrayList<String> playlistsUriCache = new ArrayList<>();
    public static final ArrayList<String> playlistsSongUriCache = new ArrayList<>();
    public static ContextMenu playlistsSongTableContextMenu;
    public static ContextMenu playlistsPlaylistsTableContextMenu;
    public static JTextPane playlistDescription;
    public static JScrollPane playlistDescriptionScrollPane;
    public static JPanel playlistsSongsPanel;
    private Runnable lazyLoadingDeInit;


    private void invalidatePlaylistsCache() {
        try {
            PublicValues.cache.namespace("LibraryPlaylists").remove(CACHE_ID);
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    private void fetchPlaylists() {
        playlistsPlaylistsTable.addModifyAction(() -> ((DefaultTableModel) playlistsPlaylistsTable.getModel()).setRowCount(0));
        playlistsUriCache.clear();

        if (PublicValues.cache.namespace("LibraryPlaylists").has(CACHE_ID)) {
            try {
                PlaylistRow[] rows = PublicValues.cache.namespace("LibraryPlaylists").get(CACHE_ID, PlaylistRow[].class);
                for (PlaylistRow row : rows) {
                    playlistsUriCache.add(row.uri);
                    playlistsPlaylistsTable.addModifyAction(() -> ((DefaultTableModel) playlistsPlaylistsTable.getModel()).addRow(new Object[]{row.name}));
                }
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            return;
        }

        try {
            ArrayList<PlaylistRow> cacheRows = new ArrayList<>();
            UnofficialSpotifyAPI.LibraryResponse response = UnofficialSpotifyAPI.getLibraryPage(new String[] {"Playlists"}, null, 999999, 0);

            Gson gson = new Gson();
            for (UnofficialSpotifyAPI.LibraryItemEntry item : response.data.me.libraryV3.items) {
                UnofficialSpotifyAPI.PlaylistItem playlistItem = gson.fromJson(gson.toJson(item.item.data), UnofficialSpotifyAPI.PlaylistItem.class);

                playlistsUriCache.add(playlistItem.uri);
                cacheRows.add(new PlaylistRow(playlistItem.uri, playlistItem.name));
                playlistsPlaylistsTable.addModifyAction(() -> ((DefaultTableModel) playlistsPlaylistsTable.getModel()).addRow(new Object[]{playlistItem.name}));
            }
            PublicValues.cache.namespace("LibraryPlaylists").put(CACHE_ID, cacheRows);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public LibraryPlaylists() {
        setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        setVisible(false);

        playlistsSongsPanel = new JPanel();
        playlistsSongsPanel.setLayout(new BorderLayout());

        playlistDescriptionScrollPane = new JScrollPane();
        playlistDescriptionScrollPane.setPreferredSize(new Dimension(-1, 40));
        playlistDescriptionScrollPane.setVisible(false);

        playlistDescription = new JTextPane();
        playlistDescription.setEditable(false);
        playlistDescription.setContentType("text/html");
        ((AbstractDocument) playlistDescription.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                string = string.replaceAll("\n", "");
                super.insertString(fb, offset, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                text = text.replaceAll("\n", "");
                super.replace(fb, offset, length, text, attrs);
            }
        });

        playlistDescriptionScrollPane.setViewportView(playlistDescription);
        playlistsSongsPanel.add(playlistDescriptionScrollPane, BorderLayout.NORTH);


        playlistsPlaylistsTable = new DefTable();
        playlistsPlaylistsTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("general.name")}));
        playlistsPlaylistsTable.setForeground(PublicValues.globalFontColor);
        playlistsPlaylistsTable.getColumnModel().getColumn(0).setPreferredWidth(623);
        playlistsPlaylistsTable.setFillsViewportHeight(true);
        playlistsPlaylistsTable.setColumnSelectionAllowed(true);
        playlistsPlaylistsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        playlistsPlaylistsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (!songsLoadGuard.tryEnter()) return;
                    if(lazyLoadingDeInit != null) {
                        lazyLoadingDeInit.run();
                        lazyLoadingDeInit = null;
                    }
                    AsyncUtils.run(() -> {
                      try {
                        playlistsSongUriCache.clear();
                        playlistsSongTable.addModifyAction(() -> ((DefaultTableModel) playlistsSongTable.getModel()).setRowCount(0));
                        try {
                            Playlist4ApiProto.SelectedListContent listContent = PublicValues.session.api().playlist().get(PlaylistId.fromUri(playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow())));
                            ApiClient.BatchedRequestHelper helper = new ApiClient.BatchedRequestHelper();
                            for (Playlist4ApiProto.Item item : listContent.getContents().getItemsList()) {
                                switch (item.getUri().split(":")[1]) {
                                    case "track": {
                                        helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                                .setEntityUri(item.getUri())
                                                .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                                        .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.TRACK_V4)
                                                        .build())
                                                .build(), data -> {
                                            Metadata.Track track = Metadata.Track.parseFrom(data[0].getValue());
                                            playlistsSongTable.addModifyAction(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((DefaultTableModel) playlistsSongTable.getModel()).addRow(new Object[] {track.getName(), TrackUtils.calculateFileSizeKb(track.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDuration())});
                                                    playlistsSongUriCache.add(TrackId.fromHex(Utils.bytesToHex(track.getGid())).toSpotifyUri());
                                                }
                                            });
                                        });
                                        break;
                                    }
                                    case "episode": {
                                        helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                                .setEntityUri(item.getUri())
                                                .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                                        .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.EPISODE_V4)
                                                        .build())
                                                .build(), data -> {
                                            Metadata.Episode episode = Metadata.Episode.parseFrom(data[0].getValue());
                                            playlistsSongTable.addModifyAction(new Runnable() {
                                                @Override
                                                public void run() {
                                                    ((DefaultTableModel) playlistsSongTable.getModel()).addRow(new Object[] {episode.getName(), TrackUtils.calculateFileSizeKb(episode.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(episode.getDuration())});
                                                    playlistsSongUriCache.add(EpisodeId.fromHex(Utils.bytesToHex(episode.getGid())).toSpotifyUri());
                                                }
                                            });
                                        });
                                        break;
                                    }
                                    default:
                                        ConsoleLogging.warning("Unsupported playlist item type: " + item.getUri());
                                        break;
                                }
                            }
                            helper.execute(PublicValues.session.api(), ((exception, response) -> ConsoleLogging.Throwable(exception)));
                        } catch (Exception e1) {
                            throw new RuntimeException(e1);
                        }
                      } finally {
                          songsLoadGuard.exit();
                      }
                    });
                }
            }
        });

        playlistsPlaylistsScrollPane = new JScrollPane();
        playlistsPlaylistsScrollPane.setPreferredSize(new Dimension(259, getHeight()));
        setLeftComponent(playlistsPlaylistsScrollPane);
        playlistsPlaylistsScrollPane.setViewportView(playlistsPlaylistsTable);

        playlistsSongTable = new DefTable();
        playlistsSongTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        playlistsSongTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("general.name"), PublicValues.language.translate("general.filesize"), PublicValues.language.translate("general.bitrate"), PublicValues.language.translate("general.length")}));
        playlistsSongTable.setForeground(PublicValues.globalFontColor);
        playlistsSongTable.getColumnModel().getColumn(0).setPreferredWidth(363);
        playlistsSongTable.getColumnModel().getColumn(1).setPreferredWidth(89);
        playlistsSongTable.getColumnModel().getColumn(3).setPreferredWidth(96);
        playlistsSongTable.setFillsViewportHeight(true);
        playlistsSongTable.setColumnSelectionAllowed(true);
        playlistsSongTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    AsyncUtils.run(() -> {
                        InstanceManager.getPlayer().getPlayer().load(playlistsSongUriCache.get(playlistsSongTable.getSelectedRow()), true, PublicValues.shuffle);
                        TrackUtils.addAllToQueue(playlistsSongUriCache, playlistsSongTable);
                    });
                }
            }
        });

        playlistsSongsScrollPane = new JScrollPane();
        setRightComponent(playlistsSongsPanel);
        playlistsSongsScrollPane.setViewportView(playlistsSongTable);

        playlistsSongsPanel.add(playlistsSongsScrollPane, BorderLayout.CENTER);

        playlistsSongTableContextMenu = new ContextMenu(playlistsSongTable, playlistsSongUriCache, getClass());
        playlistsSongTableContextMenu.addItem(PublicValues.language.translate("general.refresh"), new Runnable() {
            @Override
            public void run() {
                playlistsSongTable.addModifyAction(() -> ((DefaultTableModel) playlistsSongTable.getModel()).setRowCount(0));
                playlistsSongUriCache.clear();
                fill();
            }
        });

        playlistsPlaylistsTableContextMenu = new ContextMenu(playlistsPlaylistsTable, playlistsUriCache, getClass());
        playlistsPlaylistsTableContextMenu.addItem(PublicValues.language.translate("general.remove_playlist"), () -> {
            try {
                PublicValues.session.api().playlist().remove(PublicValues.session.username(), new String[] {playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow())});

                SpotifyXPEvents.libraryChange.trigger(new LibraryChange(
                        playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()),
                        LibraryChange.Type.PLAYLIST,
                        LibraryChange.Action.REMOVE
                ));
            } catch (IOException | TokenProvider.TokenException e) {
                throw new RuntimeException(e);
            }
        });
        playlistsPlaylistsTableContextMenu.addItem(PublicValues.language.translate("general.refresh"), () -> {
            invalidatePlaylistsCache();
            new Thread(this::fetchPlaylists, "Fetch playlists").start();
        });
        playlistsPlaylistsTableContextMenu.addItem(PublicValues.language.translate("library.playlists.context_menu.change_playlist"), () -> {
            if(playlistsPlaylistsTable.getSelectedRow() == -1) {
                JOptionPane.showMessageDialog(ContentPanel.frame, PublicValues.language.translate("dialogs.change_playlist.dialogs.no_playlist_selected.message"), PublicValues.language.translate("general.error"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            //ToDo: Implement image change functionality
            try {
                Playlist4ApiProto.SelectedListContent playlistRec = PublicValues.session.api().playlist().get(PlaylistId.fromUri(playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow())));
                ChangePlaylistDialog dialog = new ChangePlaylistDialog();
                dialog.show(
                        playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()).split(":")[2],
                        playlistRec
                        , new ChangePlaylistDialog.ChangedPlaylistRunnable() {
                            @Override
                            public void receive(ChangePlaylistDialog.ChangedPlaylist playlist) {
                                new Thread(() -> {
                                    try {
                                        PublicValues.session.api().playlist().edit(
                                                playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()),
                                                playlist.playlistName,
                                                playlist.playlistDescription,
                                                playlist.isPublic ? 1 : 0,
                                                playlist.isCollaborative ? 1 : 0,
                                                new byte[0]
                                        );
                                        invalidatePlaylistsCache();
                                        new Thread(LibraryPlaylists.this::fetchPlaylists, "Fetch playlists").start();
                                    } catch (IOException | TokenProvider.TokenException e) {
                                        ConsoleLogging.Throwable(e);
                                    }
                                }, "Change playlist").start();
                            }
                        });
            } catch (IOException | TokenProvider.TokenException e) {
                ConsoleLogging.Throwable(e);
            }
        });
        playlistsPlaylistsTableContextMenu.addItem(PublicValues.language.translate("library.playlists.context_menu.create_playlist"), () -> {
            try {
                AddPlaylistDialog dialog = new AddPlaylistDialog();
                dialog.show((data) -> {
                    new Thread(() -> {
                        try {
                            PublicValues.session.api().playlist().create(
                                    data.name,
                                    data.description,
                                    data.isPublic,
                                    data.isCollaborative,
                                    data.imageData
                            );
                            invalidatePlaylistsCache();
                            new Thread(this::fetchPlaylists, "Fetch playlists").start();
                        } catch (IOException | TokenProvider.TokenException | TimeoutException e) {
                            throw new RuntimeException(e);
                        }
                    }, "Create playlist thread").start();
                }, () -> {
                }, dialog::dispose);
            }catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
        });

        SpotifyXPEvents.libraryChange.subscribe((change) -> {
            if(playlistsUriCache.isEmpty()) return;
            if(change.getType() != LibraryChange.Type.PLAYLIST) return;
            if(change.getAction() == LibraryChange.Action.ADD) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Playlist4ApiProto.SelectedListContent playlist = PublicValues.session.api().playlist().get(PlaylistId.fromUri(change.getUri()));
                            playlistsUriCache.add(0, change.getUri());
                            playlistsPlaylistsTable.addModifyAction(() -> ((DefaultTableModel) playlistsPlaylistsTable.getModel()).insertRow(0, new Object[]{playlist.getAttributes().getName()}));
                        } catch (IOException | TokenProvider.TokenException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, "Library add playlist").start();
            }else {
                int removeIndex = playlistsUriCache.indexOf(change.getUri());
                playlistsPlaylistsTable.addModifyAction(() -> ((DefaultTableModel) playlistsPlaylistsTable.getModel()).removeRow(removeIndex));
                playlistsUriCache.remove(change.getUri());
            }
        });
    }

    public void fill() {
        new Thread(this::fetchPlaylists, "Fetch playlists").start();
    }

    public static void evict() {
        if (playlistsUriCache.isEmpty()) return;

        DefaultTableModel model = (DefaultTableModel) playlistsPlaylistsTable.getModel();
        ArrayList<PlaylistRow> rows = new ArrayList<>();
        for (int i = 0; i < model.getRowCount() && i < playlistsUriCache.size(); i++) {
            rows.add(new PlaylistRow(playlistsUriCache.get(i), (String) model.getValueAt(i, 0)));
        }
        try {
            PublicValues.cache.namespace("LibraryPlaylists").put(CACHE_ID, rows);
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }

        playlistsUriCache.clear();
        playlistsPlaylistsTable.addModifyAction(() -> model.setRowCount(0));

        playlistsSongUriCache.clear();
        playlistsSongTable.addModifyAction(() -> ((DefaultTableModel) playlistsSongTable.getModel()).setRowCount(0));
    }
}

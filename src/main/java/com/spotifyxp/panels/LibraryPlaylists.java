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
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Paging;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Playlist;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import com.spotifyxp.dialogs.AddPlaylistDialog;
import com.spotifyxp.dialogs.ChangePlaylistDialog;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.AsyncMouseListener;
import com.spotifyxp.utils.TrackUtils;

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
import java.util.Objects;

public class LibraryPlaylists extends JSplitPane {
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
    private final boolean[] inProg = {false};
    private boolean loadNew = false;
    private Runnable lazyLoadingDeInit;


    private void fetchPlaylists() {
        ((DefaultTableModel) playlistsPlaylistsTable.getModel()).setRowCount(0);
        playlistsUriCache.clear();
        try {
            int total = InstanceManager.getSpotifyApi().getListOfCurrentUsersPlaylists().build().execute().getTotal();
            int parsed = 0;
            int counter = 0;
            int last = 0;
            int offset = 0;
            while (parsed != total) {
                PlaylistSimplified[] playlists = InstanceManager.getSpotifyApi().getListOfCurrentUsersPlaylists().offset(offset).limit(50).build().execute().getItems();
                for (PlaylistSimplified simplified : playlists) {
                    playlistsUriCache.add(simplified.getUri());
                    ((DefaultTableModel) playlistsPlaylistsTable.getModel()).addRow(new Object[]{simplified.getName()});
                    parsed++;
                }
                if (parsed == last) {
                    if (counter > 1) {
                        break;
                    }
                    counter++;
                } else {
                    counter = 0;
                }
                last = parsed;
                offset += 50;
            }
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
        playlistsPlaylistsTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("ui.playlists.playlists.playlistname")}));
        playlistsPlaylistsTable.setForeground(PublicValues.globalFontColor);
        playlistsPlaylistsTable.getColumnModel().getColumn(0).setPreferredWidth(623);
        playlistsPlaylistsTable.setFillsViewportHeight(true);
        playlistsPlaylistsTable.setColumnSelectionAllowed(true);
        playlistsPlaylistsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        playlistsPlaylistsTable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if(lazyLoadingDeInit != null) {
                        lazyLoadingDeInit.run();
                        lazyLoadingDeInit = null;
                    }
                    if(PublicValues.config.getBoolean(ConfigValues.load_all_tracks.name)) {
                        Thread thread = new Thread(() -> {
                            playlistsSongUriCache.clear();
                            ((DefaultTableModel) playlistsSongTable.getModel()).setRowCount(0);
                            try {
                                int offset = 0;
                                int parsed = 0;
                                int counter = 0;
                                int last = 0;
                                Playlist playlist = InstanceManager.getSpotifyApi().getPlaylist(playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()).split(":")[2]).build().execute();
                                int total = playlist.getTracks().getTotal();
                                playlistDescription.setText(playlist.getDescription());
                                playlistDescriptionScrollPane.setVisible(!playlist.getDescription().isEmpty());
                                playlistsSongsPanel.revalidate();
                                playlistsSongsPanel.repaint();
                                while (parsed != total) {
                                    Paging<PlaylistTrack> ptracks = InstanceManager.getSpotifyApi().getPlaylistsItems(playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()).split(":")[2]).offset(offset).limit(100).build().execute();
                                    for (PlaylistTrack track : ptracks.getItems()) {
                                        ((DefaultTableModel) playlistsSongTable.getModel()).addRow(new Object[] {track.getTrack().getName(), TrackUtils.calculateFileSizeKb(track.getTrack()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getTrack().getDurationMs())});
                                        playlistsSongUriCache.add(track.getTrack().getUri());
                                        parsed++;
                                    }
                                    if (last == parsed) {
                                        if (counter > 1) {
                                            break;
                                        }
                                        counter++;
                                    } else {
                                        counter = 0;
                                    }
                                    last = parsed;
                                    offset += 100;
                                }
                            } catch (Exception e1) {
                                throw new RuntimeException(e1);
                            }
                        }, "Get playlist tracks");
                        thread.start();
                    }else {
                        try {
                            Playlist playlist = InstanceManager.getSpotifyApi().getPlaylist(playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()).split(":")[2]).build().execute();
                            playlistDescription.setText(playlist.getDescription());
                            playlistDescriptionScrollPane.setVisible(!playlist.getDescription().isEmpty());
                            playlistsSongsPanel.revalidate();
                            playlistsSongsPanel.repaint();
                        } catch (IOException ex) {
                            ConsoleLogging.Throwable(ex);
                        }
                        loadNew = true;
                        lazyLoadingDeInit = TrackUtils.initializeLazyLoadingForPlaylists(
                                playlistsSongsScrollPane,
                                playlistsSongUriCache,
                                playlistsSongTable,
                                new int[] {28},
                                playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()).split(":")[2],
                                inProg,
                                loadNew
                        );
                        loadNew = false;
                    }
                }
            }
        }));

        playlistsPlaylistsScrollPane = new JScrollPane();
        playlistsPlaylistsScrollPane.setPreferredSize(new Dimension(259, getHeight()));
        setLeftComponent(playlistsPlaylistsScrollPane);
        playlistsPlaylistsScrollPane.setViewportView(playlistsPlaylistsTable);

        playlistsSongTable = new DefTable();
        playlistsSongTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        playlistsSongTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("ui.playlists.songslist.songtitle"), PublicValues.language.translate("ui.playlists.songslist.filesize"), PublicValues.language.translate("ui.playlists.songslist.bitrate"), PublicValues.language.translate("ui.playlists.songslist.length")}));
        playlistsSongTable.setForeground(PublicValues.globalFontColor);
        playlistsSongTable.getColumnModel().getColumn(0).setPreferredWidth(363);
        playlistsSongTable.getColumnModel().getColumn(1).setPreferredWidth(89);
        playlistsSongTable.getColumnModel().getColumn(3).setPreferredWidth(96);
        playlistsSongTable.setFillsViewportHeight(true);
        playlistsSongTable.setColumnSelectionAllowed(true);
        playlistsSongTable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    InstanceManager.getPlayer().getPlayer().load(playlistsSongUriCache.get(playlistsSongTable.getSelectedRow()), true, PublicValues.shuffle);
                    TrackUtils.addAllToQueue(playlistsSongUriCache, playlistsSongTable);
                }
            }
        }));

        playlistsSongsScrollPane = new JScrollPane();
        setRightComponent(playlistsSongsPanel);
        playlistsSongsScrollPane.setViewportView(playlistsSongTable);

        playlistsSongsPanel.add(playlistsSongsScrollPane, BorderLayout.CENTER);

        playlistsSongTableContextMenu = new ContextMenu(playlistsSongTable, playlistsSongUriCache, getClass());
        playlistsSongTableContextMenu.addItem(PublicValues.language.translate("ui.general.refresh"), new Runnable() {
            @Override
            public void run() {
                ((DefaultTableModel) playlistsSongTable.getModel()).setRowCount(0);
                playlistsSongUriCache.clear();
                fill();
            }
        });

        playlistsPlaylistsTableContextMenu = new ContextMenu(playlistsPlaylistsTable, playlistsUriCache, getClass());
        playlistsPlaylistsTableContextMenu.addItem(PublicValues.language.translate("ui.general.remove.playlist"), () -> {
            InstanceManager.getSpotifyApi().unfollowPlaylist(playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()).split(":")[2]);
            Events.triggerEvent(SpotifyXPEvents.librarychange.getName(), new LibraryChange(
                    playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()),
                    LibraryChange.Type.PLAYLIST,
                    LibraryChange.Action.REMOVE
            ));
        });
        playlistsPlaylistsTableContextMenu.addItem(PublicValues.language.translate("ui.general.refresh"), () -> {
            new Thread(this::fetchPlaylists, "Fetch playlists").start();
        });
        playlistsPlaylistsTableContextMenu.addItem(PublicValues.language.translate("changeplaylist.title"), () -> {
            if(playlistsPlaylistsTable.getSelectedRow() == -1) {
                JOptionPane.showMessageDialog(ContentPanel.frame, PublicValues.language.translate("changeplaylist.dialog.noselected.description"), PublicValues.language.translate("changeplaylist.dialog.noselected.title"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Playlist playlistRec = InstanceManager.getSpotifyApi().getPlaylist(playlistsUriCache.get(playlistsPlaylistsTable.getSelectedRow()).split(":")[2]).build().execute();
                ChangePlaylistDialog dialog = new ChangePlaylistDialog();
                dialog.show(
                        playlistRec
                        , new ChangePlaylistDialog.ChangedPlaylistRunnable() {
                            @Override
                            public void receive(ChangePlaylistDialog.ChangedPlaylist playlist) {
                                new Thread(() -> {
                                    try {
                                        InstanceManager.getSpotifyApi().changePlaylistsDetails(
                                                        playlistRec.getId()
                                                )
                                                .collaborative(playlist.isCollaborative)
                                                .public_(playlist.isPublic)
                                                .name(playlist.playlistName)
                                                .description(playlist.playlistDescription)
                                                .build().execute();
                                        new Thread(LibraryPlaylists.this::fetchPlaylists, "Fetch playlists").start();
                                    } catch (IOException e) {
                                        ConsoleLogging.Throwable(e);
                                    }
                                }, "Change playlist").start();
                            }
                        });
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
        });
        playlistsPlaylistsTableContextMenu.addItem(PublicValues.language.translate("addplaylist.title"), () -> {
            try {
                AddPlaylistDialog dialog = new AddPlaylistDialog();
                dialog.show((data) -> {
                    new Thread(() -> {
                        try {
                            String uri = InstanceManager.getSpotifyApi().createPlaylist(
                                    PublicValues.session.username(),
                                    data.name
                            ).public_(data.isPublic).description(data.description).collaborative(data.isCollaborative).build().execute().getUri();
                            if(!data.imageBase64.isEmpty()) {
                                try {
                                    InstanceManager.getSpotifyApi().uploadCustomPlaylistCoverImage(uri.split(":")[2])
                                            .image_data(data.imageBase64)
                                            .build().execute();
                                }catch(IOException e) {
                                    ConsoleLogging.Throwable(e);
                                }
                            }
                            new Thread(this::fetchPlaylists, "Fetch playlists").start();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }, "Create playlist thread").start();
                }, () -> {
                }, dialog::dispose);
            }catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
        });

        Events.subscribe(SpotifyXPEvents.librarychange.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                LibraryChange change = (LibraryChange) data[0];
                if(playlistsUriCache.isEmpty()) return;
                if(change.getType() != LibraryChange.Type.PLAYLIST) return;
                if(change.getAction() == LibraryChange.Action.ADD) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Playlist playlist = InstanceManager.getSpotifyApi().getPlaylist(change.getUri().split(":")[2]).build().execute();
                                playlistsUriCache.add(0, playlist.getUri());
                                ((DefaultTableModel) playlistsPlaylistsTable.getModel()).insertRow(0, new Object[]{playlist.getName()});
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }, "Library add playlist").start();
                }else {
                    for(int uri = 0; uri < playlistsUriCache.size(); uri++) {
                        if(playlistsUriCache.get(uri).equals(change.getUri())) {
                            playlistsUriCache.remove(uri);
                            ((DefaultTableModel) playlistsPlaylistsTable.getModel()).removeRow(uri);
                            return;
                        }
                    }
                }
            }
        });
    }

    public void fill() {
        new Thread(this::fetchPlaylists, "Fetch playlists").start();
    }
}

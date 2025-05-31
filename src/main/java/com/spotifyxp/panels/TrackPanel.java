/*
 * Copyright [2024-2025] [Gianluca Beil]
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
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.EpisodeSimplified;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Paging;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
This class holds a panel that shows a table that contains a list of tracks
 **/
public class TrackPanel extends Panel implements View {
    public static DefTable advancedSongTable;
    public static JScrollPane advancedScrollPanel;
    public static JButton advancedBackButton;
    public static final ArrayList<String> advancedUriCache = new ArrayList<>();
    private static Runnable lazyLoadingDeInit;
    public static String advancedSongPanelUri;
    private boolean blockDefaultBackAction = false;
    public static ContextMenu contextMenu;
    public static JPanel backButtonContainer;

    public TrackPanel() {
        setLayout(new BorderLayout());
        setVisible(false);
        backButtonContainer = new JPanel();
        backButtonContainer.setLayout(new BorderLayout());
        advancedBackButton = new JButton(PublicValues.language.translate("ui.back"));
        backButtonContainer.add(advancedBackButton, BorderLayout.WEST);
        advancedBackButton.setForeground(PublicValues.globalFontColor);
        add(backButtonContainer, BorderLayout.NORTH);
        advancedBackButton.addActionListener(new AsyncActionListener(e -> {
            if(lazyLoadingDeInit != null) {
                lazyLoadingDeInit.run();
                lazyLoadingDeInit = null;
            }
            if(blockDefaultBackAction) {
                blockDefaultBackAction = false;
                return;
            }
            ContentPanel.switchView(ContentPanel.lastView);
            ContentPanel.enableTabSwitch();
        }));
        advancedSongTable = new DefTable();
        advancedSongTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("ui.search.songlist.songname"), PublicValues.language.translate("ui.search.songlist.filesize"), PublicValues.language.translate("ui.search.songlist.bitrate"), PublicValues.language.translate("ui.search.songlist.length")}));
        advancedSongTable.setForeground(PublicValues.globalFontColor);
        advancedSongTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        contextMenu = new ContextMenu(advancedSongTable, advancedUriCache, getClass());
        advancedScrollPanel = new JScrollPane();
        advancedScrollPanel.setBounds(0, 22, 784, 399);
        add(advancedScrollPanel, BorderLayout.CENTER);
        advancedScrollPanel.setViewportView(advancedSongTable);
        advancedSongTable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    InstanceManager.getPlayer().getPlayer().load(advancedUriCache.get(advancedSongTable.getSelectedRow()), true, PublicValues.shuffle);
                    advancedSongTable.setColumnSelectionInterval(0, advancedSongTable.getColumnCount() - 1);
                    TrackUtils.addAllToQueue(advancedUriCache, advancedSongTable);
                }
            }
        }));
    }

    ActionListener customListener;
    public void open(String forUri, HomePanel.ContentTypes contentType, Runnable onBack) {
        blockDefaultBackAction = true;
        customListener = e -> {
            onBack.run();
            advancedBackButton.removeActionListener(customListener);
        };
        open(forUri, contentType);
        advancedBackButton.addActionListener(customListener);
    }

    private static final boolean[] inProg = {false};

    public void open(String forUri, HomePanel.ContentTypes contentType) {
        ContentPanel.switchView(Views.TRACKPANEL);
        advancedSongPanelUri = forUri;
        ((DefaultTableModel) advancedSongTable.getModel()).setRowCount(0);
        advancedUriCache.clear();
        try {
            switch (contentType) {
                case playlist:
                    if(PublicValues.config.getBoolean(ConfigValues.load_all_tracks.name)) {
                        Thread thread = new Thread(() -> {
                            advancedUriCache.clear();
                            ((DefaultTableModel)  advancedSongTable.getModel()).setRowCount(0);
                            try {
                                int offset = 0;
                                int parsed = 0;
                                int counter = 0;
                                int last = 0;
                                int total = InstanceManager.getSpotifyApi().getPlaylist(forUri.split(":")[2]).build().execute().getTracks().getTotal();
                                while (parsed != total) {
                                    Paging<PlaylistTrack> ptracks = InstanceManager.getSpotifyApi().getPlaylistsItems(forUri.split(":")[2]).offset(offset).limit(100).build().execute();
                                    for (PlaylistTrack track : ptracks.getItems()) {
                                        ((DefaultTableModel)  advancedSongTable.getModel()).addRow(new Object[]{track.getTrack().getName(), TrackUtils.calculateFileSizeKb(track.getTrack()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getTrack().getDurationMs())});
                                        advancedUriCache.add(track.getTrack().getUri());
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
                            } catch (NullPointerException e) {
                                ConsoleLogging.warning("Weird nullpointer in TrackPanel");
                            } catch (Exception e1) {
                                throw new RuntimeException(e1);
                            }
                        }, "Get playlist tracks");
                        thread.start();
                    }else {
                        boolean loadNew = true;
                        lazyLoadingDeInit = TrackUtils.initializeLazyLoadingForPlaylists(
                                advancedScrollPanel,
                                advancedUriCache,
                                advancedSongTable,
                                new int[] {28},
                                forUri.split(":")[2],
                                inProg,
                                loadNew
                        );
                    }
                    break;
                case show:
                    for (EpisodeSimplified simplified : SpotifyUtils.getAllEpisodesShow(forUri)) {
                        ((DefaultTableModel) advancedSongTable.getModel()).addRow(new Object[]{simplified.getName(), TrackUtils.calculateFileSizeKb(simplified.getDurationMs()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(simplified.getDurationMs())});
                        advancedUriCache.add(simplified.getUri());
                    }
                    break;
                case album:
                    for (TrackSimplified simplified : SpotifyUtils.getAllTracksAlbum(forUri)) {
                        ((DefaultTableModel) advancedSongTable.getModel()).addRow(new Object[]{simplified.getName(), TrackUtils.calculateFileSizeKb(simplified.getDurationMs()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(simplified.getDurationMs())});
                        advancedUriCache.add(simplified.getUri());
                    }
                    break;
                default:
                    GraphicalMessage.bug("tried to invoke showAdvancedSongPanel with incompatible type -> " + contentType);
                    break;
            }
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
        }
        ContentPanel.blockTabSwitch();
        ContentPanel.frame.revalidate();
        ContentPanel.frame.repaint();
    }

    @Override
    public void makeVisible() {
        setVisible(true);
    }

    @Override
    public void makeInvisible() {
        setVisible(false);
    }
}

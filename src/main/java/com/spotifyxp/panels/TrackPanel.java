/*
 * Copyright [2024-2026] [Gianluca Beil]
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

import com.spotify.metadata.Metadata;
import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.*;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.metadata.EpisodeId;
import xyz.gianlu.librespot.metadata.TrackId;

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
                    Thread thread = new Thread(() -> {
                        advancedUriCache.clear();
                        ((DefaultTableModel)  advancedSongTable.getModel()).setRowCount(0);
                        try {
                            for (SpotifyUtils.TrackOrEpisode trackOrEpisode : SpotifyUtils.getAllTracksPlaylist(forUri)) {
                                if (trackOrEpisode.isTrack) {
                                    Metadata.Track track = trackOrEpisode.track;
                                    ((DefaultTableModel) advancedSongTable.getModel()).addRow(new Object[]{track.getName(), TrackUtils.calculateFileSizeKb(track.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDuration())});
                                    advancedUriCache.add(TrackId.fromHex(Utils.bytesToHex(track.getGid())).toSpotifyUri());
                                } else {
                                    Metadata.Episode episode = trackOrEpisode.episode;
                                    ((DefaultTableModel) advancedSongTable.getModel()).addRow(new Object[]{episode.getName(), TrackUtils.calculateFileSizeKb(episode.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(episode.getDuration())});
                                    advancedUriCache.add(EpisodeId.fromHex(Utils.bytesToHex(episode.getGid())).toSpotifyUri());
                                }
                            }
                        }catch (Exception e) {
                            ConsoleLogging.Throwable(e);
                        }
                    }, "Get playlist tracks");
                    thread.start();
                    break;
                case show:
                    for (Metadata.Episode episode : SpotifyUtils.getAllEpisodesShow(forUri)) {
                        ((DefaultTableModel) advancedSongTable.getModel()).addRow(new Object[]{episode.getName(), TrackUtils.calculateFileSizeKb(episode.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(episode.getDuration())});
                        advancedUriCache.add(EpisodeId.fromHex(Utils.bytesToHex(episode.getGid())).toSpotifyUri());
                    }
                    break;
                case album:
                    for (Metadata.Track track : SpotifyUtils.getAllTracksAlbum(forUri)) {
                        ((DefaultTableModel) advancedSongTable.getModel()).addRow(new Object[]{track.getName(), TrackUtils.calculateFileSizeKb(track.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDuration())});
                        advancedUriCache.add(TrackId.fromHex(Utils.bytesToHex(track.getGid())).toSpotifyUri());
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

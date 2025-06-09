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
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Episode;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.EpisodeWrapped;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Paging;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
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
                    try {
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
                    }
                }).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("ui.library.tabs.episodes.ctxmenu.getdescep"), new Runnable() {
            @Override
            public void run() {
                new Thread(() -> {
                    try {
                        Episode episode = InstanceManager.getSpotifyApi().getEpisode(
                                episodesUris.get(episodesTable.getSelectedRow()).split(":")[2]
                        ).build().execute();
                        openDialog(
                                String.format(PublicValues.language.translate("ui.library.tabs.episodes.epdescdialog.title"), episode.getName()),
                                episode.getDescription()
                        );
                    }catch (IOException e) {
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
                        Episode episode = InstanceManager.getSpotifyApi().getEpisode(
                                episodesUris.get(episodesTable.getSelectedRow()).split(":")[2]
                        ).build().execute();
                        openDialog(
                                String.format(PublicValues.language.translate("ui.library.tabs.episodes.showdescdialog.title"), episode.getShow().getName()),
                                episode.getShow().getDescription()
                        );
                    }catch (IOException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }).start();
            }
        });

        Events.subscribe(SpotifyXPEvents.librarychange.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                LibraryChange change = (LibraryChange) data[0];
                if(episodesUris.isEmpty()) return;
                if(change.getType() != LibraryChange.Type.EPISODE) return;
                if(change.getAction() == LibraryChange.Action.ADD) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Episode episode = InstanceManager.getSpotifyApi().getEpisode(change.getUri().split(":")[2]).build().execute();
                                episodesUris.add(0, episode.getUri());
                                episodesTable.addModifyAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((DefaultTableModel) episodesTable.getModel()).insertRow(0, new Object[]{
                                                episode.getName(),
                                                episode.getShow().getName(),
                                                TrackUtils.calculateFileSizeKb(episode),
                                                TrackUtils.getHHMMSSOfTrack(episode.getDurationMs())
                                        });
                                    }
                                });
                            }catch (IOException e) {
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
            }
        });

        setViewportView(episodesTable);
    }

    private void fetch() {
        try {
            int limit = 50;
            Paging<EpisodeWrapped> episodes = InstanceManager.getSpotifyApi().getUsersSavedEpisodes()
                    .limit(limit).build().execute();
            int total = episodes.getTotal();
            int offset = 0;
            while (offset < total) {
                for (EpisodeWrapped episode : episodes.getItems()) {
                    episodesUris.add(episode.getEpisode().getUri());
                    episodesTable.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            ((DefaultTableModel) episodesTable.getModel()).addRow(new Object[]{
                                    episode.getEpisode().getName(),
                                    episode.getEpisode().getShow().getName(),
                                    TrackUtils.calculateFileSizeKb(episode.getEpisode()),
                                    TrackUtils.getHHMMSSOfTrack(episode.getEpisode().getDurationMs())
                            });
                        }
                    });
                    offset++;
                }
                episodes = InstanceManager.getSpotifyApi().getUsersSavedEpisodes()
                        .limit(limit).offset(offset).build().execute();
            }
        } catch (IOException e) {
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

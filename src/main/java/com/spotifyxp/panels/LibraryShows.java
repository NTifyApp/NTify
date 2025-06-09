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
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Paging;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.SavedShow;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Show;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.swingextension.JDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOError;
import java.io.IOException;
import java.util.ArrayList;

public class LibraryShows extends JScrollPane {
    public static DefTable showsTable;
    public static ArrayList<String> showsUris;
    public static ContextMenu contextMenu;

    public LibraryShows() {
        showsUris = new ArrayList<>();

        showsTable = new DefTable();
        showsTable.setForeground(PublicValues.globalFontColor);
        showsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        showsTable.setModel(new DefaultTableModel(new Object[][]{}, new Object[]{
                PublicValues.language.translate("ui.library.tabs.shows.table.column1"),
                PublicValues.language.translate("ui.library.tabs.shows.table.column2")
        }));
        showsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(showsTable.getSelectedRow() == -1) return;
                if(e.getClickCount() == 2) {
                    ContentPanel.trackPanel.open(
                            showsUris.get(showsTable.getSelectedRow()).split(":")[2],
                            HomePanel.ContentTypes.show
                    );
                }
            }
        });

        contextMenu = new ContextMenu(showsTable, showsUris, getClass());
        contextMenu.addItem(PublicValues.language.translate("ui.general.refresh"), new Runnable() {
            @Override
            public void run() {
                ((DefaultTableModel) showsTable.getModel()).setRowCount(0);
                showsUris.clear();
                fill();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("ui.library.tabs.shows.ctxmenu.remove"), new Runnable() {
            @Override
            public void run() {
                if(showsTable.getSelectedRow() == -1) return;
                new Thread(() -> {
                    try {
                        InstanceManager.getSpotifyApi().removeUsersSavedShows(
                                showsUris.get(showsTable.getSelectedRow()).split(":")[2]
                        ).build().execute();
                        Events.triggerEvent(SpotifyXPEvents.librarychange.getName(), new LibraryChange(
                                showsUris.get(showsTable.getSelectedRow()),
                                LibraryChange.Type.SHOW,
                                LibraryChange.Action.REMOVE
                        ));
                    }catch (IOException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }).start();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("ui.library.tabs.shows.ctxmenu.showdesc"), new Runnable() {
            @Override
            public void run() {
                if(showsTable.getSelectedRow() == -1) return;
                new Thread(() -> {
                    try {
                        Show show = InstanceManager.getSpotifyApi().getShow(
                                showsUris.get(showsTable.getSelectedRow()).split(":")[2]
                        ).build().execute();
                        openDialog(
                                String.format(PublicValues.language.translate("ui.library.tabs.shows.descdialog.title"), show.getName()),
                                show.getDescription()
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
                if(showsUris.isEmpty()) return;
                if(change.getType() != LibraryChange.Type.SHOW) return;
                if(change.getAction() == LibraryChange.Action.ADD) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Show show = InstanceManager.getSpotifyApi().getShow(change.getUri().split(":")[2]).build().execute();
                                showsUris.add(0, show.getUri());
                                showsTable.addModifyAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((DefaultTableModel) showsTable.getModel()).insertRow(0, new Object[]{
                                                show.getName(),
                                                show.getPublisher()
                                        });
                                    }
                                });
                            }catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }, "Library add show").start();
                }else{
                    for(int uri = 0; uri < showsUris.size(); uri++) {
                        if(showsUris.get(uri).equals(change.getUri())) {
                            showsUris.remove(uri);
                            ((DefaultTableModel) showsTable.getModel()).removeRow(uri);
                        }
                    }
                }
            }
        });

        setViewportView(showsTable);
    }

    private void openDialog(
            String title,
            String text
    ) throws IOException {
        com.spotifyxp.swingextension.JDialog dialog = new JDialog();
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

    private void fetch() {
        try {
            int limit = 50;
            Paging<SavedShow> shows = InstanceManager.getSpotifyApi().getUsersSavedShows()
                    .limit(limit).build().execute();
            int total = shows.getTotal();
            int offset = 0;
            while(offset < total){
                for(SavedShow show : shows.getItems()) {
                    showsUris.add(show.getShow().getUri());
                    showsTable.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            ((DefaultTableModel) showsTable.getModel()).addRow(new Object[]{
                                    show.getShow().getName(),
                                    show.getShow().getPublisher()
                            });
                        }
                    });
                    offset++;
                }
                shows = InstanceManager.getSpotifyApi().getUsersSavedShows()
                        .limit(limit).offset(offset).build().execute();
            }
        }catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    public void fill() {
        new Thread(() -> fetch()).start();
    }
}

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

import com.spotify.metadata.Metadata;
import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.events.LibraryChange;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.spotapi.pojos.LibraryResponse;
import com.spotifyxp.spotapi.requests.collection.CollectionSet;
import com.spotifyxp.swingextension.JDialog;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.ShowId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class LibraryShows extends JScrollPane {
    private static final String CACHE_ID = "shows";

    public static DefTable showsTable;
    public static ArrayList<String> showsUris;
    public static ContextMenu contextMenu;

    private static class ShowRow {
        String uri;
        String name;
        String publisher;

        ShowRow(String uri, String name, String publisher) {
            this.uri = uri;
            this.name = name;
            this.publisher = publisher;
        }
    }

    public LibraryShows() {
        showsUris = new ArrayList<>();

        showsTable = new DefTable();
        showsTable.setForeground(PublicValues.globalFontColor);
        showsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        showsTable.setModel(new DefaultTableModel(new Object[][]{}, new Object[]{
                PublicValues.language.translate("library.general.show_name"),
                PublicValues.language.translate("library.shows.table.publisher")
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
        contextMenu.addItem(PublicValues.language.translate("general.refresh"), new Runnable() {
            @Override
            public void run() {
                showsTable.addModifyAction(() -> ((DefaultTableModel) showsTable.getModel()).setRowCount(0));
                showsUris.clear();
                try {
                    PublicValues.cache.namespace("LibraryShows").remove(CACHE_ID);
                } catch (IOException e) {
                    ConsoleLogging.Throwable(e);
                }
                fill();
            }
        });
        contextMenu.addItem(PublicValues.language.translate("general.remove"), new Runnable() {
            @Override
            public void run() {
                if(showsTable.getSelectedRow() == -1) return;
                new Thread(() -> {
                    try {
                        PublicValues.spotAPI.collection().write()
                                .setSet(CollectionSet.SHOW)
                                .removeUris(showsUris.get(showsTable.getSelectedRow()))
                                .execute();
                        SpotifyXPEvents.libraryChange.trigger(new LibraryChange(
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
        contextMenu.addItem(PublicValues.language.translate("library.shows.context_menu.view_description"), new Runnable() {
            @Override
            public void run() {
                if(showsTable.getSelectedRow() == -1) return;
                new Thread(() -> {
                    try {
                        Metadata.Show show = PublicValues.session.api().getMetadata4Show(ShowId.fromUri(showsUris.get(showsTable.getSelectedRow())));
                        openDialog(
                                String.format(PublicValues.language.translate("dialogs.library.show_description.title"), show.getName()),
                                show.getDescription()
                        );
                    }catch (IOException | TokenProvider.TokenException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }).start();
            }
        });

        SpotifyXPEvents.libraryChange.subscribe((change) -> {
            if(showsUris.isEmpty()) return;
            if(change.getType() != LibraryChange.Type.SHOW) return;
            if(change.getAction() == LibraryChange.Action.ADD) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Metadata.Show show = PublicValues.session.api().getMetadata4Show(ShowId.fromUri(showsUris.get(showsTable.getSelectedRow())));
                            showsUris.add(0, showsUris.get(showsTable.getSelectedRow()));
                            showsTable.addModifyAction(new Runnable() {
                                @Override
                                public void run() {
                                    ((DefaultTableModel) showsTable.getModel()).insertRow(0, new Object[]{
                                            show.getName(),
                                            show.getPublisher()
                                    });
                                }
                            });
                        }catch (IOException | TokenProvider.TokenException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, "Library add show").start();
            }else{
                for(int uri = 0; uri < showsUris.size(); uri++) {
                    if(showsUris.get(uri).equals(change.getUri())) {
                        showsUris.remove(uri);
                        int removeIndex = uri;
                        showsTable.addModifyAction(() -> ((DefaultTableModel) showsTable.getModel()).removeRow(removeIndex));
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
        if (PublicValues.cache.namespace("LibraryShows").has(CACHE_ID)) {
            try {
                ShowRow[] rows = PublicValues.cache.namespace("LibraryShows").get(CACHE_ID, ShowRow[].class);
                for (ShowRow row : rows) {
                    showsUris.add(row.uri);
                    showsTable.addModifyAction(() -> ((DefaultTableModel) showsTable.getModel()).addRow(new Object[]{row.name, row.publisher}));
                }
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            return;
        }

        try {
            ArrayList<ShowRow> cacheRows = new ArrayList<>();
            LibraryResponse response = PublicValues.spotAPI.library().get().setFilters("Podcasts & Shows").setLimit(999999).setOffset(0).execute();

            for (LibraryResponse.LibraryRow item : response.getItems()) {
                LibraryResponse.ShowData show = item.getItem().asShow();
                if (show == null) continue;

                showsUris.add(show.getUri());
                String publisherName = show.getPublisher() != null ? show.getPublisher().getName() : "";
                cacheRows.add(new ShowRow(show.getUri(), show.getName(), publisherName));
                showsTable.addModifyAction(() -> {
                    ((DefaultTableModel) showsTable.getModel()).addRow(new Object[]{
                            show.getName(),
                            publisherName
                    });
                });
            }
            PublicValues.cache.namespace("LibraryShows").put(CACHE_ID, cacheRows);
        }catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    public void fill() {
        new Thread(() -> fetch()).start();
    }

    public static void evict() {
        if (showsUris == null || showsUris.isEmpty()) return;

        DefaultTableModel model = (DefaultTableModel) showsTable.getModel();
        ArrayList<ShowRow> rows = new ArrayList<>();
        for (int i = 0; i < model.getRowCount() && i < showsUris.size(); i++) {
            rows.add(new ShowRow(showsUris.get(i), (String) model.getValueAt(i, 0), (String) model.getValueAt(i, 1)));
        }
        try {
            PublicValues.cache.namespace("LibraryShows").put(CACHE_ID, rows);
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }

        showsUris.clear();
        showsTable.addModifyAction(() -> model.setRowCount(0));
    }
}

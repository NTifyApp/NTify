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
package com.spotifyxp.guielements;

import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.HomePanel;
import com.spotifyxp.panels.Views;
import com.spotifyxp.utils.GraphicalMessage;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpotifyBrowseSection extends JScrollPane {
    public DefTable table;
    private ArrayList<String> uris;
    private ContextMenu contextMenu;

    public SpotifyBrowseSection(List<ArrayList<String>> entries) {
        table = new DefTable();
        uris = new ArrayList<>();

        table.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.general.name"), PublicValues.language.translate("ui.general.description"), ""
                }
        ));
        table.setForeground(PublicValues.globalFontColor);
        table.getTableHeader().setForeground(PublicValues.globalFontColor);

        contextMenu = new ContextMenu(table, uris, getClass());

        for(ArrayList<String> e : entries) {
            uris.add(e.get(3));
            table.addModifyAction(new Runnable() {
                @Override
                public void run() {
                    ((DefaultTableModel) table.getModel()).addRow(new Object[] {e.get(0), e.get(1), e.get(2)});
                }
            });
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    String entry = uris.get(table.getSelectedRow());
                    HomePanel.ContentTypes contentType;
                    switch(entry.split(":")[1].toLowerCase(Locale.ENGLISH)) {
                        case "playlist":
                            contentType = HomePanel.ContentTypes.playlist;
                            break;
                        case "album":
                            contentType = HomePanel.ContentTypes.album;
                            break;
                        case "show":
                            contentType = HomePanel.ContentTypes.show;
                            break;
                        case "episode":
                        case "track":
                            InstanceManager.getPlayer().getPlayer().load(entry, true, PublicValues.shuffle);
                            return;
                        default:
                            GraphicalMessage.bug("Called browse onclick with an unsupported content type: " + entry.split(":")[1]);
                            return;
                    }

                    //This opens the track panel but additionally also hijacks the back button
                    ContentPanel.trackPanel.open(entry, contentType, new Runnable() {
                        @Override
                        public void run() {
                            ContentPanel.switchView(Views.BROWSESECTION);
                            ContentPanel.lastViewPanel = ContentPanel.browsePanel;
                            ContentPanel.lastView = Views.BROWSE;
                            ContentPanel.frame.revalidate();
                            ContentPanel.frame.repaint();
                        }
                    });
                }
            }
        });

        setViewportView(table);
    }

    public SpotifyBrowseSection(ArrayList<UnofficialSpotifyAPI.SpotifyBrowseEntry> entries) {
        table = new DefTable();
        uris = new ArrayList<>();

        table.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.general.name"), ""
                }
        ));
        table.setForeground(PublicValues.globalFontColor);
        table.getTableHeader().setForeground(PublicValues.globalFontColor);
        contextMenu = new ContextMenu(table, uris, getClass());

        for(UnofficialSpotifyAPI.SpotifyBrowseEntry e : entries) {
            uris.add(e.getEvents().get().getEvents().get(0).getData_uri().get().getUri());
            table.addModifyAction(new Runnable() {
                @Override
                public void run() {
                    String subtitle = "";
                    if(e.getText().getSubtitle().isPresent()) {
                        subtitle = e.getText().getSubtitle().get();
                    } else if (e.getText().getDescription().isPresent()) {
                        subtitle = e.getText().getDescription().get();
                    }
                    ((DefaultTableModel) table.getModel()).addRow(new Object[] {e.getText().getTitle(), subtitle});
                }
            });
        }

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    String entry = uris.get(table.getSelectedRow());
                    HomePanel.ContentTypes contentType;
                    switch(entry.split(":")[1].toLowerCase(Locale.ENGLISH)) {
                        case "playlist":
                            contentType = HomePanel.ContentTypes.playlist;
                            break;
                        case "album":
                            contentType = HomePanel.ContentTypes.album;
                            break;
                        case "show":
                            contentType = HomePanel.ContentTypes.show;
                            break;
                        case "episode":
                        case "track":
                            InstanceManager.getSpotifyPlayer().load(entry, true, PublicValues.shuffle);
                            return;
                        default:
                            GraphicalMessage.bug("Called browse onclick with an unsupported content type: " + entry.split(":")[1]);
                            return;
                    }

                    //This opens the track panel but additionally also hijacks the back button
                    ContentPanel.trackPanel.open(entry, contentType, new Runnable() {
                        @Override
                        public void run() {
                            ContentPanel.switchView(Views.BROWSESECTION);
                            ContentPanel.lastViewPanel = ContentPanel.browsePanel;
                            ContentPanel.lastView = Views.BROWSE;
                            ContentPanel.frame.revalidate();
                            ContentPanel.frame.repaint();
                        }
                    });
                }
            }
        });

        setViewportView(table);
    }
}

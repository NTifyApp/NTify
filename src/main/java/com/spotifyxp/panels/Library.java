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

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class Library extends JScrollPane implements View {
    public static LibraryTracks libraryTracks;
    public static JTabbedPane tabbedPane;
    public static JPanel contentPanel;
    public static LibraryPlaylists libraryPlaylists;
    public static LibraryArtists libraryArtists;
    public static LibraryAlbums libraryAlbums;
    public static LibraryEpisodes libraryEpisodes;
    public static LibraryShows libraryShows;

    public Library() {
        setVisible(false);

        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        setViewportView(contentPanel);

        tabbedPane = new JTabbedPane();
        tabbedPane.setUI(new BasicTabbedPaneUI());
        tabbedPane.setForeground(PublicValues.globalFontColor);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                switch (tabbedPane.getSelectedIndex()) {
                    case 1:
                        if (LibraryAlbums.albumsTable.getModel().getRowCount() == 0) {
                            libraryAlbums.fill();
                        }
                        break;
                    case 2:
                        if (LibraryPlaylists.playlistsPlaylistsTable.getModel().getRowCount() == 0) {
                            libraryPlaylists.fill();
                        }
                        break;
                    case 3:
                        if (LibraryArtists.artistsTable.getModel().getRowCount() == 0) {
                            libraryArtists.fill();
                        }
                        break;
                    case 4:
                        if (LibraryEpisodes.episodesTable.getModel().getRowCount() == 0) {
                            libraryEpisodes.fill();
                        }
                    case 5:
                        if (LibraryShows.showsTable.getModel().getRowCount() == 0) {
                            libraryShows.fill();
                        }
                }
            }
        });

        libraryTracks = new LibraryTracks();
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.tracks"), libraryTracks);

        libraryAlbums = new LibraryAlbums();
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.albums"), libraryAlbums);

        libraryPlaylists = new LibraryPlaylists();
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.playlists"), libraryPlaylists);

        libraryArtists = new LibraryArtists();
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.artists"), libraryArtists);

        libraryEpisodes = new LibraryEpisodes();
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.episodes"), libraryEpisodes);

        libraryShows = new LibraryShows();
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.shows"), libraryShows);
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

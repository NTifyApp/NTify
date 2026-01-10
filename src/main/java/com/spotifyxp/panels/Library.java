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

        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.tracks"), null);
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.albums"), null);
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.playlists"), null);
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.artists"), null);
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.episodes"), null);
        tabbedPane.addTab(PublicValues.language.translate("ui.library.tabs.shows"), null);

        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                switch (tabbedPane.getSelectedIndex()) {
                    case 0:
                        if (tabbedPane.getComponentAt(0) == null) {
                            libraryTracks = new LibraryTracks();
                            tabbedPane.setComponentAt(0, libraryTracks);
                        }
                        if (LibraryTracks.librarySongList.getModel().getRowCount() == 0) {
                            libraryTracks.loadLibrary();
                        }
                        break;
                    case 1:
                        if (tabbedPane.getComponentAt(1) == null) {
                            libraryAlbums = new LibraryAlbums();
                            tabbedPane.setComponentAt(1, libraryAlbums);
                        }
                        if (LibraryAlbums.albumsTable.getModel().getRowCount() == 0) {
                            libraryAlbums.fill();
                        }
                        break;
                    case 2:
                        if (tabbedPane.getComponentAt(2) == null) {
                            libraryPlaylists = new LibraryPlaylists();
                            tabbedPane.setComponentAt(2, libraryPlaylists);
                        }
                        if (LibraryPlaylists.playlistsPlaylistsTable.getModel().getRowCount() == 0) {
                            libraryPlaylists.fill();
                        }
                        break;
                    case 3:
                        if (tabbedPane.getComponentAt(3) == null) {
                            libraryArtists = new LibraryArtists();
                            tabbedPane.setComponentAt(3, libraryArtists);
                        }
                        if (LibraryArtists.artistsTable.getModel().getRowCount() == 0) {
                            libraryArtists.fill();
                        }
                        break;
                    case 4:
                        if (tabbedPane.getComponentAt(4) == null) {
                            libraryEpisodes = new LibraryEpisodes();
                            tabbedPane.setComponentAt(4, libraryEpisodes);
                        }
                        if (LibraryEpisodes.episodesTable.getModel().getRowCount() == 0) {
                            libraryEpisodes.fill();
                        }
                        break;
                    case 5:
                        if (tabbedPane.getComponentAt(5) == null) {
                            libraryShows = new LibraryShows();
                            tabbedPane.setComponentAt(5, libraryShows);
                        }
                        if (LibraryShows.showsTable.getModel().getRowCount() == 0) {
                            libraryShows.fill();
                        }
                }

                tabbedPane.revalidate();
                tabbedPane.repaint();
            }
        });
    }

    @Override
    public void makeVisible() {
        setVisible(true);

        if (libraryTracks == null) {
            // Initial state
            tabbedPane.setSelectedIndex(0);
            for (ChangeListener listener : tabbedPane.getChangeListeners())
                listener.stateChanged(new ChangeEvent(tabbedPane));
        }

    }

    @Override
    public void makeInvisible() {
        setVisible(false);
    }
}

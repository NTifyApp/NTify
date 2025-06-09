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
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.SavedTrack;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Track;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;

public class LibraryTracks extends JScrollPane implements View {
    public static DefTable librarySongList;
    public static final ArrayList<String> libraryUriCache = new ArrayList<>();
    private static boolean libraryLoadingInProgress = false;
    public static ContextMenu contextMenu;
    public static int totalTracks = 0;
    public static Thread libraryThread;

    public LibraryTracks() {
        setVisible(false);
        final boolean[] inProg = {false};
        addMouseWheelListener(e -> {
            if (!inProg[0]) {
                inProg[0] = true;
                BoundedRangeModel m = getVerticalScrollBar().getModel();
                int extent = m.getExtent();
                int maximum = m.getMaximum();
                int value = m.getValue();
                if (value + extent >= maximum / 2) {
                    if (ContentPanel.currentView == Views.LIBRARY) {
                        if (!libraryLoadingInProgress) {
                            Thread thread = new Thread(LibraryTracks::loadNext, "Library load next");
                            thread.start();
                        }
                    }
                }
                inProg[0] = false;
            }
        });

        librarySongList = new DefTable();
        librarySongList.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("ui.library.songlist.songname"), PublicValues.language.translate("ui.library.songlist.filesize"), PublicValues.language.translate("ui.library.songlist.bitrate"), PublicValues.language.translate("ui.library.songlist.length")}));
        librarySongList.getTableHeader().setForeground(PublicValues.globalFontColor);
        librarySongList.setForeground(PublicValues.globalFontColor);
        librarySongList.getColumnModel().getColumn(0).setPreferredWidth(347);
        librarySongList.getColumnModel().getColumn(3).setPreferredWidth(51);
        librarySongList.setFillsViewportHeight(true);
        librarySongList.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    InstanceManager.getPlayer().getPlayer().load(libraryUriCache.get(librarySongList.getSelectedRow()), true, PublicValues.shuffle);
                    Thread thread1 = new Thread(() -> TrackUtils.addAllToQueue(libraryUriCache, librarySongList), "Library add to queue");
                    thread1.start();
                }
            }
        }));
        setViewportView(librarySongList);

        Events.subscribe(SpotifyXPEvents.librarychange.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                LibraryChange change = (LibraryChange) data[0];
                if(libraryUriCache.isEmpty()) return;
                if(change.getType() != LibraryChange.Type.TRACK) return;
                if(change.getAction() == LibraryChange.Action.ADD) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Track track = InstanceManager.getSpotifyApi().getTrack(change.getUri().split(":")[2]).build().execute();
                                libraryUriCache.add(0, track.getUri());
                                String a = TrackUtils.getArtists(track.getArtists());
                                librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).insertRow(0, new Object[]{track.getName() + " - " + a, TrackUtils.calculateFileSizeKb(track), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDurationMs())}));
                            }catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }, "Library add track").start();
                }else {
                    for(int uri = 0; uri < libraryUriCache.size(); uri++) {
                        libraryUriCache.remove(uri);
                        ((DefaultTableModel) librarySongList.getModel()).removeRow(uri);
                    }
                }
            }
        });

        createcontextMenu();
    }

    public void loadLibrary() {
        libraryThread = new Thread(new Runnable() {
            public void run() {
                try {
                    libraryLoadingInProgress = true;
                    int limit = 50;
                    Paging<SavedTrack> libraryTracks = InstanceManager.getSpotifyApi().getUsersSavedTracks().limit(limit).build().execute();
                    totalTracks = libraryTracks.getTotal();
                    for(SavedTrack track : libraryTracks.getItems()) {
                        libraryUriCache.add(track.getTrack().getUri());
                        String a = TrackUtils.getArtists(track.getTrack().getArtists());
                        librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).addRow(new Object[]{track.getTrack().getName() + " - " + a, TrackUtils.calculateFileSizeKb(track.getTrack()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getTrack().getDurationMs())}));
                    }
                    libraryLoadingInProgress = false;
                } catch (Exception e) {
                    ConsoleLogging.error("Error loading users library! Library now locked");
                    libraryLoadingInProgress = true;
                    throw new RuntimeException(e);
                }
            }
        }, "Library thread");
        libraryThread.start();
    }

    void createcontextMenu() {
        contextMenu = new ContextMenu(librarySongList, libraryUriCache, getClass());
        contextMenu.addItem(PublicValues.language.translate("ui.general.refresh"), () -> {
            libraryUriCache.clear();
            ((DefaultTableModel) librarySongList.getModel()).setRowCount(0);
            loadLibrary();
        });
        contextMenu.addItem("Add to queue", () -> {
            if(librarySongList.getSelectedRow() == -1) return;
            Events.triggerEvent(SpotifyXPEvents.addtoqueue.getName(), libraryUriCache.get(librarySongList.getSelectedRow()));
        });
        contextMenu.addItem(PublicValues.language.translate("ui.general.remove"), () -> {
            InstanceManager.getSpotifyApi().removeUsersSavedTracks(libraryUriCache.get(librarySongList.getSelectedRow()).split(":")[2]);
            Events.triggerEvent(SpotifyXPEvents.librarychange.getName(), new LibraryChange(
                    libraryUriCache.get(librarySongList.getSelectedRow()),
                    LibraryChange.Type.TRACK,
                    LibraryChange.Action.REMOVE
            ));
        });
    }

    public static void loadNext() {
        if (libraryLoadingInProgress) {
            return;
        }
        if(libraryUriCache.size() == totalTracks) return;
        try {
            libraryLoadingInProgress = true;
            int limit = 50;
            Paging<SavedTrack> libraryTracks = InstanceManager.getSpotifyApi().getUsersSavedTracks()
                    .offset(libraryUriCache.size()).limit(limit).build().execute();
            totalTracks = libraryTracks.getTotal();
            for(SavedTrack track : libraryTracks.getItems()) {
                libraryUriCache.add(track.getTrack().getUri());
                String a = TrackUtils.getArtists(track.getTrack().getArtists());
                librarySongList.addModifyAction(() -> ((DefaultTableModel) librarySongList.getModel()).addRow(new Object[]{track.getTrack().getName() + " - " + a, TrackUtils.calculateFileSizeKb(track.getTrack()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getTrack().getDurationMs())}));
            }
            libraryLoadingInProgress = false;
        } catch (Exception e) {
            libraryLoadingInProgress = true;
            ConsoleLogging.error("Error loading users library! Library now locked");
            throw new RuntimeException(e);
        }
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

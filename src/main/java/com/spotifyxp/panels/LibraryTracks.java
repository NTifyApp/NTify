package com.spotifyxp.panels;

import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Paging;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.SavedTrack;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.swingextension.ContextMenu;
import com.spotifyxp.utils.AsyncMouseListener;
import com.spotifyxp.utils.TrackUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

public class LibraryTracks extends JScrollPane implements View {
    public static DefTable librarySongList;
    public static final ArrayList<String> libraryUriCache = new ArrayList<>();
    private static boolean libraryLoadingInProgress = false;
    public static ContextMenu contextMenu;
    public static int totalTracks = 0;
    public static final Thread libraryThread = new Thread(new Runnable() {
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

        createcontextMenu();
    }

    void createcontextMenu() {
        contextMenu = new ContextMenu(librarySongList, libraryUriCache, getClass());
        contextMenu.addItem(PublicValues.language.translate("ui.general.refresh"), () -> {
            libraryUriCache.clear();
            librarySongList.removeAll();
            libraryThread.start();
        });
        contextMenu.addItem("Add to queue", () -> {
            if(librarySongList.getSelectedRow() == -1) return;
            Events.triggerEvent(SpotifyXPEvents.addtoqueue.getName(), libraryUriCache.get(librarySongList.getSelectedRow()));
        });
        contextMenu.addItem(PublicValues.language.translate("ui.general.remove"), () -> {
            InstanceManager.getSpotifyApi().removeUsersSavedTracks(libraryUriCache.get(librarySongList.getSelectedRow()).split(":")[2]);
            libraryUriCache.remove(librarySongList.getSelectedRow());
            ((DefaultTableModel) librarySongList.getModel()).removeRow(librarySongList.getSelectedRow());
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

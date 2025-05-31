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
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Artist;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.PlayHistory;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.history.PlaybackHistory;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.AsyncMouseListener;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.SpotifyUtils;
import com.spotifyxp.utils.TrackUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class HotList extends JSplitPane implements View {
    public static DefTable hotListPlaylistsTable;
    public static DefTable hotListSongsTable;
    public static JScrollPane hotListPlaylistsScrollPanel;
    public static JScrollPane hotListSongsScrollPanel;
    public static final ArrayList<String> hotListPlaylistCache = new ArrayList<>();
    public static final ArrayList<String> hotListSongListCache = new ArrayList<>();
    public static ContextMenu hotListPlaylistsPanelRightClickMenu;
    public static ContextMenu hotListSongsTablecontextmenu;

    public HotList() {
        setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        setVisible(false);

        hotListPlaylistsTable = new DefTable();
        hotListPlaylistsTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("ui.hotlist.playlistlist.playlists")}));
        hotListPlaylistsTable.setForeground(PublicValues.globalFontColor);
        hotListPlaylistsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        hotListPlaylistsTable.getColumnModel().getColumn(0).setPreferredWidth(623);
        hotListPlaylistsTable.setFillsViewportHeight(true);
        hotListPlaylistsTable.setColumnSelectionAllowed(true);
        hotListPlaylistsTable.getTableHeader().setReorderingAllowed(false);
        hotListPlaylistsTable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ((DefaultTableModel) hotListSongsTable.getModel()).setRowCount(0);
                    hotListSongListCache.clear();
                    for (TrackSimplified track : SpotifyUtils.getAllTracksAlbum(hotListPlaylistCache.get(hotListPlaylistsTable.getSelectedRow()))) {
                        String a = TrackUtils.getArtists(track.getArtists());
                        hotListSongListCache.add(track.getUri());
                        ((DefaultTableModel) hotListSongsTable.getModel()).addRow(new Object[]{track.getName() + " - " + a, TrackUtils.calculateFileSizeKb(track), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDurationMs())});
                    }
                }
            }
        }));

        hotListPlaylistsScrollPanel = new JScrollPane();
        hotListPlaylistsScrollPanel.setPreferredSize(new Dimension(259, getHeight()));
        hotListPlaylistsScrollPanel.setViewportView(hotListPlaylistsTable);
        setLeftComponent(hotListPlaylistsScrollPanel);

        hotListPlaylistsPanelRightClickMenu = new ContextMenu(hotListPlaylistsTable, hotListPlaylistCache, getClass());
        hotListPlaylistsPanelRightClickMenu.addItem(PublicValues.language.translate("ui.general.refresh"), () -> {
            hotListPlaylistCache.clear();
            hotListSongListCache.clear();
            ((DefaultTableModel) hotListSongsTable.getModel()).setRowCount(0);
            ((DefaultTableModel) hotListPlaylistsTable.getModel()).setRowCount(0);
            fetchHotlist();
        });

        hotListSongsTable = new DefTable();
        hotListSongsTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("ui.hotlist.songlist.songtitle"), PublicValues.language.translate("ui.hotlist.songlist.filesize"), PublicValues.language.translate("ui.hotlist.songlist.bitrate"), PublicValues.language.translate("ui.hotlist.songlist.length")}));
        hotListSongsTable.getColumnModel().getColumn(0).setPreferredWidth(363);
        hotListSongsTable.getColumnModel().getColumn(1).setPreferredWidth(89);
        hotListSongsTable.getColumnModel().getColumn(3).setPreferredWidth(96);
        hotListSongsTable.setFillsViewportHeight(true);
        hotListSongsTable.setColumnSelectionAllowed(true);
        hotListSongsTable.setForeground(PublicValues.globalFontColor);
        hotListSongsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        hotListSongsTable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                hotListSongsTable.setColumnSelectionInterval(0, hotListSongsTable.getColumnCount() - 1);
                if (e.getClickCount() == 2) {
                    InstanceManager.getPlayer().getPlayer().load(hotListSongListCache.get(hotListSongsTable.getSelectedRow()), true, PublicValues.shuffle);
                    TrackUtils.addAllToQueue(hotListSongListCache, hotListSongsTable);
                }
            }
        }));

        hotListSongsScrollPanel = new JScrollPane();
        hotListSongsScrollPanel.setViewportView(hotListSongsTable);
        setRightComponent(hotListSongsScrollPanel);

        hotListSongsTablecontextmenu = new ContextMenu(hotListSongsTable, hotListSongListCache, getClass());
    }

    public static void fetchHotlist() {
        try {
            Artist[] artist = InstanceManager.getSpotifyApi().getUsersTopArtists().build().execute().getItems();
            ArrayList<String> genres = new ArrayList<>();
            ArrayList<String> tracks = new ArrayList<>();
            ArrayList<String> artists = new ArrayList<>();
            ArrayList<PlaybackHistory.SongEntry> entries = PublicValues.history.get15Songs(0);
            if(entries.isEmpty()) {
                for(PlayHistory h : InstanceManager.getSpotifyApi().getCurrentUsersRecentlyPlayedTracks().limit(10).build().execute().getItems()) {
                    tracks.add(h.getTrack().getUri().split(":")[2]);
                }
            }else {
                for (PlaybackHistory.SongEntry entry : entries) {
                    tracks.add(entry.songURI.split(":")[2]);
                }
            }
            for (Artist a : artist) {
                genres.addAll(Arrays.asList(a.getGenres()));
                artists.add(a.getUri().split(":")[2]);
            }
            String[] fiveartists = getRandomValues(artists, 2);
            String[] fivegenres = getRandomValues(genres, 2);
            String[] fivetracks = getRandomValues(tracks, 1);
            TrackSimplified[] finaltracks = InstanceManager.getSpotifyApi().getRecommendations().seed_artists(Arrays.toString(fiveartists).replace("  ", ",").replace(" ", "")).seed_genres(Arrays.toString(fivegenres).replace("  ", ",").replace(" ", "")).seed_tracks(Arrays.toString(fivetracks).replace("  ", ",").replace(" ", "")).build().execute().getTracks();
            for (TrackSimplified t : finaltracks) {
                try {
                    AlbumSimplified albumreq = InstanceManager.getSpotifyApi().getTrack(t.getUri().split(":")[2]).build().execute().getAlbum();
                    String a = TrackUtils.getArtists(albumreq.getArtists());
                    ((DefaultTableModel) hotListPlaylistsTable.getModel()).addRow(new Object[]{albumreq.getName() + " - " + a});
                    hotListPlaylistCache.add(albumreq.getUri());
                } catch (IOException e) {
                    ConsoleLogging.Throwable(e);
                }
            }
        } catch (Exception exception) {
            GraphicalMessage.openException(exception);
        }
    }

    private static String[] getRandomValues(ArrayList<String> inputList, int count) {
        if (count <= 0) {
            return new String[]{}; // Return an empty list if count is zero or negative.
        }
        // Create a copy of the input list to avoid modifying the original list.
        ArrayList<String> copyList = new ArrayList<>(inputList);
        // Shuffle the copy list to randomize the order.
        Collections.shuffle(copyList);
        // Ensure that count does not exceed the size of the copy list.
        count = Math.min(count, copyList.size());
        // Create a sublist containing the first 'count' elements from the shuffled list.
        return new String[]{copyList.subList(0, count).toString().replaceAll("\\[", "").replaceAll("]", "")};
    }

    @Override
    public void makeVisible() {
        if (HotList.hotListPlaylistsTable.getRowCount() == 0) {
            Thread t = new Thread(HotList::fetchHotlist, "Get HotList");
            t.start();
        }
        setVisible(true);
    }

    @Override
    public void makeInvisible() {
        setVisible(false);
    }
}

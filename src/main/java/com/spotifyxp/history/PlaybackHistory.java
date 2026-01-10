/*
 * Copyright [2023-2026] [Gianluca Beil]
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
package com.spotifyxp.history;

import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.de.werwolf2303.sql.*;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.AlbumId;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.ArtistId;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.TrackId;
import com.spotifyxp.graphics.Graphics;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.HomePanel;
import com.spotifyxp.panels.PlayerArea;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.swingextension.URITree;
import com.spotifyxp.utils.AsyncActionListener;
import com.spotifyxp.utils.AsyncMouseListener;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;

@SuppressWarnings("Duplicates")
public class PlaybackHistory {

    public static class SongEntry {
        public String songURI;
        public String songName;
        public String artistName;
        public String artistURI;
        public String albumName;
        public String albumURI;
    }

    public static class TreeEntry {
        public String name;
        public String uri;
        public DefaultMutableTreeNode addedTo;

        public TreeEntry(String name, String uri, DefaultMutableTreeNode addedTo) {
            this.name = name;
            this.uri = uri;
            this.addedTo = addedTo;
        }
    }

    private static ArrayList<PlaybackHistory.TreeEntry> addedArtists;
    private static int offset = 0;
    private static SQLTable sqlTable;
    private Ui ui;

    public static class Ui extends JFrame {
        public JButton removeAll;
        public JScrollPane pane;

        private final URITree tree;
        private final DefaultMutableTreeNode root;

        private volatile boolean stop = false;

        private Thread fetchHistoryThread;

        public Ui() {
            setPreferredSize(new Dimension(300, 400));
            setTitle(PublicValues.language.translate("ui.history.title"));

            root = new DefaultMutableTreeNode(PublicValues.language.translate("ui.history.tree.root"));
            tree = new URITree(root);

            pane = new JScrollPane(tree);
            add(pane, BorderLayout.CENTER);

            removeAll = new JButton(PublicValues.language.translate("ui.history.removeall"));

            removeAll.addActionListener(new AsyncActionListener(e -> {
                try {
                    removeAllSongs();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }));

            add(removeAll, BorderLayout.SOUTH);

            tree.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        ContentPanel.switchView(ContentPanel.lastView);
                        try {
                            DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getSelectionModel().getSelectionPath().getLastPathComponent();
                            URITree.TreeNodeData data = ((URITree.TreeNodeData) node.getUserObject());
                            switch (data.getNodetype()) {
                                case ARTIST:
                                    ContentPanel.showArtistPanel(data.getURI());
                                    break;
                                case ALBUM:
                                    ContentPanel.trackPanel.open(data.getURI(), HomePanel.ContentTypes.album);
                                    break;
                                case TRACK:
                                    InstanceManager.getSpotifyPlayer().load(data.getURI(), true, PublicValues.shuffle);
                                    break;
                                case LOADMORE:
                                    loadMore();
                                    break;
                                default:
                                    break;
                            }
                        } catch (Exception ignored) {
                            tree.expandRow(0);
                        }
                    }
                }
            }));

            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    PlayerArea.historyButton.isFilled = false;
                    PlayerArea.historyButton.setImage(Graphics.HISTORY.getInputStream());
                    dispose();
                }
            });
        }

        void loadMore() {
            try {
                try {
                    ((DefaultTreeModel) tree.getModel()).removeNodeFromParent(((DefaultMutableTreeNode) root.getChildAt(root.getChildCount() - 1)));
                    for (PlaybackHistory.SongEntry entry : PublicValues.history.get15Songs(offset)) {
                        DefaultMutableTreeNode addedTo;
                        DefaultMutableTreeNode artist = new DefaultMutableTreeNode(new URITree.TreeNodeData(entry.artistName, entry.artistURI, URITree.NodeType.ARTIST));
                        ((DefaultTreeModel) tree.getModel()).insertNodeInto(artist, root, root.getChildCount());
                        root.insert(artist, root.getChildCount() - 1);
                        addedArtists.add(new PlaybackHistory.TreeEntry(entry.artistName, entry.artistURI, root));
                        addedTo = artist;
                        DefaultMutableTreeNode album = new DefaultMutableTreeNode(new URITree.TreeNodeData(entry.albumName, entry.albumURI, URITree.NodeType.ALBUM));
                        addedTo.add(album);
                        addedTo = album;
                        DefaultMutableTreeNode track = new DefaultMutableTreeNode(new URITree.TreeNodeData(entry.songName, entry.songURI, URITree.NodeType.TRACK));
                        addedTo.add(track);
                        offset++;
                    }
                    for (int i = 0; i < root.getChildCount(); i++) {
                        if (root.getChildAt(i).getChildCount() == 0) {
                            removeEntry(root.getChildAt(i), addedArtists);
                            root.remove(i);
                        }
                    }
                    if (sqlTable.tryGetRowCount() - 1 > offset) {
                        root.add(new DefaultMutableTreeNode(new URITree.TreeNodeData(PublicValues.language.translate("ui.general.loadmore"), "", URITree.NodeType.LOADMORE)));
                    }
                    int curPos = pane.getVerticalScrollBar().getValue();
                    ((DefaultTreeModel) tree.getModel()).reload();
                    SwingUtilities.invokeLater(() -> {
                        pane.getVerticalScrollBar().setValue(curPos);
                    });
                } catch (Exception e) {
                    ConsoleLogging.Throwable(e);
                }
            } catch (Exception ignored) {
            }
        }

        void removeEntry(TreeNode node, ArrayList<PlaybackHistory.TreeEntry> entries) {
            int toRemove = 0;
            boolean found = false;
            for (int i = 0; i < entries.size(); i++) {
                PlaybackHistory.TreeEntry entry = entries.get(i);
                if (entry.addedTo == node) {
                    toRemove = i;
                    found = true;
                    break;
                }
            }
            if (found) {
                entries.remove(toRemove);
            }
        }

        public void removeAllSongs() throws SQLException {
            sqlTable.clearTable();
            root.removeAllChildren();
            ((DefaultTreeModel) tree.getModel()).reload();
        }

        @Override
        public void open() {
            offset = 0;
            addedArtists = new ArrayList<>();
            root.removeAllChildren();
            ((DefaultTreeModel) tree.getModel()).reload();

            fetchHistoryThread = new Thread(() -> {
                try {
                    for (PlaybackHistory.SongEntry entry : PublicValues.history.get15Songs(0)) {
                        if (stop) break;
                        DefaultMutableTreeNode addedTo;
                        DefaultMutableTreeNode artist = new DefaultMutableTreeNode(new URITree.TreeNodeData(entry.artistName, entry.artistURI, URITree.NodeType.ARTIST));
                        ((DefaultTreeModel) tree.getModel()).insertNodeInto(artist, root, root.getChildCount());
                        root.insert(artist, root.getChildCount() - 1);
                        addedArtists.add(new PlaybackHistory.TreeEntry(entry.artistName, entry.artistURI, root));
                        addedTo = artist;
                        DefaultMutableTreeNode album = new DefaultMutableTreeNode(new URITree.TreeNodeData(entry.albumName, entry.albumURI, URITree.NodeType.ALBUM));
                        addedTo.add(album);
                        addedTo = album;
                        DefaultMutableTreeNode track = new DefaultMutableTreeNode(new URITree.TreeNodeData(entry.songName, entry.songURI, URITree.NodeType.TRACK));
                        addedTo.add(track);
                        offset++;
                    }
                    if (stop) return;
                    for (int i = 0; i < root.getChildCount(); i++) {
                        if (root.getChildAt(i).getChildCount() == 0) {
                            removeEntry(root.getChildAt(i), addedArtists);
                            root.remove(i);
                        }
                    }
                    if (sqlTable.tryGetRowCount() - 1 > offset) {
                        root.add(new DefaultMutableTreeNode(new URITree.TreeNodeData(PublicValues.language.translate("ui.general.loadmore"), "", URITree.NodeType.LOADMORE)));
                    }
                    tree.expandRow(0);
                } catch (Exception e) {
                    ConsoleLogging.Throwable(e);
                }
            }, "Fetch playback history");
            fetchHistoryThread.start();
            super.open();
        }

        @Override
        public void dispose() {
            stop = true;
            if (fetchHistoryThread.isAlive()) {
                try {
                    fetchHistoryThread.wait();
                } catch (InterruptedException ignored) {
                }
            }
            PublicValues.history.ui = null;
            super.dispose();
        }
    }

    public PlaybackHistory() {
        String databasePath = new File(PublicValues.fileslocation, "playbackhistory.db").getAbsolutePath();

        SQLSession sqlSession = new SQLSession(databasePath);
        sqlSession.loadDriver("org.sqlite.JDBC", "jdbc", "sqlite");

        try {
            sqlSession.connect();
        } catch (SQLException e) {
            ConsoleLogging.error("Can't establish a connection to the playback history database");
            PublicValues.history = null;
            return;
        }

        sqlTable = new SQLTable("history");

        sqlSession.initSQLElement(sqlTable);
        try {
            if (!sqlTable.exists()) {
                try {
                    sqlTable.create(new SQLEntryPair("track_uri", false, SQLEntryTypes.STRING),
                            new SQLEntryPair("track_name", false, SQLEntryTypes.STRING),
                            new SQLEntryPair("artist_uri", false, SQLEntryTypes.STRING),
                            new SQLEntryPair("artist_name", false, SQLEntryTypes.STRING),
                            new SQLEntryPair("album_uri", false, SQLEntryTypes.STRING),
                            new SQLEntryPair("album_name", false, SQLEntryTypes.STRING),
                            new SQLEntryPair("count", false, SQLEntryTypes.INTEGER));
                } catch (SQLException e) {
                    PublicValues.history = null;
                }
            }
        } catch (SQLException e) {
            PublicValues.history = null;
        }
    }

    public void open() {
        this.ui = new Ui();
        this.ui.open();
    }

    public void dispose() {
        this.ui.dispose();
    }

    public ArrayList<PlaybackHistory.SongEntry> get15Songs(int offset) {
        ArrayList<PlaybackHistory.SongEntry> songs = new ArrayList<>();
        int counter = 0;
        try {
            PlaybackHistory.SongEntry entry = new PlaybackHistory.SongEntry();
            for (SQLEntryPair pair : sqlTable.parseTableBackwards(15, offset, "count")) {
                switch (counter) {
                    case 0:
                        entry.songURI = pair.getValueString();
                        break;
                    case 1:
                        entry.songName = pair.getValueString();
                        break;
                    case 2:
                        entry.artistURI = pair.getValueString();
                        break;
                    case 3:
                        entry.artistName = pair.getValueString();
                        break;
                    case 4:
                        entry.albumName = pair.getValueString();
                        break;
                    case 5:
                        entry.albumURI = pair.getValueString();
                        songs.add(entry);
                        break;
                    case 6:
                        entry = new PlaybackHistory.SongEntry();
                        counter = 0;
                        continue;
                }
                counter++;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (ClassCastException ignored) {
        }
        return songs;
    }

    public void addSong(Metadata.Track t) throws SQLException {
        String uri = TrackId.fromHex(Utils.bytesToHex(t.getGid()).toLowerCase()).toSpotifyUri();
        String albumUri = AlbumId.fromHex(Utils.bytesToHex(t.getAlbum().getGid()).toLowerCase()).toSpotifyUri();
        String artistUri = ArtistId.fromHex(Utils.bytesToHex(t.getArtist(0).getGid()).toLowerCase()).toSpotifyUri();
        sqlTable.insertIntoTable(new SQLInsert(uri, SQLEntryTypes.STRING),
                new SQLInsert(t.getName(), SQLEntryTypes.STRING),
                new SQLInsert(artistUri, SQLEntryTypes.STRING),
                new SQLInsert(t.getArtist(0).getName(), SQLEntryTypes.STRING),
                new SQLInsert(t.getAlbum().getName(), SQLEntryTypes.STRING),
                new SQLInsert(albumUri, SQLEntryTypes.STRING),
                new SQLInsert(sqlTable.getRowCount(), SQLEntryTypes.INTEGER));
    }

    public int getSize() {
        return sqlTable.tryGetRowCount();
    }
}

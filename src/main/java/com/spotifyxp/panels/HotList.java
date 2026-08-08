/*
 * Copyright [2024-2026] [Gianluca Beil]
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
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.history.PlaybackHistory;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.spotapi.pojos.ArtistRelatedArtistsResponse;
import com.spotifyxp.utils.AsyncUtils;
import com.spotifyxp.utils.ReentryGuard;
import com.spotifyxp.utils.SpotifyUtils;
import com.spotifyxp.utils.TrackUtils;
import org.jetbrains.annotations.Nullable;
import xyz.gianlu.librespot.dealer.ApiClient;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.mercury.MercuryClient;
import xyz.gianlu.librespot.metadata.ArtistId;
import xyz.gianlu.librespot.metadata.TrackId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class HotList extends JSplitPane implements View {
    private static final String CACHE_ID = "recommendations";

    public static DefTable hotListPlaylistsTable;
    public static DefTable hotListSongsTable;
    public static JScrollPane hotListPlaylistsScrollPanel;
    public static JScrollPane hotListSongsScrollPanel;
    public static final ArrayList<String> hotListPlaylistCache = new ArrayList<>();
    public static final ArrayList<String> hotListSongListCache = new ArrayList<>();
    public static ContextMenu hotListPlaylistsPanelRightClickMenu;
    public static ContextMenu hotListSongsTablecontextmenu;
    private static final ReentryGuard songsLoadGuard = new ReentryGuard();

    public HotList() {
        setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        setVisible(false);

        hotListPlaylistsTable = new DefTable();
        hotListPlaylistsTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("general.name")}));
        hotListPlaylistsTable.setForeground(PublicValues.globalFontColor);
        hotListPlaylistsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        hotListPlaylistsTable.getColumnModel().getColumn(0).setPreferredWidth(623);
        hotListPlaylistsTable.setFillsViewportHeight(true);
        hotListPlaylistsTable.setColumnSelectionAllowed(true);
        hotListPlaylistsTable.getTableHeader().setReorderingAllowed(false);
        hotListPlaylistsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (!songsLoadGuard.tryEnter()) return;
                    AsyncUtils.run(() -> {
                        try {
                            hotListSongsTable.addModifyAction(() -> ((DefaultTableModel) hotListSongsTable.getModel()).setRowCount(0));
                            hotListSongListCache.clear();
                            try {
                                for (Metadata.Track track : SpotifyUtils.getAllTracksAlbum(hotListPlaylistCache.get(hotListPlaylistsTable.getSelectedRow()))) {
                                    String a = TrackUtils.getArtists(track.getArtistList());
                                    hotListSongListCache.add(TrackId.fromHex(Utils.bytesToHex(track.getGid().toByteArray())).toSpotifyUri());
                                    hotListSongsTable.addModifyAction(() -> ((DefaultTableModel) hotListSongsTable.getModel()).addRow(new Object[]{track.getName() + " - " + a, TrackUtils.calculateFileSizeKb(track.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDuration())}));
                                }
                            } catch (IOException | TokenProvider.TokenException ex) {
                                throw new RuntimeException(ex);
                            }
                        } finally {
                            songsLoadGuard.exit();
                        }
                    });
                }
            }
        });

        hotListPlaylistsScrollPanel = new JScrollPane();
        hotListPlaylistsScrollPanel.setPreferredSize(new Dimension(259, getHeight()));
        hotListPlaylistsScrollPanel.setViewportView(hotListPlaylistsTable);
        setLeftComponent(hotListPlaylistsScrollPanel);

        hotListPlaylistsPanelRightClickMenu = new ContextMenu(hotListPlaylistsTable, hotListPlaylistCache, getClass());
        hotListPlaylistsPanelRightClickMenu.addItem(PublicValues.language.translate("general.refresh"), () -> {
            hotListPlaylistCache.clear();
            hotListSongListCache.clear();
            hotListSongsTable.addModifyAction(() -> ((DefaultTableModel) hotListSongsTable.getModel()).setRowCount(0));
            hotListPlaylistsTable.addModifyAction(() -> ((DefaultTableModel) hotListPlaylistsTable.getModel()).setRowCount(0));
            try {
                PublicValues.cache.namespace("HotList").remove(CACHE_ID);
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            fetchHotlist();
        });

        hotListSongsTable = new DefTable();
        hotListSongsTable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("general.name"), PublicValues.language.translate("general.filesize"), PublicValues.language.translate("general.bitrate"), PublicValues.language.translate("general.length")}));
        hotListSongsTable.getColumnModel().getColumn(0).setPreferredWidth(363);
        hotListSongsTable.getColumnModel().getColumn(1).setPreferredWidth(89);
        hotListSongsTable.getColumnModel().getColumn(3).setPreferredWidth(96);
        hotListSongsTable.setFillsViewportHeight(true);
        hotListSongsTable.setColumnSelectionAllowed(true);
        hotListSongsTable.setForeground(PublicValues.globalFontColor);
        hotListSongsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        hotListSongsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                hotListSongsTable.setColumnSelectionInterval(0, hotListSongsTable.getColumnCount() - 1);
                if (e.getClickCount() == 2) {
                    AsyncUtils.run(() -> {
                        InstanceManager.getPlayer().getPlayer().load(hotListSongListCache.get(hotListSongsTable.getSelectedRow()), true, PublicValues.shuffle);
                        TrackUtils.addAllToQueue(hotListSongListCache, hotListSongsTable);
                    });
                }
            }
        });

        hotListSongsScrollPanel = new JScrollPane();
        hotListSongsScrollPanel.setViewportView(hotListSongsTable);
        setRightComponent(hotListSongsScrollPanel);

        hotListSongsTablecontextmenu = new ContextMenu(hotListSongsTable, hotListSongListCache, getClass());
    }

    private static class HotListEntry {
        String uri;
        String display;

        HotListEntry(String uri, String display) {
            this.uri = uri;
            this.display = display;
        }
    }

    public static void fetchHotlist() {
        if (PublicValues.cache.namespace("HotList").has(CACHE_ID)) {
            try {
                HotListEntry[] entries = PublicValues.cache.namespace("HotList").get(CACHE_ID, HotListEntry[].class);
                for (HotListEntry entry : entries) {
                    hotListPlaylistsTable.addModifyAction(() -> {
                        hotListPlaylistCache.add(entry.uri);
                        ((DefaultTableModel) hotListPlaylistsTable.getModel()).addRow(new Object[]{entry.display});
                    });
                }
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            return;
        }

        if (PublicValues.history == null) return;

        int historySize = PublicValues.history.getSize();

        if (historySize < 30) return;

        ArrayList<PlaybackHistory.SongEntry> recentTracks = PublicValues.history.get15Songs(historySize  - 30);

        try {
            List<RecommendationsAlgorithm.RecommendationItem> recommended = RecommendationsAlgorithm.computeRecommendations(recentTracks);
            List<HotListEntry> cacheEntries = new ArrayList<>();

            for (RecommendationsAlgorithm.RecommendationItem item : recommended) {
                switch (item.type) {
                    case ALBUM: {
                        String display = item.name + " - " + item.artistName;
                        cacheEntries.add(new HotListEntry(item.uri, display));
                        hotListPlaylistsTable.addModifyAction(() -> {
                            hotListPlaylistCache.add(item.uri);
                            ((DefaultTableModel) hotListPlaylistsTable.getModel()).addRow(new Object[]{display});
                        });
                        break;
                    }
                }
            }

            PublicValues.cache.namespace("HotList").put(CACHE_ID, cacheEntries);
        } catch (IOException | MercuryClient.MercuryException e) {
            ConsoleLogging.Throwable(e);
        }
    }

    private static class RecommendationsAlgorithm {

        private static class ArtistNode {
            public String artistURI;
            public int playCount;
            public Map<String, AlbumNode> albums = new HashMap<>();
            public double userWeight;
            public ArtistRelatedArtistsResponse.RelatedArtists relatedArtists;
            public double relatedHits;
            @Nullable
            public String name;
        }

        public static class RecommendationItem {
            public enum Type { ARTIST, ALBUM, TRACK }
            public final Type type;
            public final String uri;
            public final String name;
            public final String artistURI;
            public final String artistName;
            public final String albumURI;
            public final String albumName;
            public final double score;
            public static RecommendationItem fromTrack(TrackNode track) {
                return new RecommendationItem(
                        Type.TRACK,
                        track.trackURI,
                        track.name,
                        track.albumNode.artistNode.artistURI,
                        track.albumNode.artistNode.name,
                        track.albumNode.albumURI,
                        track.albumNode.name,
                        track.score
                );
            }
            public static RecommendationItem fromAlbum(AlbumNode album) {
                return new RecommendationItem(
                        Type.ALBUM,
                        album.albumURI,
                        album.name,
                        album.artistNode.artistURI,
                        album.artistNode.name,
                        album.albumURI,
                        album.name,
                        album.playCount
                );
            }
            public static RecommendationItem fromArtist(ArtistNode artist) {
                return new RecommendationItem(
                        Type.ARTIST,
                        artist.artistURI,
                        artist.name,
                        null,
                        null,
                        null,
                        null,
                        artist.userWeight
                );
            }

            private RecommendationItem(Type type, String uri, String name,
                                       String artistURI, String artistName,
                                       String albumURI, String albumName,
                                       double score) {
                this.type = type;
                this.uri = uri;
                this.name = name;
                this.artistURI = artistURI;
                this.artistName = artistName;
                this.albumURI = albumURI;
                this.albumName = albumName;
                this.score = score;
            }
        }

        private static class AlbumNode {
            public String albumURI;
            public int playCount;
            public Map<String, TrackNode> tracks = new HashMap<>();
            public ArtistNode artistNode;
            public String name;
        }

        private static class TrackNode {
            public String trackURI;
            public int playCount;
            public AlbumNode albumNode;
            public double score;
            public String name;
        }

        private static Map<String, ArtistNode> buildHierarchy(List<PlaybackHistory.SongEntry> history) {
            // First pass: collect each distinct artist's URI/name with no network calls.
            Map<String, String> artistNamesByUri = new LinkedHashMap<>();
            for (PlaybackHistory.SongEntry entry : history) {
                artistNamesByUri.putIfAbsent(entry.artistURI, entry.artistName);
            }

            // Fan the related-artists lookups out in parallel instead of one-at-a-time.
            Map<String, Future<ArtistRelatedArtistsResponse>> relatedArtistsFutures = new HashMap<>();
            for (String artistURI : artistNamesByUri.keySet()) {
                relatedArtistsFutures.put(artistURI, AsyncUtils.submit(() -> PublicValues.spotAPI.artist().relatedArtists().setUri(artistURI).execute()));
            }

            Map<String, ArtistNode> artists = new HashMap<>();
            for (Map.Entry<String, String> e : artistNamesByUri.entrySet()) {
                String artistURI = e.getKey();
                try {
                    ArtistNode a = new ArtistNode();
                    a.artistURI = artistURI;
                    ArtistRelatedArtistsResponse response = relatedArtistsFutures.get(artistURI).get();
                    a.relatedArtists = response != null && response.getRelatedContent() != null
                            ? response.getRelatedContent().getRelatedArtists() : null;
                    a.name = e.getValue();
                    artists.put(artistURI, a);
                } catch (InterruptedException | ExecutionException ex) {
                    ConsoleLogging.Throwable(ex);
                    ConsoleLogging.warning("Failed to fetch related artists for " + artistURI);
                }
            }

            for (PlaybackHistory.SongEntry entry : history) {
                ArtistNode artist = artists.get(entry.artistURI);
                if (artist == null) continue;
                artist.playCount++;

                AlbumNode album = artist.albums.computeIfAbsent(entry.albumURI, uri -> {
                    AlbumNode al = new AlbumNode();
                    al.albumURI = uri;
                    al.artistNode = artist;
                    al.name = entry.albumName;
                    return al;
                });
                album.playCount++;

                TrackNode track = album.tracks.computeIfAbsent(entry.songURI, uri -> {
                    TrackNode t = new TrackNode();
                    t.trackURI = uri;
                    t.albumNode = album;
                    t.name = entry.songName;
                    return t;
                });
                track.playCount++;
            }

            return artists;
        }

        private static List<ArtistNode> sortPlayCount(Map<String, ArtistNode> artists) {
            List<ArtistNode> sortedArtists = artists.values().stream()
                    .sorted(Comparator.comparingInt(a -> -a.playCount))
                    .collect(Collectors.toList());

            for (ArtistNode artist : sortedArtists) {
                artist.albums = artist.albums.values().stream()
                        .sorted(Comparator.comparingInt(a -> -a.playCount))
                        .collect(Collectors.toMap(
                                a -> a.albumURI,
                                a -> a,
                                (a, b) -> a,
                                LinkedHashMap::new
                        ));

                for (AlbumNode album : artist.albums.values()) {
                    album.tracks = album.tracks.values().stream()
                            .sorted(Comparator.comparingInt(t -> -t.playCount))
                            .collect(Collectors.toMap(
                                    t -> t.trackURI,
                                    t -> t,
                                    (a, b) -> a,
                                    LinkedHashMap::new
                            ));
                }
            }
            return sortedArtists;
        }

        public static List<RecommendationItem> computeRecommendations(List<PlaybackHistory.SongEntry> history) throws IOException, MercuryClient.MercuryException {
            Map<String, ArtistNode> artistMap = buildHierarchy(history);
            List<ArtistNode> sortedArtists = sortPlayCount(artistMap);

            int maxPlayCount = sortedArtists.stream().mapToInt(a -> a.playCount).max().orElse(1);
            for (ArtistNode artist : sortedArtists) artist.userWeight = Math.log(artist.playCount + 1) / Math.log(maxPlayCount + 1);

            // Seed top artists
            int SEED_ARTISTS = Math.min(5, sortedArtists.size());
            List<ArtistNode> seedArtists = sortedArtists.subList(0, SEED_ARTISTS);

            // Expand related artists
            Map<String, ArtistNode> candidateArtists = new HashMap<>();
            for (ArtistNode seed : seedArtists) {
                candidateArtists.put(seed.artistURI, seed);
                if (seed.relatedArtists != null && seed.relatedArtists.getItems() != null) {
                    for (ArtistRelatedArtistsResponse.RelatedArtist related : seed.relatedArtists.getItems()) {
                        ArtistNode relatedNode = convertRelatedToArtistNode(related);
                        candidateArtists.putIfAbsent(relatedNode.artistURI, relatedNode);
                        relatedNode.relatedHits += 1;
                    }
                }
            }

            // Collect candidate albums & tracks
            List<TrackNode> candidateTracks = new ArrayList<>();
            List<AlbumNode> candidateAlbums = new ArrayList<>();
            for (ArtistNode artist : candidateArtists.values()) {
                // Add albums
                candidateAlbums.addAll(artist.albums.values());
                // Add tracks
                for (AlbumNode album : artist.albums.values()) {
                    candidateTracks.addAll(album.tracks.values());
                }
            }

            // Score tracks
            for (TrackNode track : candidateTracks) {
                ArtistNode artist = track.albumNode.artistNode;
                double userWeight = artist.userWeight;
                double relatedScore = artist.relatedHits >= 2 ? 1.0 :
                        artist.relatedHits == 1 ? 0.6 : 0.0;
                track.score = 0.7 * userWeight + 0.3 * relatedScore;
            }

            // Score albums (aggregate tracks)
            for (AlbumNode album : candidateAlbums) {
                double trackScoreSum = album.tracks.values().stream().mapToDouble(t -> t.score).sum();
                album.playCount = (int) trackScoreSum; // use for sorting
            }

            // Score artists already have userWeight

            // Collect recommendation items
            List<RecommendationItem> recommendations = new ArrayList<>();

            // Top artists
            sortedArtists.stream().limit(5)
                    .forEach(a -> recommendations.add(RecommendationItem.fromArtist(a)));

            // Top albums
            candidateAlbums.stream()
                    .sorted(Comparator.comparingInt(a -> -a.playCount))
                    .limit(5)
                    .forEach(a -> recommendations.add(RecommendationItem.fromAlbum(a)));

            // Top tracks
            candidateTracks.stream()
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .limit(10)
                    .forEach(t -> recommendations.add(RecommendationItem.fromTrack(t)));

            return recommendations;
        }

        private static List<TrackNode> fetchTopTracksFromMercury(String artistURI) throws IOException, TokenProvider.TokenException {
            Metadata.Artist artist = PublicValues.session.api().getMetadata4Artist(ArtistId.fromUri(artistURI));

            List<TrackNode> tracks = new ArrayList<>();

            for (Metadata.TopTracks topTrack : artist.getTopTrackList()) {
                for (Metadata.Track track : topTrack.getTrackList()) {
                    TrackNode t = new TrackNode();
                    t.trackURI = TrackId.fromHex(Utils.bytesToHex(track.getGid().toByteArray())).toSpotifyUri();
                    tracks.add(t);
                }
            }

            return tracks;
        }

        private static ArtistNode convertRelatedToArtistNode(ArtistRelatedArtistsResponse.RelatedArtist relatedArtist) {
            ArtistNode artist = new ArtistNode();
            artist.artistURI = relatedArtist.getUri();
            artist.name = relatedArtist.getProfile() != null ? relatedArtist.getProfile().getName() : null;
            return artist;
        }
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

        if (!hotListPlaylistCache.isEmpty()) {
            hotListPlaylistCache.clear();
            hotListSongListCache.clear();
            hotListSongsTable.addModifyAction(() -> ((DefaultTableModel) hotListSongsTable.getModel()).setRowCount(0));
            hotListPlaylistsTable.addModifyAction(() -> ((DefaultTableModel) hotListPlaylistsTable.getModel()).setRowCount(0));
        }
    }
}

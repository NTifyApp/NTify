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
package com.spotifyxp.panels;

import com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotify.metadata.Metadata;
import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.swingextension.JImagePanel;
import com.spotifyxp.utils.*;
import xyz.gianlu.librespot.api.ApiClient;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.mercury.MercuryClient;
import xyz.gianlu.librespot.metadata.AlbumId;
import xyz.gianlu.librespot.metadata.ArtistId;
import xyz.gianlu.librespot.metadata.TrackId;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class ArtistPanel extends JScrollPane implements View {
    public static JPanel contentPanel;
    public static JTextPane artistTitle;
    public static JImagePanel artistImage;
    public static JLabel artistPopularLabel;
    public static JScrollPane artistPopularScrollPane;
    public static ContextMenu artistpopularsonglistcontextmenu;
    public static DefTable artistPopularSongList;
    public static ContextMenu artistalbumcontextmenu;
    public static DefTable artistAlbumTable;
    public static JLabel artistAlbumLabel;
    public static JScrollPane artistAlbumScrollPane;
    public static JButton backButton;
    public static JLabel relatedArtistsLabel;
    public static JScrollPane relatedArtistsScrollPane;
    public static DefTable relatedArtistsTable;
    public static JLabel discoveredOnLabel;
    public static JScrollPane discoveredOnScrollPane;
    public static DefTable discoveredOnTable;

    public static boolean isLastArtist = false;
    public static ArrayList<String> albumUriCache = new ArrayList<>();
    public static ArrayList<String> popularUriCache = new ArrayList<>();
    public static ArrayList<Runnable> runWhenOpeningArtistPanel = new ArrayList<>();
    public static ArrayList<String> relatedArtistsUriCache = new ArrayList<>();
    public static ArrayList<String> discoveredOnUriCache = new ArrayList<>();

    public ArtistPanel() {
        contentPanel = new JPanel();
        contentPanel.setLayout(null);
        contentPanel.setPreferredSize(new Dimension(getWidth(), 1753));

        getVerticalScrollBar().setUnitIncrement(20);
        setViewportView(contentPanel);
        setVisible(false);

        artistImage = new JImagePanel();
        artistImage.setBounds(307, 12, 155, 153);
        contentPanel.add(artistImage);

        artistTitle = new JTextPane();
        artistTitle.setEditable(false);
        artistTitle.setBounds(0, 197, 775, 64);
        artistTitle.setForeground(PublicValues.globalFontColor);
        StyledDocument documentStyle = artistTitle.getStyledDocument();
        SimpleAttributeSet centerAttribute = new SimpleAttributeSet();
        StyleConstants.setAlignment(centerAttribute, StyleConstants.ALIGN_CENTER);
        documentStyle.setParagraphAttributes(0, documentStyle.getLength(), centerAttribute, false);
        contentPanel.add(artistTitle);

        backButton = new JButton(PublicValues.language.translate("ui.back"));
        backButton.setBounds(0, 0, 100, 27);
        backButton.setForeground(PublicValues.globalFontColor);
        backButton.addActionListener(new AsyncActionListener(e -> ContentPanel.switchView(ContentPanel.lastView)));
        contentPanel.add(backButton);

        artistPopularLabel = new JLabel(PublicValues.language.translate("ui.artist.popular"));
        artistPopularLabel.setBounds(5, 291, 137, 27);
        artistPopularLabel.setForeground(PublicValues.globalFontColor);
        contentPanel.add(artistPopularLabel);

        artistPopularSongList = new DefTable();
        artistPopularSongList.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.search.songlist.songname"), PublicValues.language.translate("ui.search.songlist.filesize"), PublicValues.language.translate("ui.search.songlist.bitrate"), PublicValues.language.translate("ui.search.songlist.length")
                }
        ));
        artistPopularSongList.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    InstanceManager.getPlayer().getPlayer().load(popularUriCache.get(artistPopularSongList.getSelectedRow()), true, PublicValues.shuffle);
                    TrackUtils.addAllToQueue(popularUriCache, artistPopularSongList);
                }
            }
        }));
        artistPopularSongList.setForeground(PublicValues.globalFontColor);
        artistPopularSongList.getTableHeader().setForeground(PublicValues.globalFontColor);

        artistPopularScrollPane = new JScrollPane();
        artistPopularScrollPane.setBounds(5, 320, 760, 277);
        artistPopularScrollPane.setViewportView(artistPopularSongList);
        contentPanel.add(artistPopularScrollPane);

        artistAlbumTable = new DefTable();
        artistAlbumTable.setForeground(PublicValues.globalFontColor);
        artistAlbumTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        artistAlbumTable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    isLastArtist = true;
                    setVisible(false);
                    Search.searchplaylistpanel.setVisible(true);
                    Search.searchplaylistsongscache.clear();
                    ((DefaultTableModel) Search.searchplaylisttable.getModel()).setRowCount(0);
                    try {
                        for (Metadata.Track track : SpotifyUtils.getAllTracksAlbum(albumUriCache.get(artistAlbumTable.getSelectedRow()))) {
                            artistAlbumTable.addModifyAction(() -> {
                                ((DefaultTableModel) Search.searchplaylisttable.getModel()).addRow(new Object[]{track.getName(), TrackUtils.calculateFileSizeKb(track.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDuration())});
                                Search.searchplaylistsongscache.add(TrackId.fromHex(Utils.bytesToHex(track.getGid())).toSpotifyUri());
                            });
                        }
                    } catch (IOException | TokenProvider.TokenException ex) {
                        GraphicalMessage.openException(ex);
                        ConsoleLogging.Throwable(ex);
                    }
                }
            }
        }));
        artistAlbumTable.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.search.songlist.songname")
                }
        ));

        artistAlbumScrollPane = new JScrollPane();
        artistAlbumScrollPane.setBounds(5, 667, 760, 295);
        artistAlbumScrollPane.setViewportView(artistAlbumTable);
        contentPanel.add(artistAlbumScrollPane);

        artistAlbumLabel = new JLabel(PublicValues.language.translate("ui.artist.albums"));
        artistAlbumLabel.setBounds(5, 642, 102, 14);
        artistAlbumLabel.setForeground(PublicValues.globalFontColor);
        contentPanel.add(artistAlbumLabel);

        relatedArtistsLabel = new JLabel(PublicValues.language.translate("ui.artist.relatedartists"));
        relatedArtistsLabel.setBounds(5, 1372, 102, 14);
        relatedArtistsLabel.setForeground(PublicValues.globalFontColor);
        contentPanel.add(relatedArtistsLabel);

        relatedArtistsTable = new DefTable();
        relatedArtistsTable.setForeground(PublicValues.globalFontColor);
        relatedArtistsTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        relatedArtistsTable.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.general.artist")
                }
        ));
        relatedArtistsTable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && relatedArtistsTable.getSelectedRow() != -1) {
                    try {
                        fillWith(relatedArtistsUriCache.get(relatedArtistsTable.getSelectedRow()));
                    } catch (IOException | MercuryClient.MercuryException | TokenProvider.TokenException ex) {
                        ConsoleLogging.Throwable(ex);
                    }
                }
            }
        }));

        relatedArtistsScrollPane = new JScrollPane(relatedArtistsTable);
        relatedArtistsScrollPane.setBounds(5, 1397, 760, 295);
        contentPanel.add(relatedArtistsScrollPane);

        discoveredOnLabel = new JLabel(PublicValues.language.translate("ui.artist.discoveredon"));
        discoveredOnLabel.setForeground(PublicValues.globalFontColor);
        discoveredOnLabel.setBounds(5, 1007, 102, 14);
        contentPanel.add(discoveredOnLabel);

        discoveredOnTable = new DefTable();
        discoveredOnTable.setForeground(PublicValues.globalFontColor);
        discoveredOnTable.getTableHeader().setForeground(PublicValues.globalFontColor);
        discoveredOnTable.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.general.name"), PublicValues.language.translate("ui.general.description")
                }
        ));
        discoveredOnTable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && discoveredOnTable.getSelectedRow() != -1) {
                    HomePanel.ContentTypes contentType;
                    switch (discoveredOnUriCache.get(discoveredOnTable.getSelectedRow()).split(":")[1].toLowerCase(Locale.ENGLISH)) {
                        case "album":
                            contentType = HomePanel.ContentTypes.album;
                            break;
                        case "episode":
                        case "track":
                            InstanceManager.getSpotifyPlayer().load(discoveredOnUriCache.get(discoveredOnTable.getSelectedRow()), true, PublicValues.shuffle);
                            return;
                        case "show":
                            contentType = HomePanel.ContentTypes.show;
                            break;
                        case "playlist":
                            contentType = HomePanel.ContentTypes.playlist;
                            break;
                        default:
                            ConsoleLogging.Throwable(new RuntimeException("Invalid uri in artist discovered on: " + discoveredOnUriCache.get(discoveredOnTable.getSelectedRow()).split(":")[1].toLowerCase(Locale.ENGLISH)));
                            return;
                    }
                    setVisible(false);
                    ContentPanel.trackPanel.open(discoveredOnUriCache.get(discoveredOnTable.getSelectedRow()), contentType, new Runnable() {
                        @Override
                        public void run() {
                            ContentPanel.trackPanel.makeInvisible();
                            setVisible(true);
                            ContentPanel.tabPanel.revalidate();
                            ContentPanel.tabPanel.repaint();
                        }
                    });
                }
            }
        }));

        discoveredOnScrollPane = new JScrollPane(discoveredOnTable);
        discoveredOnScrollPane.setBounds(5, 1032, 760, 295);
        contentPanel.add(discoveredOnScrollPane);

        createContextMenus();
    }

    public void openPanel() {
        for (Runnable runnable : runWhenOpeningArtistPanel) {
            runnable.run();
        }
        ContentPanel.blockTabSwitch();
        javax.swing.SwingUtilities.invokeLater(() -> getVerticalScrollBar().setValue(0));
    }

    void createContextMenus() {
        artistpopularsonglistcontextmenu = new ContextMenu(artistPopularSongList, popularUriCache, getClass());
        artistalbumcontextmenu = new ContextMenu(artistAlbumTable, albumUriCache, getClass());
    }

    private static class ArtistResponse {
        public Metadata.Artist artist;
        public ExtendedMetadata.OnPlatformReputationTrait reputationTrait;
    }

    private ArtistResponse fetchArtist(String uri) throws IOException, TokenProvider.TokenException {
        ArtistResponse artistResponse = new ArtistResponse();

        ExtendedMetadata.BatchedExtensionResponse response = PublicValues.session.api().getExtendedMetadata(ExtendedMetadata.BatchedEntityRequest.newBuilder()
                        .addEntityRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                .setEntityUri(uri)
                                .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                        .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ARTIST_V4)
                                        .build())
                                .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                        .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ON_PLATFORM_REPUTATION_TRAIT)
                                        .build())
                                .build())
                .build());

        PublicValues.session.api().checkExtendedMetadataResponse(response);

        artistResponse.reputationTrait = ExtendedMetadata.OnPlatformReputationTrait.parseFrom(response.getExtendedMetadata(1).getExtensionData(0).getExtensionData().getValue());
        artistResponse.artist = Metadata.Artist.parseFrom(response.getExtendedMetadata(0).getExtensionData(0).getExtensionData().getValue());

        return artistResponse;
    }

    public void fillWith(String uri) throws IOException, TokenProvider.TokenException, MercuryClient.MercuryException {
        reset();

        ArtistResponse artistResponse = fetchArtist(uri);

        Metadata.Artist artist = artistResponse.artist;

        if (artist.getPortraitGroup().getImageCount() != 0) {
            artistImage.setImage(new URL("https://i.scdn.co/image/" + Utils.bytesToHex(SpotifyUtils.getImageForSystem(artist.getPortraitGroup().getImageList()).getFileId()).toLowerCase(Locale.ENGLISH)));
        }

        Style style = artistTitle.addStyle("", null);
        StyleConstants.setBold(style, true);
        StyleConstants.setFontSize(style, 18);

        try {
            artistTitle.getStyledDocument().insertString(
                    artistTitle.getStyledDocument().getLength(),
                    artist.getName() + "\n",
                    style);


            artistTitle.getStyledDocument().insertString(artistTitle.getStyledDocument().getLength(), SpotifyUtils.formatMonthlyListeners(artistResponse.reputationTrait.getMonthlyListeners()) + " " + PublicValues.language.translate("ui.artist.monthlylisteners"), null);
        } catch (BadLocationException e) {
            ConsoleLogging.Throwable(e);
        }

        String artistUri = ArtistId.fromHex(Utils.bytesToHex(artist.getGid())).toSpotifyUri();

        UnofficialSpotifyAPI.ArtistUnionRelatedArtists relatedArtists = UnofficialSpotifyAPI.getArtistRelatedArtists(artistUri);
        for(UnofficialSpotifyAPI.ArtistUnionRelatedArtistsArtist relatedArtist : relatedArtists.items) {
            relatedArtistsUriCache.add(relatedArtist.uri);
            relatedArtistsTable.addModifyAction(new Runnable() {
                @Override
                public void run() {
                    ((DefaultTableModel) relatedArtistsTable.getModel()).addRow(new Object[]{
                            relatedArtist.profile.name
                    });
                }
            });
        }

        UnofficialSpotifyAPI.ArtistUnionDiscoveredOn discoveredOn = UnofficialSpotifyAPI.getArtistDiscoveredOn(artistUri);
        for(UnofficialSpotifyAPI.ArtistUnionDiscoveredOnItem discoveredOnItem : discoveredOn.items) {
            if(discoveredOnItem.data.__typename.toLowerCase(Locale.ENGLISH).contains("error")) continue;
            discoveredOnUriCache.add(discoveredOnItem.data.uri);
            discoveredOnTable.addModifyAction(new Runnable() {
                @Override
                public void run() {
                    ((DefaultTableModel) discoveredOnTable.getModel()).addRow(new Object[]{
                            discoveredOnItem.data.name,
                            discoveredOnItem.data.description
                    });
                }
            });
        }

        ApiClient.BatchedRequestHelper batchedRequestHelper = new ApiClient.BatchedRequestHelper();

        for (Metadata.TopTracks topTracks : artist.getTopTrackList()) {
            for(Metadata.Track trackEntry : topTracks.getTrackList()) {
                batchedRequestHelper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                        .setEntityUri(TrackId.fromHex(Utils.bytesToHex(trackEntry.getGid())).toSpotifyUri())
                        .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.TRACK_V4)
                                .build())
                        .build(), (data) -> {
                    Metadata.Track track = Metadata.Track.parseFrom(data[0].getValue());
                    artistPopularSongList.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            ((DefaultTableModel) artistPopularSongList.getModel()).addRow(new Object[]{
                                    track.getName(),
                                    TrackUtils.calculateFileSizeKb(track.getDuration()),
                                    TrackUtils.getBitrate(),
                                    TrackUtils.getHHMMSSOfTrack(track.getDuration())
                            });
                        }
                    });

                    popularUriCache.add(TrackId.fromHex(Utils.bytesToHex(track.getGid())).toSpotifyUri());
                });
            }
        }

        for(Metadata.AlbumGroup albumGroup : artist.getAlbumGroupList()) {
            for (Metadata.Album albumEntry : albumGroup.getAlbumList()) {
                batchedRequestHelper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                        .setEntityUri(AlbumId.fromHex(Utils.bytesToHex(albumEntry.getGid())).toSpotifyUri())
                        .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ALBUM_V4)
                                .build())
                        .build(), (data) -> {
                    Metadata.Album album = Metadata.Album.parseFrom(data[0].getValue());
                    artistAlbumTable.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            ((DefaultTableModel) artistAlbumTable.getModel()).addRow(new Object[]{
                                    album.getName()
                            });
                        }
                    });

                    albumUriCache.add(AlbumId.fromHex(Utils.bytesToHex(album.getGid())).toSpotifyUri());
                });
            }
        }

        for (Metadata.AlbumGroup albumGroup : artist.getSingleGroupList()) {
            for (Metadata.Album albumEntry : albumGroup.getAlbumList()) {
                batchedRequestHelper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                        .setEntityUri(AlbumId.fromHex(Utils.bytesToHex(albumEntry.getGid())).toSpotifyUri())
                        .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.ALBUM_V4)
                                .build())
                        .build(), (data) -> {
                    Metadata.Album album = Metadata.Album.parseFrom(data[0].getValue());
                    artistAlbumTable.addModifyAction(new Runnable() {
                        @Override
                        public void run() {
                            ((DefaultTableModel) artistAlbumTable.getModel()).addRow(new Object[]{
                                    album.getName()
                            });
                        }
                    });

                    albumUriCache.add(AlbumId.fromHex(Utils.bytesToHex(album.getGid())).toSpotifyUri());
                });
            }
        }

        batchedRequestHelper.execute(PublicValues.session.api(), null);
    }

    public void reset() {
        popularUriCache.clear();
        albumUriCache.clear();
        relatedArtistsUriCache.clear();
        discoveredOnUriCache.clear();
        ((DefaultTableModel) discoveredOnTable.getModel()).setRowCount(0);
        ((DefaultTableModel) relatedArtistsTable.getModel()).setRowCount(0);
        ((DefaultTableModel) artistAlbumTable.getModel()).setRowCount(0);
        ((DefaultTableModel) artistPopularSongList.getModel()).setRowCount(0);
        getVerticalScrollBar().setValue(0);
        artistTitle.setText("");
    }

    @Override
    public void makeVisible() {
        setVisible(true);
        openPanel();
    }

    @Override
    public void makeInvisible() {
        setVisible(false);
        ContentPanel.enableTabSwitch();
    }
}

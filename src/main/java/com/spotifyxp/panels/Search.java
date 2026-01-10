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

import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotifyxp.deps.com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.com.spotify.playlist4.Playlist4ApiProto;
import com.spotifyxp.deps.xyz.gianlu.librespot.api.ApiClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.common.Utils;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.deps.xyz.gianlu.librespot.metadata.*;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.AsyncActionListener;
import com.spotifyxp.utils.AsyncMouseListener;
import com.spotifyxp.utils.TrackUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Search extends JPanel implements View {
    public static JPanel searchplaylistpanel;
    public static JButton searchbackbutton;
    public static DefTable searchplaylisttable;
    public static JRadioButton searchfilterplaylist;
    public static JRadioButton searchfilteralbum;
    public static JRadioButton searchfiltershow;
    public static JRadioButton searchfiltertrack;
    public static JScrollPane searchplaylistscrollpanel;
    public static JRadioButton searchfilterartist;
    public static DefTable searchsonglist;
    public static JTextField searchartistfield;
    public static JTextField searchsongtitlefield;
    public static JPanel searchfieldspanel;
    public static JLabel searchartistlabel;
    public static JLabel searchsongtitlelabel;
    public static JButton searchclearfieldsbutton;
    public static JButton searchfinditbutton;
    public static JPanel searchfilterpanel;
    public static JRadioButton searchfilterexcludeexplicit;
    public static JScrollPane searchscrollpanel;
    public static final ArrayList<String> searchsonglistcache = new ArrayList<>();
    public static final ArrayList<String> searchplaylistsongscache = new ArrayList<>();
    public static ContextMenu searchplaylistsongscontextmenu;
    public static ContextMenu searchcontextmenu;
    public static JPanel backButtonContainer;
    public static JTextPane playlistDescription;
    public static JScrollPane playlistDescriptionScrollPane;
    private String searchCacheTitle = "";
    private String searchCacheArtist = "";
    private boolean excludeExplicit = false;
    private boolean[] inProg = {false};
    private boolean loadnew = false;
    private Runnable lazyLoadingDeInit;

    public Search() {
        setVisible(false);
        setLayout(new BorderLayout());
        searchfieldspanel = new JPanel();
        searchfieldspanel.setBorder(new TitledBorder(null, PublicValues.language.translate("ui.search.searchfield.border"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        searchfieldspanel.setPreferredSize(new Dimension(getWidth(), 128));
        add(searchfieldspanel, BorderLayout.NORTH);
        searchfieldspanel.setLayout(null);
        searchfieldspanel.setForeground(PublicValues.globalFontColor);
        searchartistlabel = new JLabel(PublicValues.language.translate("ui.search.searchfield.artist"));
        searchartistlabel.setHorizontalAlignment(SwingConstants.RIGHT);
        searchartistlabel.setBounds(5, 25, 101, searchartistlabel.getFontMetrics(searchartistlabel.getFont()).getHeight());
        searchfieldspanel.add(searchartistlabel);
        searchartistlabel.setForeground(PublicValues.globalFontColor);
        searchsongtitlelabel = new JLabel(PublicValues.language.translate("ui.search.searchfield.title"));
        searchsongtitlelabel.setHorizontalAlignment(SwingConstants.RIGHT);
        searchsongtitlelabel.setBounds(10, 62, 66, searchsongtitlelabel.getFontMetrics(searchsongtitlelabel.getFont()).getHeight());
        searchfieldspanel.add(searchsongtitlelabel);
        searchsongtitlelabel.setForeground(PublicValues.globalFontColor);
        searchclearfieldsbutton = new JButton(PublicValues.language.translate("ui.search.searchfield.button.clear"));
        searchclearfieldsbutton.setBounds(30, 94, 194, 23);
        searchfieldspanel.add(searchclearfieldsbutton);
        searchclearfieldsbutton.setForeground(PublicValues.globalFontColor);
        searchclearfieldsbutton.addActionListener(e -> {
            searchartistfield.setText("");
            searchsongtitlefield.setText("");
        });
        searchfinditbutton = new JButton(PublicValues.language.translate("ui.search.searchfield.button.findit"));
        searchfinditbutton.setBounds(234, 94, 194, 23);
        searchfieldspanel.add(searchfinditbutton);
        searchfinditbutton.setForeground(PublicValues.globalFontColor);
        searchartistfield = new JTextField();
        searchartistfield.setBounds(116, 22, 326, 25);
        searchfieldspanel.add(searchartistfield);
        searchartistfield.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchfinditbutton.doClick();
                }
            }
        });
        searchsongtitlefield = new JTextField();
        searchsongtitlefield.setBounds(86, 59, 356, 25);
        searchfieldspanel.add(searchsongtitlefield);
        searchsongtitlefield.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    searchfinditbutton.doClick();
                }
            }
        });
        searchfilterpanel = new JPanel();
        searchfilterpanel.setLayout(null);
        searchfilterpanel.setBorder(new TitledBorder(null, PublicValues.language.translate("ui.search.searchfield.filters.border"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        searchfilterpanel.setBounds(452, 11, 322, 106);
        searchfieldspanel.add(searchfilterpanel);
        searchfilterexcludeexplicit = new JRadioButton(PublicValues.language.translate("ui.search.searchfield.filters.excludeexplicit"));
        searchfilterexcludeexplicit.setBounds(6, 24, 130, 23);
        searchfilterexcludeexplicit.setEnabled(false); //ToDo: Reverse engineer explicit filtering
        searchfilterpanel.add(searchfilterexcludeexplicit);
        searchfilterexcludeexplicit.setForeground(PublicValues.globalFontColor);
        searchfilterartist = new JRadioButton(PublicValues.language.translate("ui.search.filter.artist"));
        searchfilterartist.setBounds(160, 23, 130, 23);
        searchfilterpanel.add(searchfilterartist);
        searchfilterartist.setForeground(PublicValues.globalFontColor);
        searchfiltertrack = new JRadioButton(PublicValues.language.translate("ui.search.filter.track"));
        searchfiltertrack.setBounds(6, 50, 130, 23);
        searchfilterpanel.add(searchfiltertrack);
        searchfiltertrack.setForeground(PublicValues.globalFontColor);
        searchfiltertrack.setSelected(true);
        searchfilteralbum = new JRadioButton(PublicValues.language.translate("ui.search.filter.album"));
        searchfilteralbum.setBounds(160, 50, 130, 23);
        searchfilterpanel.add(searchfilteralbum);
        searchfilteralbum.setForeground(PublicValues.globalFontColor);
        searchfilterplaylist = new JRadioButton(PublicValues.language.translate("ui.search.filter.playlist"));
        searchfilterplaylist.setBounds(6, 75, 130, 23);
        searchfilterpanel.add(searchfilterplaylist);
        searchfilterplaylist.setForeground(PublicValues.globalFontColor);
        searchfiltershow = new JRadioButton(PublicValues.language.translate("ui.search.filter.show"));
        searchfiltershow.setBounds(160, 75, 130, 23);
        searchfilterpanel.add(searchfiltershow);
        searchfiltershow.setForeground(PublicValues.globalFontColor);
        searchfilterartist.addActionListener(e -> {
            searchfiltertrack.setSelected(false);
            searchfilteralbum.setSelected(false);
            searchfiltershow.setSelected(false);
            searchfilterplaylist.setSelected(false);
        });
        searchfilteralbum.addActionListener(e -> {
            searchfiltertrack.setSelected(false);
            searchfiltershow.setSelected(false);
            searchfilterplaylist.setSelected(false);
            searchfilterartist.setSelected(false);
        });
        searchfilterplaylist.addActionListener(e -> {
            searchfiltertrack.setSelected(false);
            searchfilteralbum.setSelected(false);
            searchfiltershow.setSelected(false);
            searchfilterartist.setSelected(false);
        });
        searchfiltershow.addActionListener(e -> {
            searchfiltertrack.setSelected(false);
            searchfilteralbum.setSelected(false);
            searchfilterplaylist.setSelected(false);
            searchfilterartist.setSelected(false);
        });
        searchfiltertrack.addActionListener(e -> {
            searchfilteralbum.setSelected(false);
            searchfiltershow.setSelected(false);
            searchfilterplaylist.setSelected(false);
            searchfilterartist.setSelected(false);
        });
        searchfinditbutton.addActionListener(new AsyncActionListener(e -> {
            Thread thread1 = new Thread(() -> {
                String searchartist = searchartistfield.getText();
                String searchtitle = searchsongtitlefield.getText();
                boolean track = searchfiltertrack.isSelected();
                boolean artist = searchfilterartist.isSelected();
                boolean album = searchfilteralbum.isSelected();
                boolean show = searchfiltershow.isSelected();
                boolean playlist = searchfilterplaylist.isSelected();
                if (!track & !artist & !album & !show & !playlist) {
                    //No search filter was selected! Selecting tracks
                    searchfiltertrack.setSelected(true);
                    track = true;
                }
                searchCacheTitle = searchtitle;
                searchCacheArtist = searchartist;
                excludeExplicit = searchfilterexcludeexplicit.isSelected();
                searchsonglistcache.clear();
                ((DefaultTableModel) searchsonglist.getModel()).setRowCount(0);
                if (searchtitle.isEmpty() && searchartist.isEmpty()) {
                    return; // User didn't type anything in so we just return
                }else if(searchtitle.isEmpty()) {
                    return;
                }
                try {

                    if (track) {
                        UnofficialSpotifyAPI.SearchV2Response response = UnofficialSpotifyAPI.search(searchtitle + " " + searchartist, 0, 50, 1, false, false, false, false);
                        for (UnofficialSpotifyAPI.SearchV2Response.TracksV2Item item : response.data.searchV2.tracksV2.items) {
                            UnofficialSpotifyAPI.SearchV2Response.TrackData t = item.item.data;
                            List<String> artists = new ArrayList<>();
                            for(UnofficialSpotifyAPI.SearchV2Response.ArtistItem artistItem : t.artists.items)
                               artists.add(artistItem.profile.name);
                            if (!searchartist.equalsIgnoreCase("")) {
                                if (!TrackUtils.trackHasArtist(artists.toArray(new String[0]), searchartist, true)) {
                                    continue;
                                }
                            }
                            //if (excludeExplicit) {
                            //    if (!t.getIsExplicit()) {
                            //        searchsonglistcache.add(t.getUri());
                            //        InstanceManager.getSpotifyAPI().addSongToList(artists, t, searchsonglist);
                            //    }
                            //} else {
                            searchsonglistcache.add(t.uri);
                            searchsonglist.addModifyAction(() -> ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{t.name + " - " + t.albumOfTrack.name + " - " + String.join(", ", artists), TrackUtils.calculateFileSizeKb(t.duration.totalMilliseconds), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(t.duration.totalMilliseconds)}));
                            //}
                        }
                    }
                    if (artist) {
                        UnofficialSpotifyAPI.SearchV2Response response = UnofficialSpotifyAPI.search(searchtitle, 0, 50, 1, false, false, false, false);
                        for (UnofficialSpotifyAPI.SearchV2Response.ArtistsItemData artistItem : response.data.searchV2.artists.items) {
                            searchsonglistcache.add(artistItem.data.uri);
                            searchsonglist.addModifyAction(() -> ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{artistItem.data.profile.name}));
                        }
                    }
                    if (album) {
                        UnofficialSpotifyAPI.SearchV2Response response = UnofficialSpotifyAPI.search(searchtitle, 0, 50, 1, false, false, false, false);
                        for (UnofficialSpotifyAPI.SearchV2Response.AlbumResponseWrapper albumWrapper : response.data.searchV2.albumsV2.items) {
                            if (!searchartist.isEmpty()) {
                                String[] artists = new String[albumWrapper.data.artists.items.size()];
                                for (int i = 0; i < albumWrapper.data.artists.items.size(); i++) {
                                    artists[i] = albumWrapper.data.artists.items.get(i).profile.name;
                                }
                                if (!TrackUtils.trackHasArtist(artists, searchartist, true)) {
                                    continue;
                                }
                            }
                            searchsonglistcache.add(albumWrapper.data.uri);
                            ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{albumWrapper.data.name});
                        }
                    }
                    if (show) {
                        UnofficialSpotifyAPI.SearchV2Response response = UnofficialSpotifyAPI.search(searchtitle, 0, 50, 1, false, false, false, false);
                        for (UnofficialSpotifyAPI.SearchV2Response.PodcastResponseWrapper podcast : response.data.searchV2.podcasts.items) {
                            if (!searchartist.isEmpty()) {
                                if (!podcast.data.publisher.name.equalsIgnoreCase(searchartist)) {
                                    continue;
                                }
                            }
                            searchsonglistcache.add(podcast.data.uri);
                            ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{podcast.data.name});
                        }
                    }
                    if (playlist) {
                        UnofficialSpotifyAPI.SearchV2Response response = UnofficialSpotifyAPI.search(searchtitle, 0, 50, 1, false, false, false, false);
                        for (UnofficialSpotifyAPI.SearchV2Response.PlaylistResponseWrapper playlistWrapper : response.data.searchV2.playlists.items) {
                            if (Objects.equals(playlistWrapper.data.typename, "NotFound")) continue;
                            if (!searchartist.isEmpty()) {
                                if (!playlistWrapper.data.getOwnerV2().get().data.username.equalsIgnoreCase(searchartist)) {
                                    continue;
                                }
                            }
                            searchsonglistcache.add(playlistWrapper.data.getUri().get());
                            searchsonglist.addModifyAction(() -> ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{playlistWrapper.data.getName().get() + " - " + playlistWrapper.data.getOwnerV2().get().data.username}));
                        }
                    }
                } catch (IOException | MercuryClient.MercuryException ex) {
                    ConsoleLogging.Throwable(ex);
                }
                //ToDo: Re-implement load more
                //searchsonglist.addModifyAction(() -> ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{PublicValues.language.translate("ui.general.loadmore"), PublicValues.language.translate("ui.general.loadmore"), PublicValues.language.translate("ui.general.loadmore"), PublicValues.language.translate("ui.general.loadmore")}));
            }, "Search thread");
            thread1.start();
        }));
        searchscrollpanel = new JScrollPane();
        add(searchscrollpanel, BorderLayout.CENTER);
        searchsonglist = new DefTable();
        searchsonglist.getTableHeader().setReorderingAllowed(false);
        searchsonglist.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    /*try {
                        if (searchsonglist.getModel().getValueAt(searchsonglist.getSelectedRow(), 2).toString().equals(PublicValues.language.translate("ui.general.loadmore"))) {
                            ((DefaultTableModel) searchsonglist.getModel()).setRowCount(searchsonglist.getRowCount() - 1);
                            Thread thread1 = new Thread(() -> {
                                String searchartist = searchCacheArtist;
                                String searchtitle = searchCacheTitle;
                                boolean track = searchsonglistcache.get(0).split(":")[1].equals("track");
                                boolean artist = searchsonglistcache.get(0).split(":")[1].equals("artist");
                                boolean album = searchsonglistcache.get(0).split(":")[1].equals("album");
                                boolean show = searchsonglistcache.get(0).split(":")[1].equals("show");
                                boolean playlist = searchsonglistcache.get(0).split(":")[1].equals("playlist");
                                try {
                                    if (track) {
                                        for (Track t : InstanceManager.getSpotifyApi().searchTracks(searchtitle + " " + searchartist).limit(50).offset(searchsonglistcache.size()).build().execute().getItems()) {
                                            String artists = TrackUtils.getArtists(t.getArtists());
                                            if (!searchartist.equalsIgnoreCase("")) {
                                                if (!TrackUtils.trackHasArtist(t.getArtists(), searchartist, true)) {
                                                    continue;
                                                }
                                            }
                                            if (excludeExplicit) {
                                                if (!t.getIsExplicit()) {
                                                    searchsonglistcache.add(t.getUri());
                                                    //InstanceManager.getSpotifyAPI().addSongToList(artists, t, searchsonglist);
                                                }
                                            } else {
                                                searchsonglistcache.add(t.getUri());
                                                //InstanceManager.getSpotifyAPI().addSongToList(artists, t, searchsonglist);
                                            }
                                        }
                                    }
                                    if (artist) {
                                        if (searchtitle.isEmpty()) {
                                            searchtitle = searchartist;
                                        }
                                        for (Artist a : InstanceManager.getSpotifyApi().searchArtists(searchtitle).offset(searchsonglistcache.size()).build().execute().getItems()) {
                                            searchsonglistcache.add(a.getUri());
                                            InstanceManager.getSpotifyAPI().addArtistToList(a, searchsonglist);
                                        }
                                    }
                                    if (album) {
                                        for (AlbumSimplified a : InstanceManager.getSpotifyApi().searchAlbums(searchtitle).offset(searchsonglistcache.size()).build().execute().getItems()) {
                                            if (!searchartist.isEmpty()) {
                                                if (!TrackUtils.trackHasArtist(a.getArtists(), searchartist, true)) {
                                                    continue;
                                                }
                                            }
                                            searchsonglistcache.add(a.getUri());
                                            ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{a.getName()});
                                        }
                                    }
                                    if (show) {
                                        for (ShowSimplified s : InstanceManager.getSpotifyApi().searchShows(searchtitle).offset(searchsonglistcache.size()).build().execute().getItems()) {
                                            if (!searchartist.isEmpty()) {
                                                if (!s.getPublisher().equalsIgnoreCase(searchartist)) {
                                                    continue;
                                                }
                                            }
                                            searchsonglistcache.add(s.getUri());
                                            ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{s.getName()});
                                        }
                                    }
                                    if (playlist) {
                                        for (PlaylistSimplified t : InstanceManager.getSpotifyApi().searchPlaylists(searchtitle).offset(searchsonglistcache.size()).build().execute().getItems()) {
                                            if (!searchartist.isEmpty()) {
                                                if (!t.getOwner().getDisplayName().equalsIgnoreCase(searchartist)) {
                                                    continue;
                                                }
                                            }
                                            searchsonglistcache.add(t.getUri());
                                            InstanceManager.getSpotifyAPI().addPlaylistToList(t, searchsonglist);
                                        }
                                    }
                                } catch (IOException ex) {
                                    ConsoleLogging.Throwable(ex);
                                }
                                searchsonglist.addModifyAction(() -> ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{PublicValues.language.translate("ui.general.loadmore"), PublicValues.language.translate("ui.general.loadmore"), PublicValues.language.translate("ui.general.loadmore"), PublicValues.language.translate("ui.general.loadmore")}));
                            });
                            thread1.start();
                            return;
                        }
                    } catch (NullPointerException ignored) {
                    }*/
                    switch (searchsonglistcache.get(searchsonglist.getSelectedRow()).split(":")[1]) {
                        case "playlist":
                        case "album":
                        case "show":
                            setVisible(false);
                            ContentPanel.blockTabSwitch();
                            searchplaylistpanel.setVisible(true);
                            searchplaylistsongscache.clear();
                            ((DefaultTableModel) searchplaylisttable.getModel()).setRowCount(0);
                            playlistDescription.setText("");
                            break;
                        case "artist":
                            ContentPanel.artistPanel.reset();
                            ContentPanel.switchView(Views.ARTIST);
                            ContentPanel.blockTabSwitch();
                            break;
                    }
                    Thread thread = new Thread(() -> {
                        switch (searchsonglistcache.get(searchsonglist.getSelectedRow()).split(":")[1].toLowerCase()) {
                            case "playlist":
                                Thread playlistloadthread = new Thread(() -> {
                                    searchplaylistsongscache.clear();
                                    ((DefaultTableModel) searchplaylisttable.getModel()).setRowCount(0);
                                    try {
                                        Playlist4ApiProto.SelectedListContent playlist = PublicValues.session.api().playlist().get(PlaylistId.fromUri(searchsonglistcache.get(searchsonglist.getSelectedRow())));
                                        ApiClient.BatchedRequestHelper helper = new ApiClient.BatchedRequestHelper();
                                        playlistDescription.setText(playlist.getAttributes().getDescription());
                                        playlistDescriptionScrollPane.setVisible(!playlistDescription.getText().isEmpty());
                                        backButtonContainer.revalidate();
                                        backButtonContainer.repaint();
                                        for (Playlist4ApiProto.Item item : playlist.getContents().getItemsList()) 
                                            helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                                            .setEntityUri(item.getUri())
                                                            .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                                                    .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.TRACK_V4)
                                                                    .build())
                                                    .build(), data -> {
                                                Metadata.Track track = Metadata.Track.parseFrom(data[0].getValue());
                                                ((DefaultTableModel) searchplaylisttable.getModel()).addRow(new Object[]{track.getName(), TrackUtils.calculateFileSizeKb(track.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDuration())});
                                                searchplaylistsongscache.add(item.getUri());
                                            });
                                        helper.execute(PublicValues.session.api(), (exception, response) -> ConsoleLogging.Throwable(exception));
                                    } catch (Exception e1) {
                                        throw new RuntimeException(e1);
                                    }
                                }, "Get playlist tracks");
                                playlistloadthread.start();
                                break;
                            case "artist":
                                try {
                                    ContentPanel.artistPanel.fillWith(searchsonglistcache.get(searchsonglist.getSelectedRow()));
                                } catch (Exception e1) {
                                    throw new RuntimeException(e1);
                                }
                                break;
                            case "show":
                                try {
                                    Metadata.Show show = PublicValues.session.api().show().getMetadata(ShowId.fromUri(searchsonglistcache.get(searchsonglist.getSelectedRow())));
                                    ApiClient.BatchedRequestHelper helper = new ApiClient.BatchedRequestHelper();
                                    for (Metadata.Episode episodeItem : show.getEpisodeList()) {
                                        String episodeUri = EpisodeId.fromHex(Utils.bytesToHex(episodeItem.getGid())).toSpotifyUri();
                                        helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                                .setEntityUri(episodeUri)
                                                .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                                        .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.EPISODE_V4)
                                                        .build())
                                                .build(), data -> {
                                            Metadata.Episode episode = Metadata.Episode.parseFrom(data[0].getValue());
                                            ((DefaultTableModel) searchplaylisttable.getModel()).addRow(new Object[]{episode.getName(), TrackUtils.calculateFileSizeKb(episode.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(episode.getDuration())});
                                            searchplaylistsongscache.add(episodeUri);
                                        });
                                    }
                                    helper.execute(PublicValues.session.api(), (exception, response) -> ConsoleLogging.Throwable(exception));
                                } catch (Exception e1) {
                                    throw new RuntimeException(e1);
                                }
                                break;
                            case "album":
                                try {
                                    Metadata.Album album = PublicValues.session.api().album().getMetadata(AlbumId.fromUri(searchsonglistcache.get(searchsonglist.getSelectedRow())));
                                    ApiClient.BatchedRequestHelper helper = new ApiClient.BatchedRequestHelper();
                                    for (Metadata.Disc disc : album.getDiscList())
                                        for (Metadata.Track trackItem : disc.getTrackList()) {
                                            String trackUri = TrackId.fromHex(Utils.bytesToHex(trackItem.getGid())).toSpotifyUri();
                                            helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                                    .setEntityUri(trackUri)
                                                    .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                                            .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.TRACK_V4)
                                                            .build())
                                                    .build(), data -> {
                                                Metadata.Track track = Metadata.Track.parseFrom(data[0].getValue());
                                                ((DefaultTableModel) searchplaylisttable.getModel()).addRow(new Object[]{track.getName(), TrackUtils.calculateFileSizeKb(track.getDuration()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDuration())});
                                                searchplaylistsongscache.add(trackUri);
                                            });
                                        }
                                    helper.execute(PublicValues.session.api(), (exception, response) -> ConsoleLogging.Throwable(exception));
                                } catch (Exception e1) {
                                    throw new RuntimeException(e1);
                                }
                                break;
                            case "track":
                                InstanceManager.getPlayer().getPlayer().load(searchsonglistcache.get(searchsonglist.getSelectedRow()), true, PublicValues.shuffle);
                                break;
                            default:
                                throw new RuntimeException("Invalid uri '" + searchsonglistcache.get(searchsonglist.getSelectedRow()).split(":")[1].toLowerCase() + "'");
                        }
                    });
                    thread.start();
                    searchsonglist.setColumnSelectionInterval(0, searchsonglist.getColumnCount() - 1);
                } else {
                    searchsonglist.setColumnSelectionInterval(0, searchsonglist.getColumnCount() - 1);
                }
            }
        }));
        searchsonglist.getTableHeader().setForeground(PublicValues.globalFontColor);
        searchsonglist.setForeground(PublicValues.globalFontColor);
        searchsonglist.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("ui.search.songlist.songname"), PublicValues.language.translate("ui.search.songlist.filesize"), PublicValues.language.translate("ui.search.songlist.bitrate"), PublicValues.language.translate("ui.search.songlist.length")}));
        searchsonglist.getColumnModel().getColumn(0).setPreferredWidth(342);
        searchsonglist.getColumnModel().getColumn(1).setPreferredWidth(130);
        searchsonglist.setFillsViewportHeight(true);
        searchsonglist.setColumnSelectionAllowed(true);
        searchscrollpanel.setViewportView(searchsonglist);
        searchplaylistpanel = new JPanel();
        searchplaylistpanel.setLayout(new BorderLayout());
        playlistDescriptionScrollPane = new JScrollPane();
        playlistDescriptionScrollPane.setPreferredSize(new Dimension(-1, 40));
        playlistDescriptionScrollPane.setVisible(false);
        playlistDescription = new JTextPane();
        playlistDescription.setEditable(false);
        playlistDescription.setContentType("text/html");
        ((AbstractDocument) playlistDescription.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                string = string.replaceAll("\n", "");
                super.insertString(fb, offset, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                text = text.replaceAll("\n", "");
                super.replace(fb, offset, length, text, attrs);
            }
        });
        playlistDescriptionScrollPane.setViewportView(playlistDescription);
        ContentPanel.tabPanel.add(searchplaylistpanel);
        backButtonContainer = new JPanel();
        backButtonContainer.setLayout(new BorderLayout());
        backButtonContainer.add(playlistDescriptionScrollPane, BorderLayout.CENTER);
        searchplaylistpanel.add(backButtonContainer, BorderLayout.NORTH);
        searchbackbutton = new JButton(PublicValues.language.translate("ui.back"));
        backButtonContainer.add(searchbackbutton, BorderLayout.WEST);
        searchbackbutton.setForeground(PublicValues.globalFontColor);
        searchplaylistscrollpanel = new JScrollPane();
        searchplaylistpanel.add(searchplaylistscrollpanel, BorderLayout.CENTER);
        searchplaylisttable = new DefTable();
        searchplaylistscrollpanel.setViewportView(searchplaylisttable);
        searchplaylisttable.setForeground(PublicValues.globalFontColor);
        searchplaylisttable.getTableHeader().setForeground(PublicValues.globalFontColor);
        searchplaylisttable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    InstanceManager.getPlayer().getPlayer().load(searchplaylistsongscache.get(searchplaylisttable.getSelectedRow()), true, PublicValues.shuffle);
                    searchplaylisttable.setColumnSelectionInterval(0, searchplaylisttable.getColumnCount() - 1);
                    TrackUtils.addAllToQueue(searchplaylistsongscache, searchplaylisttable);
                }
            }
        }));
        searchplaylisttable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("ui.search.songlist.songname"), PublicValues.language.translate("ui.search.songlist.filesize"), PublicValues.language.translate("ui.search.songlist.bitrate"), PublicValues.language.translate("ui.search.songlist.length")}));
        searchplaylistpanel.setVisible(false);

        searchplaylistsongscontextmenu = new ContextMenu(searchplaylisttable, searchplaylistsongscache, getClass());
        searchbackbutton.addActionListener(new AsyncActionListener(e -> {
            searchplaylistpanel.setVisible(false);
            if(lazyLoadingDeInit != null) {
                lazyLoadingDeInit.run();
                lazyLoadingDeInit = null;
            }
            if (ContentPanel.currentView == Views.ARTIST) {
                ContentPanel.artistPanel.setVisible(true);
            } else {
                ContentPanel.searchPanel.setVisible(true);
            }
            PublicValues.contentPanel.setVisible(true);
            if (ContentPanel.currentView != Views.ARTIST) {
                ContentPanel.enableTabSwitch();
            }
        }));
        searchcontextmenu = new ContextMenu(searchsonglist, searchsonglistcache, getClass());
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

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

import com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotify.metadata.Metadata;
import com.spotify.playlist4.Playlist4ApiProto;
import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.spotapi.pojos.SearchResponse;
import com.spotifyxp.utils.AsyncUtils;
import com.spotifyxp.utils.ReentryGuard;
import com.spotifyxp.utils.TrackUtils;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.dealer.ApiClient;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.*;

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
    private final ReentryGuard searchGuard = new ReentryGuard();
    /**
     * Guards writes to searchplaylisttable. Shared across classes (see ArtistPanel's album
     * double-click handler) since that table is written from more than one panel.
     */
    public static final ReentryGuard searchplaylisttableGuard = new ReentryGuard();
    private boolean loadnew = false;
    private Runnable lazyLoadingDeInit;

    public Search() {
        setVisible(false);
        setLayout(new BorderLayout());
        searchfieldspanel = new JPanel();
        searchfieldspanel.setBorder(new TitledBorder(null, PublicValues.language.translate("search.input_fields.border_title"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        searchfieldspanel.setPreferredSize(new Dimension(getWidth(), 128));
        add(searchfieldspanel, BorderLayout.NORTH);
        searchfieldspanel.setLayout(null);
        searchfieldspanel.setForeground(PublicValues.globalFontColor);
        searchartistlabel = new JLabel(PublicValues.language.translate("search.input_fields.artist_field"));
        searchartistlabel.setHorizontalAlignment(SwingConstants.RIGHT);
        searchartistlabel.setBounds(5, 25, 101, searchartistlabel.getFontMetrics(searchartistlabel.getFont()).getHeight());
        searchfieldspanel.add(searchartistlabel);
        searchartistlabel.setForeground(PublicValues.globalFontColor);
        searchsongtitlelabel = new JLabel(PublicValues.language.translate("search.input_fields.title_field"));
        searchsongtitlelabel.setHorizontalAlignment(SwingConstants.RIGHT);
        searchsongtitlelabel.setBounds(10, 62, 66, searchsongtitlelabel.getFontMetrics(searchsongtitlelabel.getFont()).getHeight());
        searchfieldspanel.add(searchsongtitlelabel);
        searchsongtitlelabel.setForeground(PublicValues.globalFontColor);
        searchclearfieldsbutton = new JButton(PublicValues.language.translate("search.clear_button"));
        searchclearfieldsbutton.setBounds(30, 94, 194, 23);
        searchfieldspanel.add(searchclearfieldsbutton);
        searchclearfieldsbutton.setForeground(PublicValues.globalFontColor);
        searchclearfieldsbutton.addActionListener(e -> {
            searchartistfield.setText("");
            searchsongtitlefield.setText("");
        });
        searchfinditbutton = new JButton(PublicValues.language.translate("search.find_it_button"));
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
        searchfilterpanel.setBorder(new TitledBorder(null, PublicValues.language.translate("search.filters.border_title"), TitledBorder.LEADING, TitledBorder.TOP, null, null));
        searchfilterpanel.setBounds(452, 11, 322, 106);
        searchfieldspanel.add(searchfilterpanel);
        searchfilterexcludeexplicit = new JRadioButton(PublicValues.language.translate("search.filters.exclude_explicit"));
        searchfilterexcludeexplicit.setBounds(6, 24, 130, 23);
        searchfilterexcludeexplicit.setEnabled(false); //ToDo: Reverse engineer explicit filtering
        searchfilterpanel.add(searchfilterexcludeexplicit);
        searchfilterexcludeexplicit.setForeground(PublicValues.globalFontColor);
        searchfilterartist = new JRadioButton(PublicValues.language.translate("search.filters.artist"));
        searchfilterartist.setBounds(160, 23, 130, 23);
        searchfilterpanel.add(searchfilterartist);
        searchfilterartist.setForeground(PublicValues.globalFontColor);
        searchfiltertrack = new JRadioButton(PublicValues.language.translate("search.filters.track"));
        searchfiltertrack.setBounds(6, 50, 130, 23);
        searchfilterpanel.add(searchfiltertrack);
        searchfiltertrack.setForeground(PublicValues.globalFontColor);
        searchfiltertrack.setSelected(true);
        searchfilteralbum = new JRadioButton(PublicValues.language.translate("search.filters.album"));
        searchfilteralbum.setBounds(160, 50, 130, 23);
        searchfilterpanel.add(searchfilteralbum);
        searchfilteralbum.setForeground(PublicValues.globalFontColor);
        searchfilterplaylist = new JRadioButton(PublicValues.language.translate("search.filters.playlist"));
        searchfilterplaylist.setBounds(6, 75, 130, 23);
        searchfilterpanel.add(searchfilterplaylist);
        searchfilterplaylist.setForeground(PublicValues.globalFontColor);
        searchfiltershow = new JRadioButton(PublicValues.language.translate("search.filters.show"));
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
        searchfinditbutton.addActionListener(e -> {
            if (!searchGuard.tryEnter()) return;
            AsyncUtils.run(() -> {
              try {
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
                    String query = track ? searchtitle + " " + searchartist : searchtitle;
                    SearchResponse response = PublicValues.spotAPI.feed().search()
                            .setSearchTerm(query)
                            .setOffset(0)
                            .setLimit(50)
                            .setNumberOfTopResults(1)
                            .execute();

                    if (track) {
                        for (SearchResponse.TrackItem item : response.getTracks().getItems()) {
                            SearchResponse.TrackData t = item.getItem().getData();
                            List<String> artists = new ArrayList<>();
                            for (SearchResponse.ArtistCredit artistItem : t.getArtists().getItems())
                               artists.add(artistItem.getProfile().getName());
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
                            searchsonglistcache.add(t.getUri());
                            ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{t.getName() + " - " + t.getAlbumOfTrack().getName() + " - " + String.join(", ", artists), TrackUtils.calculateFileSizeKb(t.getDuration().getTotalMilliseconds()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(t.getDuration().getTotalMilliseconds())});
                            //}
                        }
                    }
                    if (artist) {
                        for (SearchResponse.ArtistWrapper artistItem : response.getArtists().getItems()) {
                            searchsonglistcache.add(artistItem.getData().getUri());
                            ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{artistItem.getData().getProfile().getName()});
                        }
                    }
                    if (album) {
                        for (SearchResponse.AlbumWrapper albumWrapper : response.getAlbums().getItems()) {
                            if (!searchartist.isEmpty()) {
                                List<SearchResponse.ArtistCredit> albumArtists = albumWrapper.getData().getArtists().getItems();
                                String[] artists = new String[albumArtists.size()];
                                for (int i = 0; i < albumArtists.size(); i++) {
                                    artists[i] = albumArtists.get(i).getProfile().getName();
                                }
                                if (!TrackUtils.trackHasArtist(artists, searchartist, true)) {
                                    continue;
                                }
                            }
                            searchsonglistcache.add(albumWrapper.getData().getUri());
                            ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{albumWrapper.getData().getName()});
                        }
                    }
                    if (show) {
                        for (SearchResponse.PodcastWrapper podcast : response.getPodcasts().getItems()) {
                            if (!searchartist.isEmpty()) {
                                if (!podcast.getData().getPublisher().getName().equalsIgnoreCase(searchartist)) {
                                    continue;
                                }
                            }
                            searchsonglistcache.add(podcast.getData().getUri());
                            ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{podcast.getData().getName()});
                        }
                    }
                    if (playlist) {
                        for (SearchResponse.PlaylistWrapper playlistWrapper : response.getPlaylists().getItems()) {
                            if (Objects.equals(playlistWrapper.getTypename(), "NotFound")) continue;
                            if (!searchartist.isEmpty()) {
                                if (!playlistWrapper.getData().getOwnerV2().getData().getUsername().equalsIgnoreCase(searchartist)) {
                                    continue;
                                }
                            }
                            searchsonglistcache.add(playlistWrapper.getData().getUri());
                            ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{playlistWrapper.getData().getName() + " - " + playlistWrapper.getData().getOwnerV2().getData().getUsername()});
                        }
                    }
                } catch (IOException ex) {
                    ConsoleLogging.Throwable(ex);
                }
                //ToDo: Re-implement load more
                //searchsonglist.addModifyAction(() -> ((DefaultTableModel) searchsonglist.getModel()).addRow(new Object[]{PublicValues.language.translate("general.load_more"), PublicValues.language.translate("general.load_more"), PublicValues.language.translate("general.load_more"), PublicValues.language.translate("general.load_more")}));
              } finally {
                  searchGuard.exit();
              }
            });
        });
        searchscrollpanel = new JScrollPane();
        add(searchscrollpanel, BorderLayout.CENTER);
        searchsonglist = new DefTable();
        searchsonglist.getTableHeader().setReorderingAllowed(false);
        searchsonglist.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    if (!searchplaylisttableGuard.tryEnter()) return;
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
                    searchsonglist.setColumnSelectionInterval(0, searchsonglist.getColumnCount() - 1);
                    AsyncUtils.run(() -> {
                      try {
                        switch (searchsonglistcache.get(searchsonglist.getSelectedRow()).split(":")[1].toLowerCase()) {
                            case "playlist":
                                searchplaylistsongscache.clear();
                                ((DefaultTableModel) searchplaylisttable.getModel()).setRowCount(0);
                                try {
                                    Playlist4ApiProto.SelectedListContent playlist = PublicValues.session.api().getPlaylist(PlaylistId.fromUri(searchsonglistcache.get(searchsonglist.getSelectedRow())));
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
                                    Metadata.Show show = PublicValues.session.api().getMetadata4Show(ShowId.fromUri(searchsonglistcache.get(searchsonglist.getSelectedRow())));
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
                                    Metadata.Album album = PublicValues.session.api().getMetadata4Album(AlbumId.fromUri(searchsonglistcache.get(searchsonglist.getSelectedRow())));
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
                      } finally {
                          searchplaylisttableGuard.exit();
                      }
                    });
                } else {
                    searchsonglist.setColumnSelectionInterval(0, searchsonglist.getColumnCount() - 1);
                }
            }
        });
        searchsonglist.getTableHeader().setForeground(PublicValues.globalFontColor);
        searchsonglist.setForeground(PublicValues.globalFontColor);
        searchsonglist.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("general.name"), PublicValues.language.translate("general.filesize"), PublicValues.language.translate("general.bitrate"), PublicValues.language.translate("general.length")}));
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
        searchbackbutton = new JButton(PublicValues.language.translate("general.back"));
        backButtonContainer.add(searchbackbutton, BorderLayout.WEST);
        searchbackbutton.setForeground(PublicValues.globalFontColor);
        searchplaylistscrollpanel = new JScrollPane();
        searchplaylistpanel.add(searchplaylistscrollpanel, BorderLayout.CENTER);
        searchplaylisttable = new DefTable();
        searchplaylistscrollpanel.setViewportView(searchplaylisttable);
        searchplaylisttable.setForeground(PublicValues.globalFontColor);
        searchplaylisttable.getTableHeader().setForeground(PublicValues.globalFontColor);
        searchplaylisttable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    searchplaylisttable.setColumnSelectionInterval(0, searchplaylisttable.getColumnCount() - 1);
                    AsyncUtils.run(() -> {
                        InstanceManager.getPlayer().getPlayer().load(searchplaylistsongscache.get(searchplaylisttable.getSelectedRow()), true, PublicValues.shuffle);
                        TrackUtils.addAllToQueue(searchplaylistsongscache, searchplaylisttable);
                    });
                }
            }
        });
        searchplaylisttable.setModel(new DefaultTableModel(new Object[][]{}, new String[]{PublicValues.language.translate("general.name"), PublicValues.language.translate("general.filesize"), PublicValues.language.translate("general.bitrate"), PublicValues.language.translate("general.length")}));
        searchplaylistpanel.setVisible(false);

        searchplaylistsongscontextmenu = new ContextMenu(searchplaylisttable, searchplaylistsongscache, getClass());
        searchbackbutton.addActionListener(e -> {
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
        });
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

package com.spotifyxp.panels;

import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.deps.se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Artist;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Track;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.threading.DefThread;
import com.spotifyxp.utils.AsyncMouseListener;
import com.spotifyxp.utils.SpotifyUtils;
import com.spotifyxp.utils.StringUtils;
import com.spotifyxp.utils.TrackUtils;
import org.apache.hc.core5.http.ParseException;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class HomePanel {
    JScrollPane scrollholder;
    JPanel content;

    final UnofficialSpotifyAPI.HomeTab tab;

    public HomePanel() {
        tab = InstanceManager.getUnofficialSpotifyApi().getHomeTab();
        initializeLayout();
    }

    public void initializeLayout() {
        content = new JPanel();
        content.setPreferredSize(new Dimension(784, 337 * tab.sections.size()));
        scrollholder = new JScrollPane(content);
        scrollholder.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollholder.setSize(784, 421);
        DefThread t = new DefThread(this::initializeContent);
        t.start();
    }

    int addCache = 302;

    int cache = 0;

    public enum ContentTypes {
        show,
        track,
        album,
        artist,
        episode,
        user,
        playlist
    }

    public void addModule(UnofficialSpotifyAPI.HomeTabSection section) {
        ArrayList<String> uricache = new ArrayList<>();
        JLabel homepanelmoduletext = new JLabel(section.name);
        homepanelmoduletext.setFont(new Font("Tahoma", Font.PLAIN, 16));
        homepanelmoduletext.setBounds(0, addCache + 11, 375, 24);
        content.add(homepanelmoduletext);

        homepanelmoduletext.setForeground(PublicValues.globalFontColor);

        JScrollPane homepanelmodulescrollpanel = new JScrollPane();
        homepanelmodulescrollpanel.setBounds(0, addCache + 38, 777, 281);
        content.add(homepanelmodulescrollpanel);

        DefTable homepanelmodulecontenttable = new DefTable()  {
        };
        homepanelmodulescrollpanel.setViewportView(homepanelmodulecontenttable);
        homepanelmodulecontenttable.setForeground(PublicValues.globalFontColor);
        homepanelmodulecontenttable.getTableHeader().setForeground(PublicValues.globalFontColor);

        homepanelmodulecontenttable.setModel(new DefaultTableModel(
                new Object[][] {
                },
                new String[] {
                        "Name", "Artist"
                }
        ));


        for(UnofficialSpotifyAPI.HomeTabAlbum album : section.albums) {
            uricache.add(album.uri);
            homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{album.name, artistParser(album.artists)}));
        }
        for(UnofficialSpotifyAPI.HomeTabEpisodeOrChapter episodeOrChapter : section.episodeOrChapters) {
            uricache.add(episodeOrChapter.uri);
            homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{episodeOrChapter.EpisodeOrChapterName, episodeOrChapter.name + " - " + episodeOrChapter.publisherName}));
        }
        for(UnofficialSpotifyAPI.HomeTabPlaylist playlist : section.playlists) {
            uricache.add(playlist.uri);
            homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{playlist.name, playlist.ownerName}));
        }
        for(UnofficialSpotifyAPI.HomeTabArtist artist : section.artists) {
            uricache.add(artist.uri);
            homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{artist.name, ""}));
        }

        homepanelmodulecontenttable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if(e.getClickCount() == 2) {
                    ContentTypes ct = ContentTypes.valueOf(uricache.get(homepanelmodulecontenttable.getSelectedRow()).split(":")[1]);
                    String uri = uricache.get(homepanelmodulecontenttable.getSelectedRow());
                    String id = uri.split(":")[2];
                    try {
                        switch (ct) {
                            case track:
                                PublicValues.spotifyplayer.load(uri, true, false);
                                Events.triggerEvent(SpotifyXPEvents.queueUpdate.getName());
                                break;
                            case artist:
                                scrollholder.setVisible(false);
                                ArtistPanel.popularuricache.clear();
                                ArtistPanel.albumuricache.clear();
                                ((DefaultTableModel)ArtistPanel.artistalbumalbumtable.getModel()).setRowCount(0);
                                ((DefaultTableModel)ArtistPanel.artistpopularsonglist.getModel()).setRowCount(0);
                                ArtistPanel.artisttitle.setText("");
                                try {
                                    Artist a = InstanceManager.getSpotifyApi().getArtist(id).build().execute();
                                    try {
                                        ArtistPanel.artistimage.setImage(new URL(SpotifyUtils.getImageForSystem(a.getImages()).getUrl()).openStream());
                                    } catch (ArrayIndexOutOfBoundsException exception) {
                                        //No artist image (when this is raised it's a bug)
                                    }
                                    ArtistPanel.artisttitle.setText(a.getName());
                                    DefThread trackthread = new DefThread(() -> {
                                        try {
                                            for (Track t : InstanceManager.getSpotifyApi().getArtistsTopTracks(id, PublicValues.countryCode).build().execute()) {
                                                ArtistPanel.popularuricache.add(t.getUri());
                                                InstanceManager.getSpotifyAPI().addSongToList(TrackUtils.getArtists(t.getArtists()), t, ArtistPanel.artistpopularsonglist);
                                            }
                                        } catch (IOException | ParseException | SpotifyWebApiException ex) {
                                            ConsoleLogging.Throwable(ex);
                                        }
                                    });
                                    DefThread albumthread = new DefThread(() -> InstanceManager.getSpotifyAPI().addAllAlbumsToList(ArtistPanel.albumuricache, uri, ArtistPanel.artistalbumalbumtable));
                                    albumthread.start();
                                    trackthread.start();
                                } catch (IOException | ParseException | SpotifyWebApiException ex) {
                                    ConsoleLogging.Throwable(ex);
                                }
                                ContentPanel.artistPanel.openPanel();
                                ArtistPanel.isFirst = true;
                                ArtistPanel.contentPanel.setVisible(true);
                                ContentPanel.artistPanelBackButton.setVisible(true);
                                break;
                            case episode:
                                PublicValues.spotifyplayer.load(uri, true, PublicValues.shuffle);
                                Events.triggerEvent(SpotifyXPEvents.queueUpdate.getName());
                                break;
                            default:
                                ContentPanel.showAdvancedSongPanel(uri, ct);
                                break;
                        }
                    }catch (Exception ignored) {
                    }
                }
            }
        }));

        addCache+=319;
        cache++;
        content.revalidate();
        content.repaint();
    }

    String artistParser(ArrayList<UnofficialSpotifyAPI.HomeTabArtistNoImage> cache) {
        StringBuilder builder = new StringBuilder();
        int read = 0;
        for(UnofficialSpotifyAPI.HomeTabArtistNoImage s : cache) {
            if(read==cache.size()) {
                builder.append(s.name);
            }else{
                builder.append(s.name).append(",");
            }
            read++;
        }
        return StringUtils.replaceLast(builder.toString(), ",", "");
    }


    public void initializeContent() {
        content.setLayout(null);
        ArrayList<String> usersuricache = new ArrayList<>();

        JScrollPane homepaneluserscrollpanel = new JScrollPane();
        homepaneluserscrollpanel.setBounds(0, 39, 777, 261);
        content.add(homepaneluserscrollpanel);

        DefTable homepanelusertable = new DefTable()  {
        };
        homepaneluserscrollpanel.setViewportView(homepanelusertable);
        homepanelusertable.setForeground(PublicValues.globalFontColor);
        homepanelusertable.getTableHeader().setForeground(PublicValues.globalFontColor);

        homepanelusertable.setModel(new DefaultTableModel(
                new Object[][] {
                },
                new String[] {
                        "Name", "Artist"
                }
        ));

        homepanelusertable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if(e.getClickCount() == 2) {
                    ContentTypes ct = ContentTypes.valueOf(usersuricache.get(homepanelusertable.getSelectedRow()).split(":")[1]);
                    String uri = usersuricache.get(homepanelusertable.getSelectedRow());
                    String id = uri.split(":")[2];
                    try {
                        switch (ct) {
                            case track:
                                PublicValues.spotifyplayer.load(uri, true, false);
                                Events.triggerEvent(SpotifyXPEvents.queueUpdate.getName());
                                break;
                            case artist:
                                scrollholder.setVisible(false);
                                ArtistPanel.popularuricache.clear();
                                ArtistPanel.albumuricache.clear();
                                ((DefaultTableModel)ArtistPanel.artistalbumalbumtable.getModel()).setRowCount(0);
                                ((DefaultTableModel)ArtistPanel.artistpopularsonglist.getModel()).setRowCount(0);
                                ArtistPanel.artisttitle.setText("");
                                try {
                                    Artist a = InstanceManager.getSpotifyApi().getArtist(id).build().execute();
                                    try {
                                        ArtistPanel.artistimage.setImage(new URL(SpotifyUtils.getImageForSystem(a.getImages()).getUrl()).openStream());
                                    } catch (ArrayIndexOutOfBoundsException exception) {
                                        //No artist image (when this is raised it's a bug)
                                    }
                                    ArtistPanel.artisttitle.setText(a.getName());
                                    DefThread trackthread = new DefThread(() -> {
                                        try {
                                            for (Track t : InstanceManager.getSpotifyApi().getArtistsTopTracks(id, PublicValues.countryCode).build().execute()) {
                                                ArtistPanel.popularuricache.add(t.getUri());
                                                InstanceManager.getSpotifyAPI().addSongToList(TrackUtils.getArtists(t.getArtists()), t, ArtistPanel.artistpopularsonglist);
                                            }
                                        } catch (IOException | ParseException | SpotifyWebApiException ex) {
                                            ConsoleLogging.Throwable(ex);
                                        }
                                    });
                                    DefThread albumthread = new DefThread(() -> InstanceManager.getSpotifyAPI().addAllAlbumsToList(ArtistPanel.albumuricache, uri, ArtistPanel.artistalbumalbumtable));
                                    albumthread.start();
                                    trackthread.start();
                                } catch (IOException | ParseException | SpotifyWebApiException ex) {
                                    ConsoleLogging.Throwable(ex);
                                }
                                ContentPanel.artistPanel.openPanel();
                                ArtistPanel.isFirst = true;
                                ArtistPanel.contentPanel.setVisible(true);
                                ContentPanel.artistPanelBackButton.setVisible(true);
                                break;
                            default:
                                ContentPanel.showAdvancedSongPanel(uri, ct);
                                break;
                        }
                    }catch (Exception ignored) {
                    }
                }
            }
        }));

        JLabel homepanelgreetingstext = new JLabel(tab.greeting);
        homepanelgreetingstext.setFont(new Font("Tahoma", Font.PLAIN, 16));
        homepanelgreetingstext.setBounds(0, 11, 375, 24);
        content.add(homepanelgreetingstext);
        homepanelgreetingstext.setForeground(PublicValues.globalFontColor);

        for(UnofficialSpotifyAPI.HomeTabAlbum album : tab.firstSection.albums) {
            usersuricache.add(album.uri);
            homepanelusertable.addModifyAction(() -> ((DefaultTableModel) homepanelusertable.getModel()).addRow(new Object[]{album.name, artistParser(album.artists)}));
        }
        for(UnofficialSpotifyAPI.HomeTabEpisodeOrChapter episodeOrChapter : tab.firstSection.episodeOrChapters) {
            usersuricache.add(episodeOrChapter.uri);
            homepanelusertable.addModifyAction(() -> ((DefaultTableModel) homepanelusertable.getModel()).addRow(new Object[]{episodeOrChapter.EpisodeOrChapterName, episodeOrChapter.name + " - " + episodeOrChapter.publisherName}));
        }
        for(UnofficialSpotifyAPI.HomeTabPlaylist playlist : tab.firstSection.playlists) {
            usersuricache.add(playlist.uri);
            homepanelusertable.addModifyAction(() -> ((DefaultTableModel) homepanelusertable.getModel()).addRow(new Object[]{playlist.name, playlist.ownerName}));
        }
        for(UnofficialSpotifyAPI.HomeTabArtist artist : tab.firstSection.artists) {
            usersuricache.add(artist.uri);
            homepanelusertable.addModifyAction(() -> ((DefaultTableModel) homepanelusertable.getModel()).addRow(new Object[]{artist.name, ""}));
        }

        for(UnofficialSpotifyAPI.HomeTabSection section : tab.sections) {
            addModule(section);
        }
    }


    public JScrollPane getComponent() {
        return scrollholder;
    }

    public JPanel getPanel() {
        return content;
    }
}

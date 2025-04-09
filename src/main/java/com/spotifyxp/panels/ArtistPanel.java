package com.spotifyxp.panels;

import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Album;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Artist;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Track;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.swingextension.JImagePanel;
import com.spotifyxp.utils.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

public class ArtistPanel extends JScrollPane implements View {
    public static JPanel contentPanel;
    public static JLabel artistTitle;
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

    public static boolean isLastArtist = false;
    public static ArrayList<String> albumUriCache = new ArrayList<>();
    public static ArrayList<String> popularUriCache = new ArrayList<>();
    public static ArrayList<Runnable> runWhenOpeningArtistPanel = new ArrayList<>();

    public ArtistPanel() {
        contentPanel = new JPanel();
        contentPanel.setLayout(null);
        contentPanel.setPreferredSize(new Dimension(getWidth(), 1005));

        getVerticalScrollBar().setUnitIncrement(20);
        setViewportView(contentPanel);
        setVisible(false);

        artistImage = new JImagePanel();
        artistImage.setBounds(288, 11, 155, 153);
        contentPanel.add(artistImage);

        artistTitle = new JLabel("");
        artistTitle.setHorizontalAlignment(SwingConstants.CENTER);
        artistTitle.setBounds(0, 213, 780, 64);
        artistTitle.setForeground(PublicValues.globalFontColor);
        contentPanel.add(artistTitle);

        backButton = new JButton(PublicValues.language.translate("ui.back"));
        backButton.setBounds(0, 0, 89, 23);
        backButton.setForeground(PublicValues.globalFontColor);
        backButton.addActionListener(new AsyncActionListener(e -> ContentPanel.switchView(ContentPanel.lastView)));
        contentPanel.add(backButton);

        artistPopularLabel = new JLabel("Popular"); //ToDo: Translate
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
                        Album album = InstanceManager.getSpotifyApi().getAlbum(albumUriCache.get(artistAlbumTable.getSelectedRow()).split(":")[2]).build().execute();
                        for (TrackSimplified simplified : album.getTracks().getItems()) {
                            artistAlbumTable.addModifyAction(() -> {
                                ((DefaultTableModel) Search.searchplaylisttable.getModel()).addRow(new Object[]{simplified.getName(), TrackUtils.calculateFileSizeKb(simplified.getDurationMs()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(simplified.getDurationMs())});
                                Search.searchplaylistsongscache.add(simplified.getUri());
                            });
                        }
                    } catch (IOException ex) {
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

        artistAlbumLabel = new JLabel("Albums"); //ToDo: Translate
        artistAlbumLabel.setBounds(5, 642, 102, 14);
        artistAlbumLabel.setForeground(PublicValues.globalFontColor);
        contentPanel.add(artistAlbumLabel);

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

    public void fillWith(Artist artist) throws IOException {
        reset();
        artistImage.setImage(new URL(SpotifyUtils.getImageForSystem(artist.getImages()).getUrl()).openStream());
        artistTitle.setText(artist.getName());
        Thread trackthread = new Thread(() -> {
            try {
                for (Track t : InstanceManager.getSpotifyApi().getArtistsTopTracks(artist.getId(), PublicValues.countryCode).build().execute()) {
                    ArtistPanel.popularUriCache.add(t.getUri());
                    InstanceManager.getSpotifyAPI().addSongToList(TrackUtils.getArtists(t.getArtists()), t, artistPopularSongList);
                }
            } catch (IOException ex) {
                ConsoleLogging.Throwable(ex);
            }
        }, "Get tracks (HomePanel)");
        InstanceManager.getSpotifyAPI().addAllAlbumsToList(ArtistPanel.albumUriCache, artist.getUri(), ArtistPanel.artistAlbumTable);
        trackthread.start();
    }

    public void reset() {
        popularUriCache.clear();
        albumUriCache.clear();
        ((DefaultTableModel) artistAlbumTable.getModel()).setRowCount(0);
        ((DefaultTableModel) artistPopularSongList.getModel()).setRowCount(0);
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

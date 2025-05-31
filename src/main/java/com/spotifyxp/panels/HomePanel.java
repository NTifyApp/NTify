/*
 * Copyright [2023-2025] [Gianluca Beil]
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
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Artist;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.AsyncMouseListener;
import com.spotifyxp.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.Timer;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class HomePanel extends JScrollPane implements View {
    public static JPanel content;
    public static Optional<UnofficialSpotifyAPI.HomeTab> tab;
    public static ContextMenu menu;
    public static Timer reloadTimer;
    public static TimerTask nextReload;

    public HomePanel() {
        content = new JPanel();
        content.setLayout(null);

        reloadTimer = new Timer();

        menu = new ContextMenu(content, null, getClass());
        menu.addItem(PublicValues.language.translate("ui.general.refresh"), () -> {
            nextReload.cancel();
            reloadHome();
        });

        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVisible(false);
        setViewportView(content);

        CompletableFuture<Boolean> homeFuture = loadHome();

        Events.subscribe(SpotifyXPEvents.onFrameVisible.getName(), (args) -> {
            Thread thread = new Thread(() -> {
                try {
                    homeFuture.join();
                }catch (CancellationException e) {
                    ConsoleLogging.error("Failed to get home tab");
                    return;
                }
                SwingUtilities.invokeLater(this::fill);
                nextReload = new TimerTask() {
                    @Override
                    public void run() {
                        reloadHome();
                    }
                };
                reloadTimer.schedule(nextReload, Date.from(Instant.now().plusSeconds(1800))); // Every 30 minutes
            }, "Wait for home tab");
            thread.start();
        });
    }

    private void reloadHome() {
        Thread thread = new Thread(() -> {
            try {
                loadHome().join();
            }catch (CancellationException e) {
                ConsoleLogging.error("Failed to get home tab");
                return;
            }
            content.removeAll();
            SwingUtilities.invokeLater(this::fill);
            nextReload = new TimerTask() {
                @Override
                public void run() {
                    reloadHome();
                }
            };
            reloadTimer.schedule(nextReload, Date.from(Instant.now().plusSeconds(1800))); // Every 30 minutes
        }, "Wait for home tab");
        thread.start();
    }

    private CompletableFuture<Boolean> loadHome() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Thread requestTabThread = new Thread(() -> {
            try {
                tab = InstanceManager.getUnofficialSpotifyApi().getHomeTab();
                future.complete(null);
            } catch (IOException e) {
                future.cancel(false);
                throw new RuntimeException(e);
            }
        }, "Request home tab");
        requestTabThread.start();
        return future;
    }

    public enum ContentTypes {
        show,
        track,
        album,
        artist,
        episode,
        user,
        playlist
    }

    public void addModule(UnofficialSpotifyAPI.HomeTabSection section, int titleHeight, int x, int y, int titleY, int width, int height) {
        ArrayList<String> uricache = new ArrayList<>();
        JLabel homepanelmoduletext = new JLabel(section.getName().orElse(""));
        homepanelmoduletext.setFont(new Font("Tahoma", Font.PLAIN, 16));
        homepanelmoduletext.setBounds(x, titleY, width, titleHeight);
        content.add(homepanelmoduletext);

        homepanelmoduletext.setForeground(PublicValues.globalFontColor);

        JScrollPane homepanelmodulescrollpanel = new JScrollPane();
        homepanelmodulescrollpanel.setBounds(x, y, width, height);
        content.add(homepanelmodulescrollpanel);

        DefTable homepanelmodulecontenttable = new DefTable() {
        };
        homepanelmodulescrollpanel.setViewportView(homepanelmodulecontenttable);
        homepanelmodulecontenttable.setForeground(PublicValues.globalFontColor);
        homepanelmodulecontenttable.getTableHeader().setForeground(PublicValues.globalFontColor);
        homepanelmodulecontenttable.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        "Name", "Artist"
                }
        ));

        new ContextMenu(homepanelmodulecontenttable, uricache, getClass());

        for(UnofficialSpotifyAPI.HomeTabSectionItem item : section.getItems()) {
            switch (item.getType()) {
                case AlbumResponseWrapper:
                    if(!item.getAlbum().isPresent()) break;
                    UnofficialSpotifyAPI.HomeTabAlbum album = item.getAlbum().get();
                    uricache.add(album.getUri());
                    homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{album.getName(), artistParser(album.getArtists())}));
                    break;
                case ArtistResponseWrapper:
                    if(!item.getArtist().isPresent()) break;
                    UnofficialSpotifyAPI.HomeTabArtist artist = item.getArtist().get();
                    uricache.add(artist.getUri());
                    homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{artist.getName(), ""}));
                    break;
                case EpisodeOrChapterResponseWrapper:
                    if(!item.getEpisodeOrChapter().isPresent()) break;
                    UnofficialSpotifyAPI.HomeTabEpisodeOrChapter episodeOrChapter = item.getEpisodeOrChapter().get();
                    uricache.add(episodeOrChapter.getUri());
                    homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{episodeOrChapter.getEpisodeOrChapterName(), episodeOrChapter.getName() + " - " + episodeOrChapter.getPublisherName()}));
                    break;
                case PlaylistResponseWrapper:
                    if(!item.getPlaylist().isPresent()) break;
                    UnofficialSpotifyAPI.HomeTabPlaylist playlist = item.getPlaylist().get();
                    uricache.add(playlist.getUri());
                    homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{playlist.getName(), playlist.getOwnerName()}));
                    break;
            }
        }

        homepanelmodulecontenttable.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    ContentTypes ct = ContentTypes.valueOf(uricache.get(homepanelmodulecontenttable.getSelectedRow()).split(":")[1]);
                    String uri = uricache.get(homepanelmodulecontenttable.getSelectedRow());
                    String id = uri.split(":")[2];
                    try {
                        switch (ct) {
                            case episode:
                            case track:
                                InstanceManager.getSpotifyPlayer().load(uri, true, PublicValues.shuffle);
                                Events.triggerEvent(SpotifyXPEvents.queueUpdate.getName());
                                break;
                            case artist:
                                setVisible(false);
                                ContentPanel.switchView(Views.ARTIST);
                                try {
                                    Artist a = InstanceManager.getSpotifyApi().getArtist(id).build().execute();
                                    ContentPanel.artistPanel.fillWith(a);
                                } catch (IOException ex) {
                                    ConsoleLogging.Throwable(ex);
                                }
                                break;
                            default:
                                ContentPanel.trackPanel.open(uri, ct);
                                break;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }));
    }

    String artistParser(ArrayList<UnofficialSpotifyAPI.HomeTabArtist> cache) {
        StringBuilder builder = new StringBuilder();
        int read = 0;
        for (UnofficialSpotifyAPI.HomeTabArtist s : cache) {
            if (read == cache.size()) {
                builder.append(s.getName());
            } else {
                builder.append(s.getName()).append(",");
            }
            read++;
        }
        return StringUtils.replaceLast(builder.toString(), ",", "");
    }


    public void initializeContent() {
        if(!tab.isPresent()) return;

        int width = getWidth() - 32;
        int height = 261;
        int spacing = 70;
        int xCache = 10;
        int titleHeight = getFontMetrics(new Font("Tahoma", Font.PLAIN, 16)).getHeight();
        int yCache = titleHeight + 55;
        int titleSpacing = 5;

        UnofficialSpotifyAPI.HomeTab tabCopy = tab.get();

        JPanel homepanelgreetings = new JPanel();
        homepanelgreetings.setBounds(0, 11, getWidth(), getFontMetrics(new Font("Tahoma", Font.PLAIN, 20)).getHeight());
        homepanelgreetings.setLayout(new BorderLayout());
        JLabel homepanelgreetingstext = new JLabel(tabCopy.getGreeting());
        homepanelgreetingstext.setFont(new Font("Tahoma", Font.PLAIN, 20));
        homepanelgreetingstext.setHorizontalAlignment(SwingConstants.CENTER);
        homepanelgreetingstext.setForeground(PublicValues.globalFontColor);
        homepanelgreetings.add(homepanelgreetingstext);
        content.add(homepanelgreetings);

        for (UnofficialSpotifyAPI.HomeTabSection section : tabCopy.getSections()) {
            addModule(section, titleHeight, xCache, yCache, yCache - titleHeight - titleSpacing, width, height);
            yCache += height + spacing;
        }
    }

    void fill() {
        Thread t = new Thread(this::initializeContent, "Get home");
        t.start();
        if(!tab.isPresent()) return;
        content.setPreferredSize(new Dimension(content.getWidth(), (261 + getFontMetrics(getFont()).getHeight() + 55) * tab.get().getSections().size()));
        content.revalidate();
        content.repaint();
    }

    public JPanel getPanel() {
        return content;
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

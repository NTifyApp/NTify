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

import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.spotapi.pojos.HomeResponse;
import com.spotifyxp.utils.AsyncUtils;
import com.spotifyxp.utils.StringUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

public class HomePanel extends JScrollPane implements View {
    private static final String CACHE_ID = "homeTab";

    public static JPanel content;
    public static HomeResponse tab;
    public static ContextMenu menu;
    public static Timer reloadTimer;
    public static TimerTask nextReload;
    private static boolean refreshDue = false;
    private static boolean isVisible = false;
    private static boolean loadInProgress = false;

    public HomePanel() {
        content = new JPanel();
        content.setLayout(null);

        reloadTimer = new Timer();

        menu = new ContextMenu(content, null, getClass());
        menu.addItem(PublicValues.language.translate("general.refresh"), () -> {
            nextReload.cancel();
            try {
                PublicValues.cache.namespace("HomePanel").remove(CACHE_ID);
            } catch (IOException e) {
                ConsoleLogging.Throwable(e);
            }
            reloadHome();
        });

        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        setVisible(false);
        setViewportView(content);

        loadInProgress = true;
        CompletableFuture<Boolean> homeFuture = loadHome();

        SpotifyXPEvents.onFrameVisible.subscribe((data) -> {
            Thread thread = new Thread(() -> {
                try {
                    homeFuture.join();
                }catch (CancellationException e) {
                    ConsoleLogging.error("Failed to get home tab");
                    loadInProgress = false;
                    return;
                }
                SwingUtilities.invokeLater(this::fill);
                loadInProgress = false;
                scheduleNextReload();
            }, "Wait for home tab");
            thread.start();
        });
    }

    private void scheduleNextReload() {
        nextReload = new TimerTask() {
            @Override
            public void run() {
                if (isVisible) {
                    reloadHome();
                } else {
                    refreshDue = true;
                }
            }
        };
        reloadTimer.schedule(nextReload, Date.from(Instant.now().plusSeconds(1800))); // Every 30 minutes
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
            refreshDue = false;
            scheduleNextReload();
        }, "Wait for home tab");
        thread.start();
    }

    private CompletableFuture<Boolean> loadHome() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Thread requestTabThread = new Thread(() -> {
            try {
                if (PublicValues.cache.namespace("HomePanel").has(CACHE_ID)) {
                    tab = PublicValues.cache.namespace("HomePanel").get(CACHE_ID, HomeResponse.class);
                } else {
                    tab = PublicValues.spotAPI.feed().home().execute();
                    if (tab != null) {
                        PublicValues.cache.namespace("HomePanel").put(CACHE_ID, tab);
                    }
                }
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

    public void addModule(HomeResponse.Section section, int titleHeight, int x, int y, int titleY, int width, int height) {
        ArrayList<String> uricache = new ArrayList<>();
        String sectionName = (section.getData() != null && section.getData().getTitle() != null)
                ? section.getData().getTitle().getText() : "";
        JLabel homepanelmoduletext = new JLabel(sectionName);
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
                        PublicValues.language.translate( "general.name"), PublicValues.language.translate("general.artist")
                }
        ));

        new ContextMenu(homepanelmodulecontenttable, uricache, getClass());

        if (section.getSectionItems() != null && section.getSectionItems().getItems() != null) {
            for(HomeResponse.SectionItem sectionItem : section.getSectionItems().getItems()) {
                HomeResponse.ContentItem item = sectionItem.getContent();
                if (item == null) continue;

                HomeResponse.AlbumData album = item.asAlbum();
                HomeResponse.EpisodeData episodeOrChapter = item.asEpisodeOrChapter();
                HomeResponse.PlaylistData playlist = item.asPlaylist();

                if (album != null) {
                    uricache.add(album.getUri());
                    homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{album.getName(), artistParser(album.getArtists())}));
                } else if (episodeOrChapter != null) {
                    uricache.add(episodeOrChapter.getUri());
                    String showName = episodeOrChapter.getPodcastV2() != null && episodeOrChapter.getPodcastV2().getData() != null
                            ? episodeOrChapter.getPodcastV2().getData().getName() : "";
                    homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{episodeOrChapter.getName(), showName}));
                } else if (playlist != null) {
                    uricache.add(playlist.getUri());
                    String ownerName = playlist.getOwnerV2() != null && playlist.getOwnerV2().getData() != null
                            ? playlist.getOwnerV2().getData().getName() : "";
                    homepanelmodulecontenttable.addModifyAction(() -> ((DefaultTableModel) homepanelmodulecontenttable.getModel()).addRow(new Object[]{playlist.getName(), ownerName}));
                }
            }
        }

        homepanelmodulecontenttable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (e.getClickCount() == 2) {
                    AsyncUtils.run(() -> {
                        ContentTypes ct = ContentTypes.valueOf(uricache.get(homepanelmodulecontenttable.getSelectedRow()).split(":")[1]);
                        String uri = uricache.get(homepanelmodulecontenttable.getSelectedRow());
                        try {
                            switch (ct) {
                                case episode:
                                case track:
                                    InstanceManager.getSpotifyPlayer().load(uri, true, PublicValues.shuffle);
                                    SpotifyXPEvents.queueUpdate.trigger(uri);
                                    break;
                                case artist:
                                    setVisible(false);
                                    ContentPanel.switchView(Views.ARTIST);
                                    try {
                                        ContentPanel.artistPanel.fillWith(uri);
                                    } catch (IOException ex) {
                                        ConsoleLogging.Throwable(ex);
                                    }
                                    break;
                                default:
                                    ContentPanel.trackPanel.open(uri, ct);
                                    break;
                            }
                        } catch (Exception exception) {
                            ConsoleLogging.Throwable(exception);
                        }
                    });
                }
            }
        });
    }

    String artistParser(HomeResponse.Artists artists) {
        StringBuilder builder = new StringBuilder();
        if (artists == null || artists.getItems() == null) return "";
        List<HomeResponse.Artist> cache = artists.getItems();
        int read = 0;
        for (HomeResponse.Artist s : cache) {
            if (read == cache.size()) {
                builder.append(s.getProfile().getName());
            } else {
                builder.append(s.getProfile().getName()).append(",");
            }
            read++;
        }
        return StringUtils.replaceLast(builder.toString(), ",", "");
    }


    public void initializeContent() {
        if(tab == null) return;

        int width = getWidth() - 32;
        int height = 261;
        int spacing = 70;
        int xCache = 10;
        int titleHeight = getFontMetrics(new Font("Tahoma", Font.PLAIN, 16)).getHeight();
        int yCache = titleHeight + 55;
        int titleSpacing = 5;

        JPanel homepanelgreetings = new JPanel();
        homepanelgreetings.setBounds(0, 11, getWidth(), getFontMetrics(new Font("Tahoma", Font.PLAIN, 20)).getHeight());
        homepanelgreetings.setLayout(new BorderLayout());
        JLabel homepanelgreetingstext = new JLabel(tab.getGreeting() != null ? tab.getGreeting().getText() : "");
        homepanelgreetingstext.setFont(new Font("Tahoma", Font.PLAIN, 20));
        homepanelgreetingstext.setHorizontalAlignment(SwingConstants.CENTER);
        homepanelgreetingstext.setForeground(PublicValues.globalFontColor);
        homepanelgreetings.add(homepanelgreetingstext);
        content.add(homepanelgreetings);

        for (HomeResponse.Section section : tab.getSectionContainer().getSections().getItems()) {
            addModule(section, titleHeight, xCache, yCache, yCache - titleHeight - titleSpacing, width, height);
            yCache += height + spacing;
        }

        tab = null;
    }

    void fill() {
        if(tab == null) return;
        int sectionCount = tab.getSectionContainer().getSections().getItems().size();

        Thread t = new Thread(() -> {
            initializeContent();
            SwingUtilities.invokeLater(() -> {
                content.setPreferredSize(new Dimension(content.getWidth(), (261 + getFontMetrics(getFont()).getHeight() + 55) * sectionCount));
                content.revalidate();
                content.repaint();
            });
        }, "Get home");
        t.start();
    }

    public JPanel getPanel() {
        return content;
    }

    @Override
    public void makeVisible() {
        setVisible(true);
        isVisible = true;

        if (refreshDue) {
            reloadHome();
        } else if (content.getComponentCount() == 0 && !loadInProgress) {
            loadInProgress = true;
            Thread thread = new Thread(() -> {
                try {
                    loadHome().join();
                } catch (CancellationException e) {
                    ConsoleLogging.error("Failed to get home tab");
                    loadInProgress = false;
                    return;
                }
                SwingUtilities.invokeLater(this::fill);
                loadInProgress = false;
            }, "Wait for home tab");
            thread.start();
        }
    }

    @Override
    public void makeInvisible() {
        setVisible(false);
        isVisible = false;

        if (!refreshDue && content.getComponentCount() > 0) {
            content.removeAll();
            content.revalidate();
            content.repaint();
        }
    }
}

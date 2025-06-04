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
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.guielements.ArtistEventView;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.guielements.SpotifyBrowseModule;
import com.spotifyxp.guielements.SpotifyBrowseSection;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.protogens.ConcertsOuterClass;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class BrowsePanel extends JScrollPane implements View {
    public static UnofficialSpotifyAPI.SpotifyBrowse spotifyBrowse;
    public static JPanel contentPanel;
    public static JPopupMenu popupMenu;
    public static DefTable table;
    public static ArrayList<String> genreIds;
    public static JCheckBoxMenuItem metroLayout;
    public static JCheckBoxMenuItem tableLayout;
    public static JScrollPane tableScrollPane;

    public BrowsePanel() {
        contentPanel = new JPanel();
        contentPanel.setLayout(null);
        contentPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)) {
                    xyRunnable.run(e.getX(), e.getY());
                }
            }
        });

        setVisible(false);
        setViewportView(contentPanel);
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        getVerticalScrollBar().setUnitIncrement(32);

        Thread thread = new Thread(() -> {
            try {
                spotifyBrowse = UnofficialSpotifyAPI.getSpotifyBrowse();
            }catch (IOException e) {
                throw new RuntimeException(e);
            }
            if(PublicValues.config.getInt(ConfigValues.browse_view_style.name) == 1) {
                displayBrowseTable();
            } else {
                displayBrowseMetro();
            }
        });
        thread.start();

        popupMenu = new JPopupMenu();
        metroLayout = new JCheckBoxMenuItem(PublicValues.language.translate("ui.browse.ctxmenu.metro"));
        tableLayout = new JCheckBoxMenuItem(PublicValues.language.translate("ui.browse.ctxmenu.table"));
        metroLayout.setSelected(PublicValues.config.getInt(ConfigValues.browse_view_style.name) == 0);
        tableLayout.setSelected(!metroLayout.isSelected());
        metroLayout.addActionListener(e -> {
            tableLayout.setSelected(false);
            metroLayout.setSelected(true);
            PublicValues.config.write(ConfigValues.browse_view_style.name, 0);
            PublicValues.config.save();
            contentPanel.removeAll();
            contentPanel.revalidate();
            contentPanel.repaint();
            Thread thread1 = new Thread(this::displayBrowseMetro);
            thread1.start();
        });
        tableLayout.addActionListener(e -> {
            metroLayout.setSelected(false);
            tableLayout.setSelected(true);
            PublicValues.config.write(ConfigValues.browse_view_style.name, 1);
            PublicValues.config.save();
            contentPanel.removeAll();
            contentPanel.revalidate();
            contentPanel.repaint();
            Thread thread2 = new Thread(this::displayBrowseTable);
            thread2.start();
        });
        popupMenu.add(metroLayout);
        popupMenu.add(tableLayout);
    }

    @FunctionalInterface
    public interface XYRunnable  {
        void run(int x, int y);
    }

    @FunctionalInterface
    public interface IDRunnable {
        void run(String id);
    }

    void displayBrowseTable() throws NoSuchElementException {
        genreIds = new ArrayList<>();

        table = new DefTable();
        table.setModel(new DefaultTableModel(
                new Object[][]{
                },
                new String[]{
                        PublicValues.language.translate("ui.general.name")
                }
        ));
        table.setForeground(PublicValues.globalFontColor);
        table.getTableHeader().setForeground(PublicValues.globalFontColor);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e)) {
                    xyRunnable.run(table.getX() + e.getX(), table.getY() + e.getY());
                    return;
                }
                if(e.getClickCount() == 2) {
                    try {
                        idRunnable.run(genreIds.get(table.getSelectedRow()).split(":")[2]);
                    }catch (ArrayIndexOutOfBoundsException ex) {
                        idRunnable.run(genreIds.get(table.getSelectedRow()));
                    }
                }
            }
        });

        tableScrollPane = new JScrollPane(table);
        tableScrollPane.setBounds(10, 10, 774,  461);

        for(UnofficialSpotifyAPI.SpotifyBrowseEntry entry : spotifyBrowse.getBody()) {
            if(entry.getMetadata().isPresent()) {
                if(!entry.getMetadata().get().getVideoUrl().isPresent()
                        && !entry.getComponent().getCategory().equals("row")
                        && !entry.getComponent().getCategory().toLowerCase(Locale.ENGLISH).contains("sectionheader")
                        && entry.getCustom().isPresent()
                        && entry.getCustom().get().getBackgroundColor().isPresent()) {
                    table.addModifyAction(() -> ((DefaultTableModel) table.getModel()).addRow(new Object[] {entry.getText().getTitle()}));
                    if (!entry.getImages().isPresent()
                            || !entry.getEvents().isPresent()
                            || !entry.getEvents().get().getEvents().get(0).getData_uri().isPresent()
                    ) throw new NoSuchElementException();
                    genreIds.add(entry.getEvents().get().getEvents().get(0).getData_uri().get().getUri());
                }
            }
        }

        contentPanel.add(tableScrollPane);
        contentPanel.setPreferredSize(new Dimension(782, 405));
        revalidate();
        repaint();
    }

    void displayBrowseMetro() throws NoSuchElementException {
        int yCache = 10;
        int xCache = 10;
        int xCount = 0;
        int elementWidth = (784 / 4) - 23;
        int elementHeight = (421 / 4) - 5;
        for(UnofficialSpotifyAPI.SpotifyBrowseEntry entry : spotifyBrowse.getBody()) {
            if(entry.getMetadata().isPresent()) {
                if(!entry.getMetadata().get().getVideoUrl().isPresent()
                        && !entry.getComponent().getCategory().equals("row")
                        && !entry.getComponent().getCategory().toLowerCase(Locale.ENGLISH).contains("sectionheader")
                        && entry.getCustom().isPresent()
                        && entry.getCustom().get().getBackgroundColor().isPresent()) {
                    if(xCount == 4) {
                        xCount = 0;
                        xCache = 10;
                        yCache += elementHeight + 20;
                    }

                    try {
                        String uri = "";
                        if (!entry.getImages().isPresent()
                                || !entry.getEvents().isPresent()
                                || !entry.getEvents().get().getEvents().get(0).getData_uri().isPresent()
                        ) throw new NoSuchElementException();
                        for(UnofficialSpotifyAPI.SpotifyBrowseEntryImagesImage image : entry.getImages().get().getImages()) {
                            if(image.getType() == UnofficialSpotifyAPI.SpotifyBrowseEntryImagesImageTypes.MAIN) {
                                uri = image.getUri();
                                break;
                            }
                        }
                        SpotifyBrowseModule panel = new SpotifyBrowseModule(xCache, yCache, entry.getText().getTitle(), new URL(uri).openStream(), elementWidth, elementHeight, entry.getEvents().get().getEvents().get(0).getData_uri().get().getUri(), xyRunnable, idRunnable);
                        panel.setBackground(Color.decode(entry.getCustom().get().getBackgroundColor().get()));
                        panel.setBounds(xCache, yCache, elementWidth, elementHeight);
                        contentPanel.add(panel);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    xCount += 1;
                    xCache += elementWidth + 20;
                }
            }
        }
        contentPanel.setPreferredSize(new Dimension(784, yCache));
        revalidate();
        repaint();
    }

    XYRunnable xyRunnable = new XYRunnable() {
        @Override
        public void run(int x, int y) {
            popupMenu.show(contentPanel, x, y);
        }
    };

    IDRunnable idRunnable = id -> {
        ContentPanel.switchView(Views.BROWSESECTION);
        Thread thread = new Thread(() -> {
            switch (id) {
                case "spotify:concerts":
                    try {
                        ContentPanel.sectionPanel.fillWith(concertsToViewDescriptor(UnofficialSpotifyAPI.getConcerts()));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    try {
                        ContentPanel.sectionPanel.fillWith(browseSectionToViewDescriptor(UnofficialSpotifyAPI.getSpotifyBrowseSection(id)));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
            }
        });
        thread.start();
        ContentPanel.blockTabSwitch();
    };

    private SpotifySectionPanel.ViewDescriptorBuilder concertsToViewDescriptor(ConcertsOuterClass.Concerts concerts) {
        SpotifySectionPanel.ViewDescriptorBuilder builder = new SpotifySectionPanel.ViewDescriptorBuilder();

        builder.setTitle(concerts.getHeader().getTitle());

        for(ConcertsOuterClass.Concerts.SectionsContainer container : concerts.getSectionsContainer().getSectionsContainerList()) {
            DefTable eventsTable = new DefTable();
            JScrollPane eventsTableScrollPane = new JScrollPane(eventsTable);
            JPanel contentPanel = new JPanel();
            DefTable eventsListTable = new DefTable();
            JScrollPane eventsListTableScrollPane = new JScrollPane(eventsListTable);
            JPanel alternateTablesTableContainer = new JPanel();
            JPanel alternateTablesContainer = new JPanel(new BorderLayout());
            JButton backButton = new JButton(PublicValues.language.translate("ui.back"));
            AtomicReference<ArtistEventView> eventView = new AtomicReference<>();

            AtomicBoolean isOnTicketView = new AtomicBoolean(false);

            HashMap<String, List<ConcertsOuterClass.Concerts.ArtistConcertConcert>> concertsMap = new HashMap<>();
            ArrayList<ConcertsOuterClass.Concerts.ArtistConcertConcert> currentEventsList = new ArrayList<>();

            alternateTablesTableContainer.setLayout(new BoxLayout(alternateTablesTableContainer, BoxLayout.Y_AXIS));
            alternateTablesContainer.add(alternateTablesTableContainer, BorderLayout.CENTER);

            contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

            contentPanel.add(eventsTableScrollPane);
            contentPanel.add(alternateTablesContainer);

            backButton.setForeground(PublicValues.globalFontColor);
            backButton.addActionListener(e -> {
                contentPanel.setBorder(BorderFactory.createEmptyBorder());
                if(isOnTicketView.get()) {
                    eventView.get().setVisible(false);
                    backButton.setVisible(false);
                    eventsTableScrollPane.setVisible(true);

                    contentPanel.revalidate();
                    contentPanel.repaint();

                    return;
                }
                eventsTableScrollPane.setVisible(true);
                eventsListTableScrollPane.setVisible(false);
                backButton.setVisible(false);

                contentPanel.revalidate();
                contentPanel.repaint();
            });
            backButton.setVisible(false);

            alternateTablesContainer.add(backButton, BorderLayout.NORTH);

            alternateTablesTableContainer.add(eventsListTableScrollPane);

            eventsListTableScrollPane.setVisible(false);

            eventsTable.setModel(new DefaultTableModel(new Object[][]{}, new Object[]{
                    PublicValues.language.translate("ui.general.artist"),
                    ""
            }));

            eventsTable.setForeground(PublicValues.globalFontColor);
            eventsTable.getTableHeader().setForeground(PublicValues.globalFontColor);

            ArrayList<String> eventsList = new ArrayList<>();

            eventsTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if(e.getClickCount() == 2
                            && !SwingUtilities.isRightMouseButton(e)
                            && eventsTable.getSelectedRow() != -1) {
                        List<ConcertsOuterClass.Concerts.ArtistConcertConcert> concerts = concertsMap.get(
                                eventsTable.getModel().getValueAt(eventsTable.getSelectedRow(), 0).toString()
                        );

                        if(concerts.size() > 1) {
                            ((DefaultTableModel) eventsListTable.getModel()).setRowCount(0);
                            currentEventsList.addAll(concerts);

                            eventsTableScrollPane.setVisible(false);
                            eventsListTableScrollPane.setVisible(true);
                            backButton.setVisible(true);

                            contentPanel.revalidate();
                            contentPanel.repaint();

                            eventsList.clear();

                            for(ConcertsOuterClass.Concerts.ArtistConcertConcert concert : concerts) {
                                eventsList.add(concert.getConcertUri().split(":")[2]);
                                eventsListTable.addModifyAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((DefaultTableModel) eventsListTable.getModel()).addRow(new Object[]{
                                                concert.getLocationName(),
                                                formatDate(parseDate(concert.getDate().getTime()))
                                        });
                                    }
                                });
                            }
                        }else {
                            new Thread(() -> {
                                try {
                                    backButton.setVisible(true);
                                    eventView.set(new ArtistEventView(UnofficialSpotifyAPI.getConcert(concerts.get(0).getConcertUri().split(":")[2])));
                                    eventsTableScrollPane.setVisible(false);
                                    alternateTablesTableContainer.add(eventView.get());
                                    contentPanel.setBorder(new LineBorder(Color.GRAY, 1));
                                    contentPanel.revalidate();
                                    contentPanel.repaint();
                                    isOnTicketView.set(true);
                                } catch (IOException ex) {
                                    ConsoleLogging.Throwable(ex);
                                }
                            }).start();
                        }
                    }
                }
            });

            eventsListTable.setModel(new DefaultTableModel(new Object[][]{}, new Object[]{
                    PublicValues.language.translate("ui.general.location"),
                    PublicValues.language.translate("ui.general.date")
            }));

            eventsListTable.setForeground(PublicValues.globalFontColor);
            eventsListTable.getTableHeader().setForeground(PublicValues.globalFontColor);


            eventsListTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if(e.getClickCount() == 2 && !SwingUtilities.isRightMouseButton(e) && eventsListTable.getSelectedRow() != -1) {
                        new Thread(() -> {
                            try {
                                backButton.setVisible(true);
                                eventsListTableScrollPane.setVisible(false);
                                eventView.set(new ArtistEventView(UnofficialSpotifyAPI.getConcert(eventsList.get(eventsListTable.getSelectedRow()))));
                                alternateTablesTableContainer.add(eventView.get());
                                contentPanel.setBorder(new LineBorder(Color.GRAY, 1));
                                contentPanel.revalidate();
                                contentPanel.repaint();
                                isOnTicketView.set(true);
                            } catch (IOException ex) {
                                ConsoleLogging.Throwable(ex);
                            }
                        }).start();
                    }
                }
            });

            for(ConcertsOuterClass.Concerts.UNKNContainer unknContainer : container.getArtistContainerList()) {
                for(ConcertsOuterClass.Concerts.ArtistsContainer artistContainer : unknContainer.getArtistsList()) {
                    if(artistContainer.hasArtist()) {
                        ConcertsOuterClass.Concerts.Artist artist = artistContainer.getArtist();

                        concertsMap.put(artist.getName(), artist.getArtistConcerts().getConcertsList());

                        eventsTable.addModifyAction(new Runnable() {
                            @Override
                            public void run() {
                                ((DefaultTableModel) eventsTable.getModel()).addRow(new Object[]{
                                        artist.getName(),
                                        artist.getArtistConcerts().getConcertsList().size() + " " + PublicValues.language.translate("ui.browse.events.events")
                                });
                            }
                        });
                    }else if(artistContainer.hasConcert()) {
                        ConcertsOuterClass.Concerts.Concert concert = artistContainer.getConcert();

                        concertsMap.put(concert.getArtist(), new ArrayList<ConcertsOuterClass.Concerts.ArtistConcertConcert>() {{
                            add(ConcertsOuterClass.Concerts.ArtistConcertConcert.newBuilder()
                                    .setDate(concert.getDate())
                                    .setArtist(concert.getArtist())
                                    .setConcertUri(concert.getConcertUri())
                                    .setLocationName(concert.getLocationName())
                                    .build());
                        }});

                        eventsTable.addModifyAction(new Runnable() {
                            @Override
                            public void run() {
                                ((DefaultTableModel) eventsTable.getModel()).addRow(new Object[]{
                                        concert.getArtist(),
                                        concert.getLocationName() + " • " + formatDate(parseDate(concert.getDate().getTime()))
                                });
                            }
                        });
                    }else {
                        ConsoleLogging.warning("[BrowsePanel events] Got artist container that doesn't have recognized content");
                    }
                }
            }

            builder.addComponent(new SpotifySectionPanel.ViewDescriptorComponent(
                    container.getDescription().getText(),
                    contentPanel
            ));
        }

        return builder;
    }

    private OffsetDateTime parseDate(String date) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        return OffsetDateTime.parse(date, formatter);
    }

    private String formatDate(OffsetDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM HH:mm");
        return date.format(formatter);
    }

    private SpotifySectionPanel.ViewDescriptorBuilder browseSectionToViewDescriptor(UnofficialSpotifyAPI.SpotifyBrowseSection section) {
        SpotifySectionPanel.ViewDescriptorBuilder builder = new SpotifySectionPanel.ViewDescriptorBuilder();

        builder.setTitle(section.getHeader());

        ArrayList<Integer> skip = new ArrayList<>();

        for(int i = 0; i < section.getBody().size(); i++) {
            if(skip.contains(i)) {
                continue;
            }
            UnofficialSpotifyAPI.SpotifyBrowseEntry entry = section.getBody().get(i);
            if(entry.getComponent().getId().contains("carousel")) {
                builder.addComponent(new SpotifySectionPanel.ViewDescriptorComponent(
                        entry.getText().getTitle(),
                        new SpotifyBrowseSection(entry.getChildren().get())
                ));
            }
            if(entry.getComponent().getCategory().contains("card") && !entry.getChildren().isPresent()) {
                ArrayList<ArrayList<String>> entries = new ArrayList<>();

                for(int j = 0; j < section.getBody().subList(i, section.getBody().size()).size(); j++) {
                    UnofficialSpotifyAPI.SpotifyBrowseEntry cardEntry = section.getBody().subList(i, section.getBody().size()).get(j);
                    if(cardEntry.getComponent().getCategory().contains("card")) {
                        entries.add(new ArrayList<>(Arrays.asList(cardEntry.getText().getTitle(), cardEntry.getText().getDescription().orElse(""), cardEntry.getText().getSubtitle().orElse(""), cardEntry.getEvents().get().getEvents().get(0).getData_uri().get().getUri())));
                        skip.add(i + j);
                    } else {
                        break;
                    }
                }

                if(i == 0) {
                    builder.addComponent(new SpotifySectionPanel.ViewDescriptorComponent(
                            "",
                            new SpotifyBrowseSection(entries)
                    ));
                }else {
                    builder.addComponent(new SpotifySectionPanel.ViewDescriptorComponent(
                            section.getBody().get(i-1).getText().getTitle(),
                            new SpotifyBrowseSection(entries)
                    ));
                }
            }
        }

        return builder;
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

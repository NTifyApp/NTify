/*
 * Copyright [2025] [Gianluca Beil]
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
package com.spotifyxp.guielements;

import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.protogens.Concert;
import com.spotifyxp.swingextension.JImagePanel;
import com.spotifyxp.utils.ConnectionUtils;

import javax.swing.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;


public class ArtistEventView extends JPanel {
    private final JLabel genresLabel;
    private final JTextField genres;
    private final JImagePanel companyImage;
    private final JLabel companyName;
    private final JButton viewEvent;
    private final JTextArea disclaimer;
    private final JLabel time;
    private final JLabel location;
    private final JButton viewOnAMaps;
    private final JButton viewOnGMaps;

    private Concert.ConcertResponse.TicketInfo ticketInfo = null;
    private Concert.ConcertResponse.VenueInfo venueInfo = null;
    private String googleMaps = null;
    private String appleMaps = null;
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public ArtistEventView(Concert.ConcertResponse concert) throws IllegalArgumentException {
        setLayout(null);
        setSize(770, 290);

        for(Concert.ConcertResponse.Section section : concert.getSectionsList()) {
            if(section.hasTicketInfo()) {
                ticketInfo = section.getTicketInfo();
            }
            if(section.hasVenueInfo()) {
                venueInfo = section.getVenueInfo();
            }
        }

        if(ticketInfo == null) {
            JOptionPane.showMessageDialog(ContentPanel.frame,
                    PublicValues.language.translate("ui.artisteventview.error.noticketinfo"),
                    PublicValues.language.translate("ui.general.error"),
                    JOptionPane.ERROR_MESSAGE);
            throw new IllegalArgumentException("No ticket info found");
        }

        if(venueInfo == null) {
            JOptionPane.showMessageDialog(ContentPanel.frame,
                    PublicValues.language.translate("ui.artisteventview.error.novenueinfo"),
                    PublicValues.language.translate("ui.general.warning"),
                    JOptionPane.WARNING_MESSAGE);
        }

        genresLabel = new JLabel(PublicValues.language.translate("ui.artisteventview.genres"));
        genresLabel.setBounds(12, 12, 60, 17);
        add(genresLabel);

        genresLabel.setForeground(PublicValues.globalFontColor);

        genres = new JTextField();
        genres.setEditable(false);
        genres.setBounds(12, 41, 686, 21);
        add(genres);

        ArrayList<String> genresList = new ArrayList<>();
        for(Concert.ConcertResponse.Genre genre : concert.getConcertInfo().getGenresList()) {
            genresList.add(genre.getName());
        }
        genres.setText(String.join(", ", genresList));

        companyImage = new JImagePanel();
        companyImage.setBounds(12, 80, 37, 37);
        add(companyImage);

        try {
            companyImage.setImage(new URL(ticketInfo.getTicketServiceInfo().getCompanyBranding()));
        } catch (MalformedURLException e) {
            ConsoleLogging.Throwable(e);
        }

        companyName = new JLabel("Company name 1234");
        companyName.setBounds(58, 91, 186, 17);
        add(companyName);

        companyName.setText(ticketInfo.getTicketServiceInfo().getCompanyName());
        companyName.setForeground(PublicValues.globalFontColor);

        viewEvent = new JButton(PublicValues.language.translate("ui.artisteventview.viewevent"));
        viewEvent.setBounds(282, 86, 106, 27);
        add(viewEvent);

        viewEvent.setForeground(PublicValues.globalFontColor);
        viewEvent.addActionListener(e -> {
            try {
                ConnectionUtils.openBrowser(ticketInfo.getTicketServiceInfo().getBookUrl());
            } catch (URISyntaxException | IOException ex) {
                ConsoleLogging.Throwable(ex);
            }
        });

        disclaimer = new JTextArea();
        disclaimer.setBounds(12, 129, 486, 42);
        add(disclaimer);

        disclaimer.setEditable(false);
        disclaimer.setText(ticketInfo.getDisclaimer());

        time = new JLabel("");
        time.setBounds(12, 183, 686, 33);
        add(time);

        time.setForeground(PublicValues.globalFontColor);
        if(venueInfo != null) time.setText(PublicValues.language.translate("ui.general.date") + ": " + parseDate(venueInfo.getDateInfo().getDate().getTime()).format(formatter));

        location = new JLabel("");
        location.setBounds(500, 91, 209, 17);
        add(location);

        location.setHorizontalAlignment(SwingConstants.CENTER);
        location.setForeground(PublicValues.globalFontColor);
        if(venueInfo != null) location.setText(PublicValues.language.translate("ui.general.location") + ": " + venueInfo.getVenue().getVenueName());

        if(venueInfo != null) {
            for(Concert.ConcertResponse.VenueMapService service : venueInfo.getVenue().getMapServicesList()) {
                if(service.getUrl().toLowerCase().contains("https://www.google.com/maps")) {
                    googleMaps = service.getUrl();
                }
                if(service.getUrl().toLowerCase().contains("https://maps.apple.com")) {
                    appleMaps = service.getUrl();
                }
            }
        }

        viewOnAMaps = new JButton(PublicValues.language.translate("ui.artisteventview.viewonamaps"));
        viewOnAMaps.setBounds(500, 121, 209, 27);
        add(viewOnAMaps);

        viewOnAMaps.setForeground(PublicValues.globalFontColor);
        viewOnAMaps.addActionListener(e -> {
            if(venueInfo == null) return;
            if(googleMaps == null) return;

            try {
                ConnectionUtils.openBrowser(appleMaps);
            } catch (URISyntaxException | IOException ex) {
                ConsoleLogging.Throwable(ex);
            }
        });

        viewOnGMaps = new JButton(PublicValues.language.translate("ui.artisteventview.viewongmaps"));
        viewOnGMaps.setBounds(500, 161, 209, 27);
        add(viewOnGMaps);

        viewOnGMaps.setForeground(PublicValues.globalFontColor);
        viewOnGMaps.addActionListener(e -> {
            if(venueInfo == null) return;
            if(googleMaps == null) return;

            try {
                ConnectionUtils.openBrowser(googleMaps);
            } catch (URISyntaxException | IOException ex) {
                ConsoleLogging.Throwable(ex);
            }
        });
    }

    private OffsetDateTime parseDate(String date) throws DateTimeParseException {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
        return OffsetDateTime.parse(date, formatter);
    }
}

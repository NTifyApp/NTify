/*
 * Copyright [2025-2026] [Gianluca Beil]
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
import com.spotifyxp.spotapi.pojos.ConcertDetailsResponse;
import com.spotifyxp.swingextension.JImagePanel;
import com.spotifyxp.utils.Utils;

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

    private ConcertDetailsResponse.Offer ticketInfo = null;
    private ConcertDetailsResponse.Venue venueInfo = null;
    private String googleMaps = null;
    private String appleMaps = null;
    public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public ArtistEventView(ConcertDetailsResponse concertResponse) throws IllegalArgumentException {
        setLayout(null);
        setSize(770, 290);

        ConcertDetailsResponse.Concert concert = concertResponse.getConcert();

        if(concert.getOffers() != null
                && concert.getOffers().getItems() != null
                && !concert.getOffers().getItems().isEmpty()) {
            ticketInfo = concert.getOffers().getItems().get(0);
        }
        venueInfo = concert.getVenue();

        if(ticketInfo == null) {
            JOptionPane.showMessageDialog(ContentPanel.frame,
                    PublicValues.language.translate("artist_event_view.dialogs.no_ticket_info.message"),
                    PublicValues.language.translate("general.error"),
                    JOptionPane.ERROR_MESSAGE);
            throw new IllegalArgumentException("No ticket info found");
        }

        if(venueInfo == null) {
            JOptionPane.showMessageDialog(ContentPanel.frame,
                    PublicValues.language.translate("artist_event_view.dialogs.no_venue_info.message"),
                    PublicValues.language.translate("general.warning"),
                    JOptionPane.WARNING_MESSAGE);
        }

        genresLabel = new JLabel(PublicValues.language.translate("general.genres"));
        genresLabel.setBounds(12, 12, 60, 17);
        add(genresLabel);

        genresLabel.setForeground(PublicValues.globalFontColor);

        genres = new JTextField();
        genres.setEditable(false);
        genres.setBounds(12, 41, 686, 21);
        add(genres);

        ArrayList<String> genresList = new ArrayList<>();
        if(concert.getConcepts() != null && concert.getConcepts().getItems() != null) {
            for(ConcertDetailsResponse.ConceptItem concept : concert.getConcepts().getItems()) {
                genresList.add(concept.getData().getName());
            }
        }
        genres.setText(String.join(", ", genresList));

        companyImage = new JImagePanel();
        companyImage.setBounds(12, 80, 37, 37);
        add(companyImage);

        try {
            if(ticketInfo.getProviderImageUrl() != null) {
                companyImage.setImage(new URL(ticketInfo.getProviderImageUrl()));
            }
        } catch (MalformedURLException e) {
            ConsoleLogging.Throwable(e);
        }

        companyName = new JLabel("Company name 1234");
        companyName.setBounds(58, 91, 186, 17);
        add(companyName);

        companyName.setText(ticketInfo.getProviderName());
        companyName.setForeground(PublicValues.globalFontColor);

        viewEvent = new JButton(PublicValues.language.translate("artist_event_view.view_event"));
        viewEvent.setBounds(282, 86, 106, 27);
        add(viewEvent);

        viewEvent.setForeground(PublicValues.globalFontColor);
        viewEvent.addActionListener(e -> {
            try {
                Utils.openBrowser(ticketInfo.getUrl());
            } catch (URISyntaxException | IOException ex) {
                ConsoleLogging.Throwable(ex);
            }
        });

        disclaimer = new JTextArea();
        disclaimer.setBounds(12, 129, 486, 42);
        add(disclaimer);

        disclaimer.setEditable(false);
        disclaimer.setText(concert.getAgeRestriction());

        time = new JLabel("");
        time.setBounds(12, 183, 686, 33);
        add(time);

        time.setForeground(PublicValues.globalFontColor);
        if(concert.getStartDateIsoString() != null) time.setText(PublicValues.language.translate("general.date") + ": " + parseDate(concert.getStartDateIsoString()).format(formatter));

        location = new JLabel("");
        location.setBounds(500, 91, 209, 17);
        add(location);

        location.setHorizontalAlignment(SwingConstants.CENTER);
        location.setForeground(PublicValues.globalFontColor);
        if(venueInfo != null) location.setText(PublicValues.language.translate("general.location") + ": " + venueInfo.getName());

        if(concert.getLocation() != null && concert.getLocation().getCoordinates() != null) {
            double lat = concert.getLocation().getCoordinates().getLatitude();
            double lon = concert.getLocation().getCoordinates().getLongitude();
            googleMaps = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
            appleMaps = "https://maps.apple.com/?q=" + lat + "," + lon;
        }

        viewOnAMaps = new JButton(PublicValues.language.translate("artist_event_view.view_on_apple_maps"));
        viewOnAMaps.setBounds(500, 121, 209, 27);
        add(viewOnAMaps);

        viewOnAMaps.setForeground(PublicValues.globalFontColor);
        viewOnAMaps.addActionListener(e -> {
            if(venueInfo == null) return;
            if(googleMaps == null) return;

            try {
                Utils.openBrowser(appleMaps);
            } catch (URISyntaxException | IOException ex) {
                ConsoleLogging.Throwable(ex);
            }
        });

        viewOnGMaps = new JButton(PublicValues.language.translate("artist_event_view.view_on_google_maps"));
        viewOnGMaps.setBounds(500, 161, 209, 27);
        add(viewOnGMaps);

        viewOnGMaps.setForeground(PublicValues.globalFontColor);
        viewOnGMaps.addActionListener(e -> {
            if(venueInfo == null) return;
            if(googleMaps == null) return;

            try {
                Utils.openBrowser(googleMaps);
            } catch (URISyntaxException | IOException ex) {
                ConsoleLogging.Throwable(ex);
            }
        });
    }

    private OffsetDateTime parseDate(String date) throws DateTimeParseException {
        return OffsetDateTime.parse(date);
    }
}

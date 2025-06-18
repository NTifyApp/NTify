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
package com.spotifyxp.utils;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.deps.com.spotify.context.ContextTrackOuterClass;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.IPlaylistItem;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.*;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.util.ArrayList;

@SuppressWarnings({"SameReturnValue", "IntegerDivisionInFloatingPointContext", "BooleanMethodIsAlwaysInverted"})
public class TrackUtils {
    public static String calculateFileSizeKb(Track t) {
        return calculateFileSizeKb(t.getDurationMs());
    }

    public static String calculateFileSizeKb(long milliseconds) {
        long minutes = getMMofTrack(milliseconds);
        //720kb per minute if normal 96kbps
        //1200kb per minute if high 160kbps
        //2400kb per minute if extremely high 320kbps
        String toret = "";
        switch (PublicValues.quality) {
            case NORMAL:
                toret = String.valueOf(minutes * 720);
                break;
            case HIGH:
                toret = String.valueOf(minutes * 1200);
                break;
            case VERY_HIGH:
                toret = String.valueOf(minutes * 2400);
                break;
        }
        if (toret.isEmpty() || toret.equals("0")) {
            toret = "N/A";
        }
        return toret + " KB";
    }

    public static String calculateFileSizeKb(TrackSimplified t) {
        return calculateFileSizeKb(t.getDurationMs());
    }

    public static long getMMofTrack(long milliseconds) {
        return milliseconds / 60000;
    }

    public static String getHHMMSSOfTrack(long milliseconds) {
        int seconds = Math.round(milliseconds / 1000);
        int hh = seconds / 3600;
        int mm = (seconds % 3600) / 60;
        int ss = seconds % 60;
        String formattedTime = String.format("%02d:%02d", mm, ss);
        if (hh > 0) {
            formattedTime = String.format("%02d:%s", hh, formattedTime);
        }
        return formattedTime;
    }

    public static void addAllToQueue(ArrayList<String> cache, DefTable addintable) {
        if(PublicValues.config.getBoolean(ConfigValues.disable_autoqueue.name)) {
            return;
        }
        try {
            try {
                InstanceManager.getPlayer().getPlayer().clearQueue();
            } catch (Exception exc) {
                ConsoleLogging.warning("Couldn't queue tracks");
                ConsoleLogging.Throwable(exc);
                return;
            }
            int counter = 0;
            try {
                ArrayList<ContextTrackOuterClass.ContextTrack> tracks = new ArrayList<>();
                for (String s : cache) {
                    if (!(counter == addintable.getSelectedRow() + 1)) {
                        counter++;
                        continue;
                    }
                    if (counter == addintable.getRowCount()) {
                        break; //User is on the last song
                    }
                    tracks.add(ContextTrackOuterClass.ContextTrack.newBuilder().setUri(s).build());
                }
                InstanceManager.getPlayer().getPlayer().setQueue(tracks);
            } catch (ArrayIndexOutOfBoundsException exception) {
                GraphicalMessage.bug("TrackUtils line 112");
            }
            InstanceManager.getPlayer().getPlayer().setShuffle(PublicValues.shuffle);
            if (PublicValues.shuffle) {
                Shuffle.makeShuffle();
            }
            Events.triggerEvent(SpotifyXPEvents.queueUpdate.getName());
        } catch (IndexOutOfBoundsException ignored) {
        }
    }

    public static Integer roundVolumeToNormal(float volume) {
        int vol = (Math.round(volume * 10)) * 6754;
        if(vol > 65536) vol = 65536;
        return vol;
    }

    public static int getSecondsFromMS(long milliseconds) {
        return Math.round(milliseconds / 1000);
    }

    public static String getBitrate() {
        switch (PublicValues.quality) {
            case NORMAL:
                return "96kbps";
            case HIGH:
                return "160kbps";
            case VERY_HIGH:
                return "320kbps";
        }
        return "Unknown (BUG)";
    }

    public static String getArtists(Metadata.Artist[] artists) {
        StringBuilder builder = new StringBuilder();
        for (Metadata.Artist artist : artists) {
            if (!(builder.length() == artists.length - 1)) {
                builder.append(artist.getName()).append(", ");
            } else {
                builder.append(artist.getName());
            }
        }
        return builder.toString();
    }

    public static String getArtists(ArtistSimplified[] artists) {
        StringBuilder builder = new StringBuilder();
        for (ArtistSimplified artist : artists) {
            if (!(builder.length() == artists.length - 1)) {
                builder.append(artist.getName()).append(", ");
            } else {
                builder.append(artist.getName());
            }
        }
        return builder.toString();
    }

    public static boolean trackHasArtist(ArtistSimplified[] artists, String tosearchfor, boolean ignoreCase) {
        for (ArtistSimplified artist : artists) {
            if (ignoreCase) {
                if (artist.getName().equalsIgnoreCase(tosearchfor)) {
                    return true;
                }
            } else {
                if (artist.getName().equals(tosearchfor)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isTrackLiked(String id) {
        try {
            return InstanceManager.getSpotifyApi().checkUsersSavedTracks(id).build().execute()[0];
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
            return false;
        }
    }

    public static Runnable initializeLazyLoadingForPlaylists(
            JScrollPane scrollPane,
            ArrayList<String> uricache,
            DefTable table, 
            int[] visibleCount,
            String playlistId,
            boolean[] inProg,
            boolean loadnew) {
        if (loadnew) {
            uricache.clear();
            ((DefaultTableModel) table.getModel()).setRowCount(0);
        }
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        try {
            int count = 0;
            int songTableHeight = scrollPane.getHeight();
            int limitTrackHeight = table.getRowHeight() * visibleCount[0];
            while (songTableHeight > limitTrackHeight) {
                limitTrackHeight = table.getRowHeight() * (visibleCount[0] + 1);
            }
            for (PlaylistTrack track : InstanceManager.getSpotifyApi().getPlaylistsItems(playlistId).limit(visibleCount[0]).build().execute().getItems()) {
                if (!uricache.contains(track.getTrack().getUri())) {
                    String a = TrackUtils.getArtists(InstanceManager.getSpotifyApi().getTrack(track.getTrack().getId()).build().execute().getArtists());
                    model.insertRow(count, new Object[]{track.getTrack().getName() + " - " + a, TrackUtils.calculateFileSizeKb(track.getTrack()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getTrack().getDurationMs())});
                    uricache.add(count, track.getTrack().getUri());
                    count++;
                }
            }
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
        }
        Runnable deinitRunnable = () -> {};
        //ToDo: Add parameter for storing if the function should add a mouse listener
        if (scrollPane.getName() == null || !scrollPane.getName().equals("MouseListenerTrackUtilsActive")) { // <- This bad
            scrollPane.setName("MouseListenerTrackUtilsActive"); // <- This bad
            MouseWheelListener listener = new MouseWheelListener() {
                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if (!inProg[0]) {
                        inProg[0] = true;
                        BoundedRangeModel m = scrollPane.getVerticalScrollBar().getModel();
                        int extent = m.getExtent();
                        int maximum = m.getMaximum();
                        int value = m.getValue();
                        if (value + extent >= maximum / 4) {
                            if (scrollPane.isVisible()) {
                                Thread thread = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            int counter = 0;
                                            int last = 0;
                                            int parsed = 0;
                                            while (parsed != visibleCount[0]) {
                                                PlaylistTrack[] track;
                                                Paging<PlaylistTrack> playlist = InstanceManager.getSpotifyApi().getPlaylistsItems(playlistId).limit(visibleCount[0]).offset(table.getRowCount()).build().execute();
                                                if (playlist.getTotal() <= uricache.size()) {
                                                    return;
                                                }
                                                track = playlist.getItems();
                                                for (PlaylistTrack t : track) {
                                                    uricache.add(t.getTrack().getUri());
                                                    String a = TrackUtils.getArtists(InstanceManager.getSpotifyApi().getTrack(t.getTrack().getId()).build().execute().getArtists());
                                                    table.addModifyAction(() -> ((DefaultTableModel) table.getModel()).addRow(new Object[]{t.getTrack().getName() + " - " + a, TrackUtils.calculateFileSizeKb(t.getTrack()), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(t.getTrack().getDurationMs())}));
                                                    parsed++;
                                                }
                                                if (parsed == last) {
                                                    if (counter > 1) {
                                                        break;
                                                    }
                                                    counter++;
                                                } else {
                                                    counter = 0;
                                                }
                                                last = parsed;
                                            }
                                            inProg[0] = false;
                                        } catch (Exception e) {
                                            ConsoleLogging.error("Error loading playlist tracks!");
                                            inProg[0] = false;
                                            throw new RuntimeException(e);
                                        }
                                    }
                                }, "Lazy load next");
                                thread.start();
                            }
                        }
                    }
                }
            };
            scrollPane.addMouseWheelListener(listener);
            deinitRunnable = () -> {
                scrollPane.removeMouseWheelListener(listener);
                scrollPane.setName("");
            };
        }
        return deinitRunnable;
    }

    public static String calculateFileSizeKb(IPlaylistItem track) {
        return calculateFileSizeKb(track.getDurationMs());
    }
}

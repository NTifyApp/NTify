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
package com.spotifyxp.utils;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.deps.com.spotify.context.ContextTrackOuterClass;
import com.spotifyxp.deps.com.spotify.metadata.Metadata;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"SameReturnValue", "IntegerDivisionInFloatingPointContext", "BooleanMethodIsAlwaysInverted"})
public class TrackUtils {
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
        if(PublicValues.config.getFields().disableAutoQueue) {
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

    public static String getArtists(List<Metadata.Artist> artists) {
        StringBuilder builder = new StringBuilder();
        for (Metadata.Artist artist : artists) {
            if (!(builder.length() == artists.size() - 1)) {
                builder.append(artist.getName()).append(", ");
            } else {
                builder.append(artist.getName());
            }
        }
        return builder.toString();
    }

    public static boolean trackHasArtist(String[] artists, String tosearchfor, boolean ignoreCase) {
        for (String artist : artists) {
            if (ignoreCase) {
                if (artist.equalsIgnoreCase(tosearchfor)) {
                    return true;
                }
            } else {
                if (artist.equals(tosearchfor)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isTrackLiked(String id) {
        try {
            return PublicValues.session.api().user().isInLibrary(new String[] {"spotify:track:" + id})[0];
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
            return false;
        }
    }
}

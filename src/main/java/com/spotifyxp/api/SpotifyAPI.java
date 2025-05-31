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
package com.spotifyxp.api;

import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Artist;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Track;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.guielements.DefTable;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.TrackUtils;

import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;

public class SpotifyAPI {
    public SpotifyAPI() {
        Events.subscribe(SpotifyXPEvents.apikeyrefresh.getName(), data -> {
            if (InstanceManager.getSpotifyPlayer() == null) return;
            InstanceManager.getPkce().refresh();
        });
    }

    /**
     * Adds all albums to the table specified
     *
     * @param uricache cache that holds all uris
     * @param fromuri  artist uri
     * @param totable  the table to store all albums found
     */
    public void addAllAlbumsToList(ArrayList<String> uricache, String fromuri, DefTable totable) {
        Thread thread = new Thread(() -> {
            try {
                int offset = 0;
                int limit = 50;
                int parsed = 0;
                int total = InstanceManager.getSpotifyApi().getArtistsAlbums(fromuri.split(":")[2]).build().execute().getTotal();
                int counter = 0;
                int last = 0;
                while (parsed != total) {
                    for (AlbumSimplified album : InstanceManager.getSpotifyApi().getArtistsAlbums(fromuri.split(":")[2]).offset(offset).limit(limit).build().execute().getItems()) {
                        totable.addModifyAction(() -> ((DefaultTableModel) totable.getModel()).addRow(new Object[]{album.getName()}));
                        uricache.add(album.getUri());
                        parsed++;
                    }
                    if (last == parsed) {
                        if (counter > 1) {
                            break;
                        }
                        counter++;
                    } else {
                        counter = 0;
                    }
                    last = parsed;
                    offset += limit;
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, "Add albums to list");
        thread.start();
    }

    /**
     * Adds an album to a table
     *
     * @param artist instance of Artist
     * @param table  table to store the album
     * @see Artist
     */
    public void addArtistToList(Artist artist, DefTable table) {
        table.addModifyAction(() -> ((DefaultTableModel) table.getModel()).addRow(new Object[]{artist.getName()}));
    }

    /**
     * Adds a song to a table
     *
     * @param artists artists to insert (for display)
     * @param track   instance of Track
     * @param table   table to store the song
     * @see Track
     */
    public void addSongToList(String artists, Track track, DefTable table) {
        table.addModifyAction(() -> ((DefaultTableModel) table.getModel()).addRow(new Object[]{track.getName() + " - " + track.getAlbum().getName() + " - " + artists, TrackUtils.calculateFileSizeKb(track), TrackUtils.getBitrate(), TrackUtils.getHHMMSSOfTrack(track.getDurationMs())}));
    }

    /**
     * Adds a playlist to a table
     *
     * @param simplified instance of PlaylistSimplified
     * @param table      table to store the playlist
     * @see PlaylistSimplified
     */
    public void addPlaylistToList(PlaylistSimplified simplified, DefTable table) {
        table.addModifyAction(() -> ((DefaultTableModel) table.getModel()).addRow(new Object[]{simplified.getName() + " - " + simplified.getOwner().getDisplayName()}));
    }
}

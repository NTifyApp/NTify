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

import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.AlbumSimplified;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.EpisodeSimplified;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Image;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.TrackSimplified;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;

import java.util.ArrayList;

public class SpotifyUtils {
    public static Image getImageForSystem(Image[] images) {
        if (SystemUtils.getUsableRAMmb() < 512) {
            for (Image i : images) {
                if (i.getWidth() == 64) {
                    return i;
                }
            }
            ConsoleLogging.warning("Can't get the right image for the system ram! Using the default one");
        }
        return images[0];
    }

    public static ArrayList<TrackSimplified> getAllTracksAlbum(String uri) {
        ArrayList<TrackSimplified> tracks = new ArrayList<>();
        try {
            int offset = 0;
            int limit = 50;
            int parsed = 0;
            int total = InstanceManager.getSpotifyApi().getAlbumsTracks(uri.split(":")[2]).build().execute().getTotal();
            int counter = 0;
            int last = 0;
            while (parsed != total) {
                for (TrackSimplified track : InstanceManager.getSpotifyApi().getAlbumsTracks(uri.split(":")[2]).offset(offset).limit(limit).build().execute().getItems()) {
                    tracks.add(track);
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
        return tracks;
    }

    /*public static ArrayList<PlaylistTrack> getAllTracksPlaylist(String uri) {
        ArrayList<PlaylistTrack> tracks = new ArrayList<>();
        try {
            int offset = 0;
            int limit = 50;
            int parsed = 0;
            int total = InstanceManager.getSpotifyApi().getPlaylistsItems(uri.split(":")[2]).build().execute().getTotal();
            int counter = 0;
            int last = 0;
            while (parsed != total) {
                for (PlaylistTrack track : InstanceManager.getSpotifyApi().getPlaylistsItems(uri.split(":")[2]).offset(offset).limit(limit).build().execute().getItems()) {
                    tracks.add(track);
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
        return tracks;
    }*/

    public static ArrayList<EpisodeSimplified> getAllEpisodesShow(String uri) {
        ArrayList<EpisodeSimplified> episodes = new ArrayList<>();
        try {
            int offset = 0;
            int limit = 50;
            int parsed = 0;
            int total = InstanceManager.getSpotifyApi().getShowEpisodes(uri.split(":")[2]).build().execute().getTotal();
            int counter = 0;
            int last = 0;
            while (parsed != total) {
                for (EpisodeSimplified episode : InstanceManager.getSpotifyApi().getShowEpisodes(uri.split(":")[2]).offset(offset).limit(limit).build().execute().getItems()) {
                    episodes.add(episode);
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
        return episodes;
    }

    public static ArrayList<AlbumSimplified> getAllAlbumsArtist(String uri) {
        ArrayList<AlbumSimplified> albums = new ArrayList<>();
        try {
            int offset = 0;
            int limit = 50;
            int parsed = 0;
            int total = InstanceManager.getSpotifyApi().getArtistsAlbums(uri.split(":")[2]).build().execute().getTotal();
            int counter = 0;
            int last = 0;
            while (parsed != total) {
                for (AlbumSimplified album : InstanceManager.getSpotifyApi().getArtistsAlbums(uri.split(":")[2]).offset(offset).limit(limit).build().execute().getItems()) {
                    albums.add(album);
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
        return albums;
    }

    /*public static ArrayList<Track> getAllTopTracksArtist(String uri) {
        ArrayList<Track> tracks = new ArrayList<>();
        try {
            int parsed = 0;
            int total = InstanceManager.getSpotifyApi().getArtistsTopTracks(uri.split(":")[2], PublicValues.countryCode).build().execute().length;
            int counter = 0;
            int last = 0;
            while (parsed != total) {
                for (Track track : InstanceManager.getSpotifyApi().getArtistsTopTracks(uri.split(":")[2], PublicValues.countryCode).build().execute()) {
                    tracks.add(track);
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
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return tracks;
    }*/
}

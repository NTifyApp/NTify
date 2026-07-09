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

import com.spotify.extendedmetadata.ExtendedMetadata;
import com.spotify.extendedmetadata.ExtensionKindOuterClass;
import com.spotify.metadata.Metadata;
import com.spotify.playlist4.Playlist4ApiProto;
import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import xyz.gianlu.librespot.api.ApiClient;
import xyz.gianlu.librespot.common.Utils;
import xyz.gianlu.librespot.core.TokenProvider;
import xyz.gianlu.librespot.metadata.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SpotifyUtils {

    public static Metadata.Image getImageForSystem(List<Metadata.Image> images) {
        if (SystemUtils.getUsableRAMmb() < 512) {
            for (Metadata.Image i : images) {
                if (i.getWidth() == 64) {
                    return i;
                }
            }
            ConsoleLogging.warning("Can't get the right image for the system ram! Using the default one");
        }
        return images.get(0);
    }

    public static ArrayList<Metadata.Track> getAllTracksAlbum(String uri) throws IOException, TokenProvider.TokenException {
        Metadata.Album album = PublicValues.session.api().album().getMetadata(AlbumId.fromUri(uri));
        ApiClient.BatchedRequestHelper batchedRequestHelper = new ApiClient.BatchedRequestHelper();
        ArrayList<Metadata.Track> tracks = new ArrayList<>();

        for (Metadata.Disc disc : album.getDiscList()) {
            for (Metadata.Track track : disc.getTrackList()) {
                batchedRequestHelper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                                .setEntityUri(TrackId.fromHex(Utils.bytesToHex(track.getGid())).toSpotifyUri())
                                .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                        .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.TRACK_V4)
                                        .build())
                        .build(), (data) -> {
                    tracks.add(Metadata.Track.parseFrom(data[0].getValue()));
                });
            }
        }

        batchedRequestHelper.execute(PublicValues.session.api(), ((exception, response) -> ConsoleLogging.Throwable(exception)));

        return tracks;
    }

    public static ArrayList<Metadata.Episode> getAllEpisodesShow(String uri) throws IOException, TokenProvider.TokenException {
        Metadata.Show show = PublicValues.session.api().show().getMetadata(ShowId.fromUri(uri));
        ApiClient.BatchedRequestHelper batchedRequestHelper = new ApiClient.BatchedRequestHelper();
        ArrayList<Metadata.Episode> episodes = new ArrayList<>();

        for (Metadata.Episode episode : show.getEpisodeList()) {
            batchedRequestHelper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                            .setEntityUri(EpisodeId.fromHex(Utils.bytesToHex(episode.getGid())).toSpotifyUri())
                            .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                    .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.EPISODE_V4)
                                    .build())
                    .build(), (data) -> {
                episodes.add(Metadata.Episode.parseFrom(data[0].getValue()));
            });
        }

        batchedRequestHelper.execute(PublicValues.session.api(), ((exception, response) -> ConsoleLogging.Throwable(exception)));

        return episodes;
    }

    public static ArrayList<TrackOrEpisode> getAllTracksPlaylist(String uri) throws IOException, TokenProvider.TokenException {
        Playlist4ApiProto.SelectedListContent listContent = PublicValues.session.api().playlist().get(PlaylistId.fromUri(uri));
        ApiClient.BatchedRequestHelper helper = new ApiClient.BatchedRequestHelper();
        ArrayList<TrackOrEpisode> tracks = new ArrayList<>();
        for (Playlist4ApiProto.Item item : listContent.getContents().getItemsList()) {
            switch (item.getUri().split(":")[1]) {
                case "track": {
                    helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                            .setEntityUri(item.getUri())
                            .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                    .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.TRACK_V4)
                                    .build())
                            .build(), data -> {
                        Metadata.Track track = Metadata.Track.parseFrom(data[0].getValue());
                        tracks.add(new TrackOrEpisode(track, null, true));
                    });
                    break;
                }
                case "episode": {
                    helper.addRequest(ExtendedMetadata.EntityRequest.newBuilder()
                            .setEntityUri(item.getUri())
                            .addQuery(ExtendedMetadata.ExtensionQuery.newBuilder()
                                    .setExtensionKind(ExtensionKindOuterClass.ExtensionKind.EPISODE_V4)
                                    .build())
                            .build(), data -> {
                        Metadata.Episode episode = Metadata.Episode.parseFrom(data[0].getValue());
                        tracks.add(new TrackOrEpisode(null, episode, false));
                    });
                    break;
                }
                default:
                    ConsoleLogging.warning("Unsupported playlist item type: " + item.getUri());
                    break;
            }
        }
        helper.execute(PublicValues.session.api(), ((exception, response) -> ConsoleLogging.Throwable(exception)));
        return tracks;
    }

    public static class TrackOrEpisode {
        public Metadata.Track track;
        public Metadata.Episode episode;
        public boolean isTrack;

        public TrackOrEpisode(Metadata.Track track, Metadata.Episode episode, boolean isTrack) {
            this.track = track;
            this.episode = episode;
            this.isTrack = isTrack;
        }
    }

    public static String formatMonthlyListeners(long monthlyListeners) {
        if(monthlyListeners >= 1_000_000) {
            return String.format(Locale.ENGLISH, "%.1fM", monthlyListeners / 1_000_000.0);
        } else if(monthlyListeners >= 1_000) {
            return String.format(Locale.ENGLISH, "%.1fK", monthlyListeners / 1_000.0);
        } else {
            return String.valueOf(monthlyListeners);
        }
    }
}

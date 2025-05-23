package com.spotifyxp.deps.se.michaelthelin.spotify.requests.data.player;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.spotifyxp.deps.se.michaelthelin.spotify.requests.data.AbstractDataRequest;

import java.io.IOException;

/**
 * Seeks to the given position in the user’s currently playing track.
 */
@JsonDeserialize(builder = SeekToPositionInCurrentlyPlayingTrackRequest.Builder.class)
public class SeekToPositionInCurrentlyPlayingTrackRequest extends AbstractDataRequest<String> {

    /**
     * The private {@link SeekToPositionInCurrentlyPlayingTrackRequest} constructor.
     *
     * @param builder A {@link SeekToPositionInCurrentlyPlayingTrackRequest.Builder}.
     */
    private SeekToPositionInCurrentlyPlayingTrackRequest(final Builder builder) {
        super(builder);
    }

    /**
     * Seek to a position in the user's currently playing track.
     *
     * @return A string. <b>Note:</b> This endpoint doesn't return something in its response body.
     * @throws IOException In case of networking issues.
     */
    public String execute() throws
            IOException {
        return putJson();
    }

    /**
     * Builder class for building a {@link SeekToPositionInCurrentlyPlayingTrackRequest}.
     */
    public static final class Builder extends AbstractDataRequest.Builder<String, Builder> {

        /**
         * Create a new {@link SeekToPositionInCurrentlyPlayingTrackRequest.Builder}.
         * <p>
         * Your access token must have the {@code user-modify-playback-state} scope authorized in order to control playback.
         *
         * @param accessToken Required. A valid access token from the Spotify Accounts service.
         * @see <a href="https://developer.spotify.com/web-api/using-scopes/">Spotify: Using Scopes</a>
         */
        public Builder(final String accessToken) {
            super(accessToken);
        }

        /**
         * The position setter.
         *
         * @param position_ms Required. The position in milliseconds to seek to. Must be a positive number. Passing in a
         *                    position that is greater than the length of the track will cause the player to start
         *                    playing the next song.
         * @return A {@link SeekToPositionInCurrentlyPlayingTrackRequest.Builder}.
         */
        public Builder position_ms(final Integer position_ms) {
            assert (position_ms != null);
            assert (position_ms >= 0);
            return setQueryParameter("position_ms", position_ms);
        }

        /**
         * The device ID setter.
         *
         * @param device_id Optional. The ID of the device this command is targeting. If not supplied, the
         *                  user's currently active device is the target.
         * @return A {@link SeekToPositionInCurrentlyPlayingTrackRequest.Builder}.
         * @see <a href="https://developer.spotify.com/web-api/user-guide/#spotify-uris-and-ids">Spotify: URIs &amp; IDs</a>
         */
        public Builder device_id(final String device_id) {
            assert (device_id != null);
            assert (!device_id.isEmpty());
            return setQueryParameter("device_id", device_id);
        }

        /**
         * The request build method.
         *
         * @return A custom {@link SeekToPositionInCurrentlyPlayingTrackRequest}.
         */
        @Override
        public SeekToPositionInCurrentlyPlayingTrackRequest build() {
            setContentType("application/json");
            setPath("/v1/me/player/seek");
            return new SeekToPositionInCurrentlyPlayingTrackRequest(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}

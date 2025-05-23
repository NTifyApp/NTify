package com.spotifyxp.deps.se.michaelthelin.spotify.requests.data.follow;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.spotifyxp.deps.se.michaelthelin.spotify.requests.data.AbstractDataRequest;

import java.io.IOException;

/**
 * Add the current user as a follower of a playlist.
 */
@JsonDeserialize(builder = FollowPlaylistRequest.Builder.class)
public class FollowPlaylistRequest extends AbstractDataRequest<String> {

    /**
     * The private {@link FollowPlaylistRequest} constructor.
     *
     * @param builder A {@link FollowPlaylistRequest.Builder}.
     */
    private FollowPlaylistRequest(final Builder builder) {
        super(builder);
    }

    /**
     * Follow a playlist.
     *
     * @return A string. <b>Note:</b> This endpoint doesn't return something in its response body.
     * @throws IOException In case of networking issues.
     */
    public String execute() throws
            IOException {
        return putJson();
    }

    /**
     * Builder class for building a {@link FollowPlaylistRequest}.
     */
    public static final class Builder extends AbstractDataRequest.Builder<String, Builder> {

        /**
         * Create a new {@link FollowPlaylistRequest} instance.
         * <p>
         * Following a playlist publicly requires authorization of the {@code playlist-modify-public} scope; following it
         * privately requires the {@code playlist-modify-private} scope.
         *
         * @param accessToken Required. A valid access token from the Spotify Accounts service.
         * @see <a href="https://developer.spotify.com/web-api/using-scopes/">Spotify: Using Scopes</a>
         */
        public Builder(final String accessToken) {
            super(accessToken);
        }

        /**
         * The playlist ID setter.
         *
         * @param playlist_id The Spotify ID of the playlist. Any playlist can be followed, regardless of its public/private
         *                    status, as long as you know its playlist ID.
         * @return A {@link FollowPlaylistRequest.Builder}.
         * @see <a href="https://developer.spotify.com/web-api/user-guide/#spotify-uris-and-ids">Spotify: URIs &amp; IDs</a>
         */
        public Builder playlist_id(final String playlist_id) {
            assert (playlist_id != null);
            assert (!playlist_id.isEmpty());
            return setPathParameter("playlist_id", playlist_id);
        }

        /**
         * The public following state setter.
         *
         * @param public_ Optional, default {@code true}. If {@code true} the playlist will be included in user's public
         *                playlists, if {@code false} it will remain private. To be able to follow playlists privately, the
         *                user must have granted the {@code playlist-modify-private} scope.
         * @return A {@link FollowPlaylistRequest.Builder}.
         */
        public Builder public_(final Boolean public_) {
            assert (public_ != null);
            return setBodyParameter("public", public_);
        }

        /**
         * The request build method.
         *
         * @return A custom {@link FollowPlaylistRequest}.
         */
        @Override
        public FollowPlaylistRequest build() {
            setContentType("application/json");
            setPath("/v1/playlists/{playlist_id}/followers");
            return new FollowPlaylistRequest(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}

package com.spotifyxp.deps.se.michaelthelin.spotify.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * Compare to [AlbumType] this field represents relationship between the artist and the album.
 */
public enum AlbumGroup {

    ALBUM("album"),
    APPEARS_ON("appears_on"),
    COMPILATION("compilation"),
    SINGLE("single");

    private static final Map<String, AlbumGroup> map = new HashMap<>();

    static {
        for (AlbumGroup albumGroup : AlbumGroup.values()) {
            map.put(albumGroup.group, albumGroup);
        }
    }

    public final String group;

    AlbumGroup(final String group) {
        this.group = group;
    }

    public static AlbumGroup keyOf(String type) {
        return map.get(type);
    }

    /**
     * Get the album group as a string.
     *
     * @return Album group as string.
     */
    public String getGroup() {
        return group;
    }

}

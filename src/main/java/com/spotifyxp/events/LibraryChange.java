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
package com.spotifyxp.events;

public class LibraryChange {
    public enum Type {
        ARTIST,
        ALBUM,
        SHOW,
        EPISODE,
        TRACK,
        PLAYLIST
    }

    public enum Action {
        REMOVE,
        ADD
    }

    private final Type type;
    private final Action action;
    private final String uri;

    public LibraryChange(String uri, Type type, Action action) {
        this.uri = uri;
        this.type = type;
        this.action = action;
    }

    public Type getType() {
        return type;
    }

    public Action getAction() {
        return action;
    }

    public String getUri() {
        return uri;
    }
}

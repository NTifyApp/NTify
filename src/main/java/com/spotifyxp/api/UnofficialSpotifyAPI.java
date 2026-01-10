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
package com.spotifyxp.api;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.logging.ConsoleLogging;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "unused"})
public class UnofficialSpotifyAPI {
    /**
     * Holds all the information of the lyrics for a track
     */
    public static class Lyrics {
        public String syncType = "";
        public final ArrayList<LyricsLine> lines = new ArrayList<>();
        public String language = "";
        public String providerDisplayName = "";
        public String provider = "";
        public String providerLyricsId = "";
    }

    /**
     * Holds all the information of a lyrics text line
     */
    public static class LyricsLine {
        public long startTimeMs = 0;
        public String words = "";
        public long endTimeMs = 0; //Usually 0
    }

    /**
     * Returns lyrics for a track
     *
     * @param uri uri of track
     * @return instance of Lyrics
     * @see Lyrics
     */
    public Lyrics getLyrics(String uri) {
        try {
            JsonObject lyricsRoot = PublicValues.session.api().track().getLyrics(uri, false).getAsJsonObject("lyrics");
            Lyrics lyrics = new Lyrics();
            lyrics.language = lyricsRoot.get("language").getAsString();
            lyrics.providerLyricsId = lyricsRoot.get("providerLyricsId").getAsString();
            lyrics.providerDisplayName = lyricsRoot.get("providerDisplayName").getAsString();
            lyrics.syncType = lyricsRoot.get("syncType").getAsString();
            for (JsonElement line : lyricsRoot.getAsJsonArray("lines")) {
                JsonObject l = line.getAsJsonObject();
                LyricsLine lyricsLine = new LyricsLine();
                lyricsLine.endTimeMs = Long.parseLong(l.get("endTimeMs").getAsString());
                lyricsLine.startTimeMs = Long.parseLong(l.get("startTimeMs").getAsString());
                lyricsLine.words = l.get("words").getAsString();
                lyrics.lines.add(lyricsLine);
            }
            return lyrics;
        } catch (IOException e) {
            return null;
        } catch (MercuryClient.MercuryException e) {
            ConsoleLogging.Throwable(e);
            return null;
        }
    }

    public enum HomeTabSectionTypes {
        HomeShortsSectionData,
        HomeGenericSectionData,
        HomeRecentlyPlayedSectionData,
        HomeSpotlightSectionData
    }

    public enum HomeTabSectionItemTypes {
        UnknownType,
        PlaylistResponseWrapper,
        ArtistResponseWrapper,
        AlbumResponseWrapper,
        EpisodeOrChapterResponseWrapper,
        PodcastOrAudiobookResponseWrapper
    }

    public static class HomeTabSectionItem {
        private final HomeTabSectionItemTypes type;
        private final String uri;
        private final Optional<HomeTabPlaylist> playlist;
        private final Optional<HomeTabArtist> artist;
        private final Optional<HomeTabAlbum> album;
        private final Optional<HomeTabEpisodeOrChapter> episodeOrChapter;

        public HomeTabSectionItem(
                HomeTabSectionItemTypes type,
                String uri,
                Optional<HomeTabPlaylist> playlist,
                Optional<HomeTabArtist> artist,
                Optional<HomeTabAlbum> album,
                Optional<HomeTabEpisodeOrChapter> episodeOrChapter
        ) {
            this.type = type;
            this.uri = uri;
            this.playlist = playlist;
            this.artist = artist;
            this.album = album;
            this.episodeOrChapter = episodeOrChapter;
        }

        public HomeTabSectionItemTypes getType() {
            return type;
        }

        public String getUri() {
            return uri;
        }

        public Optional<HomeTabPlaylist> getPlaylist() {
            return playlist;
        }

        public Optional<HomeTabArtist> getArtist() {
            return artist;
        }

        public Optional<HomeTabAlbum> getAlbum() {
            return album;
        }

        public Optional<HomeTabEpisodeOrChapter> getEpisodeOrChapter() {
            return episodeOrChapter;
        }

        public static HomeTabSectionItem fromJSON(JsonObject json) {
            Optional<HomeTabPlaylist> playlist = Optional.empty();
            Optional<HomeTabArtist> artist = Optional.empty();
            Optional<HomeTabAlbum> album = Optional.empty();
            Optional<HomeTabEpisodeOrChapter> episodeOrChapter = Optional.empty();
            switch (HomeTabSectionItemTypes.valueOf(json.getAsJsonObject("content").get("__typename").getAsString())) {
                case PlaylistResponseWrapper:
                    if(json.getAsJsonObject("content").getAsJsonObject("data").get("__typename").getAsString().equals("NotFound")) {
                        break;
                    }
                    if(json.getAsJsonObject("content").getAsJsonObject("data").get("__typename").getAsString().equals("GenericError")) break;
                    playlist = Optional.of(HomeTabPlaylist.fromJSON(json.getAsJsonObject("content").getAsJsonObject("data")));
                    break;
                case ArtistResponseWrapper:
                    if(json.getAsJsonObject("content").getAsJsonObject("data").get("__typename").getAsString().equals("NotFound")) {
                        break;
                    }
                    artist = Optional.of(HomeTabArtist.fromJSON(json.getAsJsonObject("content").getAsJsonObject("data")));
                    break;
                case AlbumResponseWrapper:
                    if(json.getAsJsonObject("content").getAsJsonObject("data").get("__typename").getAsString().equals("NotFound")) {
                        break;
                    }
                    album = Optional.of(HomeTabAlbum.fromJSON(json.getAsJsonObject("content").getAsJsonObject("data")));
                    break;
                case EpisodeOrChapterResponseWrapper:
                    if(json.getAsJsonObject("content").getAsJsonObject("data").get("__typename").getAsString().equals("NotFound")) {
                        break;
                    }
                    HomeTabEpisodeOrChapter homeTabEpisodeOrChapter = HomeTabEpisodeOrChapter.fromJSON(json.getAsJsonObject("content").getAsJsonObject("data"));
                    if(homeTabEpisodeOrChapter != null) episodeOrChapter = Optional.of(homeTabEpisodeOrChapter);
                    break;
                case UnknownType:
                    ConsoleLogging.warning("[HomeTab] Got section item with unknown type");
            }
            return new HomeTabSectionItem(
                    HomeTabSectionItemTypes.valueOf(json.getAsJsonObject("content").get("__typename").getAsString()),
                    json.get("uri").getAsString(),
                    playlist,
                    artist,
                    album,
                    episodeOrChapter
            );
        }
    }

    /**
     * Holds information about an HomeTab section
     */
    public static class HomeTabSection {
        private final HomeTabSectionTypes type;
        private final Optional<String> name;
        private final String uri;
        private final ArrayList<HomeTabSectionItem> items;

        public HomeTabSection(
                HomeTabSectionTypes type,
                Optional<String> name,
                String uri,
                ArrayList<HomeTabSectionItem> items
        ) {
            this.type = type;
            this.name = name;
            this.uri = uri;
            this.items = items;
        }

        public HomeTabSectionTypes getType() {
            return type;
        }

        public Optional<String> getName() {
            return name;
        }

        public String getUri() {
            return uri;
        }

        public ArrayList<HomeTabSectionItem> getItems() {
            return items;
        }

        public static HomeTabSection fromJSON(JsonObject json) {
            Optional<String> name = Optional.empty();
            if(json.getAsJsonObject("data").has("title")) {
                name = Optional.of(json.getAsJsonObject("data").getAsJsonObject("title").get("text").getAsString());
            }
            ArrayList<HomeTabSectionItem> items = new ArrayList<>();
            for(JsonElement element : json.getAsJsonObject("sectionItems").getAsJsonArray("items")) {
                items.add(HomeTabSectionItem.fromJSON(element.getAsJsonObject()));
            }
            return new HomeTabSection(
                    HomeTabSectionTypes.valueOf(json.getAsJsonObject("data").get("__typename").getAsString()),
                    name,
                    json.get("uri").getAsString(),
                    items
            );
        }
    }

    /**
     * Holds information about the HomeTab content
     */
    public static class HomeTab {
        private final String greeting;
        private final ArrayList<HomeTabSection> sections;

        private HomeTab(String greeting, ArrayList<HomeTabSection> sections) {
            this.greeting = greeting;
            this.sections = sections;
        }

        public String getGreeting() {
            return greeting;
        }

        public ArrayList<HomeTabSection> getSections() {
            return sections;
        }

        public static HomeTab fromJSON(JsonObject json) {
            ArrayList<HomeTabSection> sections = new ArrayList<>();
            for(JsonElement sectionObject : json.getAsJsonObject("sectionContainer").getAsJsonObject("sections").getAsJsonArray("items")) {
                try {
                    sections.add(HomeTabSection.fromJSON(sectionObject.getAsJsonObject()));
                }catch (Exception e) {
                    ConsoleLogging.warning("[HomeTab] Got section item with unknown/unsupported data");
                }
            }
            return new HomeTab(
                    json.getAsJsonObject("greeting").get("text").getAsString(),
                    sections
            );
        }
    }

    /**
     * Holds information about an HomeTab playlist
     */
    public static class HomeTabPlaylist {
        private final String name;
        private final String description;
        private final String uri;
        private final String ownerName;
        private final ArrayList<HomeTabImage> images;

        private HomeTabPlaylist(
                String name,
                String description,
                String uri,
                String ownerName,
                ArrayList<HomeTabImage> images
        ) {
            this.name = name;
            this.description = description;
            this.uri = uri;
            this.ownerName = ownerName;
            this.images = images;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getUri() {
            return uri;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public ArrayList<HomeTabImage> getImages() {
            return images;
        }

        public static HomeTabPlaylist fromJSON(JsonObject object) {
            ArrayList<HomeTabImage> images = new ArrayList<>();
            for(JsonElement image : object.getAsJsonObject("images").getAsJsonArray("items")) {
                JsonObject imageObject = image.getAsJsonObject();
                images.add(HomeTabImage.fromJSON(imageObject.getAsJsonArray("sources").get(0).getAsJsonObject()));
            }
            return new HomeTabPlaylist(
                    object.get("name").getAsString(),
                    object.get("description").getAsString(),
                    object.get("uri").getAsString(),
                    object.getAsJsonObject("ownerV2").getAsJsonObject("data").get("name").getAsString(),
                    images
            );
        }
    }

    /**
     * Holds information about an HomeTab image
     */
    public static class HomeTabImage {
        private final int width;
        private final int height;
        private final String url;

        private HomeTabImage(
                int width,
                int height,
                String url
        ) {
            this.width = width;
            this.height = height;
            this.url = url;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public String getUrl() {
            return url;
        }

        public static HomeTabImage fromJSON(JsonObject json) {
            return new HomeTabImage(
                    optionalJson(json, "width", new JsonPrimitive(-1)).getAsInt(),
                    optionalJson(json, "height", new JsonPrimitive(-1)).getAsInt(),
                    json.get("url").getAsString()
            );
        }
    }

    /**
     * Holds information about an HomeTab artist
     */
    public static class HomeTabArtist {
        private final String name;
        private final String uri;
        private final ArrayList<HomeTabImage> images;

        private HomeTabArtist(String name, String uri, ArrayList<HomeTabImage> images) {
            this.name = name;
            this.uri = uri;
            this.images = images;
        }

        public String getName() {
            return name;
        }

        public String getUri() {
            return uri;
        }

        public ArrayList<HomeTabImage> getImages() {
            return images;
        }

        public static HomeTabArtist fromJSON(JsonObject json) {
            ArrayList<HomeTabImage> images = new ArrayList<>();
            if(json.has("visuals")) {
                for (JsonElement imageObject : json.getAsJsonObject("visuals").getAsJsonObject("avatarImage").getAsJsonArray("sources")) {
                    images.add(HomeTabImage.fromJSON(imageObject.getAsJsonObject()));
                }
            }
            return new HomeTabArtist(
                    json.getAsJsonObject("profile").get("name").getAsString(),
                    json.get("uri").getAsString(),
                    images
            );
        }
    }

    /**
     * Holds information about an HomeTab album
     */
    public static class HomeTabAlbum {
        private final String name;
        private final String uri;
        private final ArrayList<HomeTabArtist> artists;
        private final ArrayList<HomeTabImage> images;

        private HomeTabAlbum(
                String name,
                String uri,
                ArrayList<HomeTabArtist> artists,
                ArrayList<HomeTabImage> images
        ) {
            this.name = name;
            this.uri = uri;
            this.artists = artists;
            this.images = images;
        }

        public String getName() {
            return name;
        }

        public String getUri() {
            return uri;
        }

        public ArrayList<HomeTabArtist> getArtists() {
            return artists;
        }

        public ArrayList<HomeTabImage> getImages() {
            return images;
        }

        public static HomeTabAlbum fromJSON(JsonObject object) {
            ArrayList<HomeTabArtist> artists = new ArrayList<>();
            for(JsonElement artist : object.getAsJsonObject("artists").getAsJsonArray("items")) {
                artists.add(HomeTabArtist.fromJSON(artist.getAsJsonObject()));
            }
            ArrayList<HomeTabImage> images = new ArrayList<>();
            for(JsonElement image : object.getAsJsonObject("coverArt").getAsJsonArray("sources")) {
                images.add(HomeTabImage.fromJSON(image.getAsJsonObject()));
            }
            return new HomeTabAlbum(
                    object.get("name").getAsString(),
                    object.get("uri").getAsString(),
                    artists,
                    images
            );
        }
    }

    /**
     * Holds information about an HomeTab episode or chapter
     */
    public static class HomeTabEpisodeOrChapter {
        private final long totalMilliseconds;
        private final String isoDate;
        private final long playPositionMilliseconds;
        private final String EpisodeOrChapterName;
        private final String description;
        private final String uri;
        private final ArrayList<HomeTabImage> EpisodeOrChapterImages;
        private final String name;
        private final String publisherName;
        private final ArrayList<HomeTabImage> coverImages;

        private HomeTabEpisodeOrChapter(
                long totalMilliseconds,
                String isoDate,
                long playPositionMilliseconds,
                String EpisodeOrChapterName,
                String description,
                String uri,
                ArrayList<HomeTabImage> EpisodeOrChapterImages,
                String name,
                String publisherName,
                ArrayList<HomeTabImage> coverImages
        ) {
            this.totalMilliseconds = totalMilliseconds;
            this.isoDate = isoDate;
            this.playPositionMilliseconds = playPositionMilliseconds;
            this.EpisodeOrChapterName = EpisodeOrChapterName;
            this.description = description;
            this.uri = uri;
            this.EpisodeOrChapterImages = EpisodeOrChapterImages;
            this.name = name;
            this.publisherName = publisherName;
            this.coverImages = coverImages;
        }

        public long getTotalMilliseconds() {
            return totalMilliseconds;
        }

        public String getIsoDate() {
            return isoDate;
        }

        public long getPlayPositionMilliseconds() {
            return playPositionMilliseconds;
        }

        public String getEpisodeOrChapterName() {
            return EpisodeOrChapterName;
        }

        public String getDescription() {
            return description;
        }

        public String getUri() {
            return uri;
        }

        public ArrayList<HomeTabImage> getEpisodeOrChapterImages() {
            return EpisodeOrChapterImages;
        }

        public String getName() {
            return name;
        }

        public String getPublisherName() {
            return publisherName;
        }

        public ArrayList<HomeTabImage> getCoverImages() {
            return coverImages;
        }

        public static HomeTabEpisodeOrChapter fromJSON(JsonObject object) {
            ArrayList<HomeTabImage> EpisodeOrChapterImages = new ArrayList<>();
            if(object.get("__typename").getAsString().equalsIgnoreCase("restrictedcontent")) return null;
            for(JsonElement image : object.getAsJsonObject("coverArt").getAsJsonArray("sources")) {
                EpisodeOrChapterImages.add(HomeTabImage.fromJSON(image.getAsJsonObject()));
            }
            ArrayList<HomeTabImage> coverImages = new ArrayList<>();
            for(JsonElement image : object.getAsJsonObject("podcastV2").getAsJsonObject("data").getAsJsonObject("coverArt").getAsJsonArray("sources")) {
                coverImages.add(HomeTabImage.fromJSON(image.getAsJsonObject()));
            }
            return new HomeTabEpisodeOrChapter(
                    object.getAsJsonObject("duration").get("totalMilliseconds").getAsLong(),
                    object.getAsJsonObject("releaseDate").get("isoString").getAsString(),
                    object.getAsJsonObject("playedState").get("playPositionMilliseconds").getAsLong(),
                    object.get("name").getAsString(),
                    object.get("description").getAsString(),
                    object.get("uri").getAsString(),
                    EpisodeOrChapterImages,
                    object.getAsJsonObject("podcastV2").getAsJsonObject("data").get("name").getAsString(),
                    object.getAsJsonObject("podcastV2").getAsJsonObject("data").getAsJsonObject("publisher").get("name").getAsString(),
                    coverImages
            );
        }
    }

    public static class SpotifyBrowse {
        private final String id;
        private final String title;
        private final ArrayList<SpotifyBrowseEntry> body;

        private SpotifyBrowse(String id, String title, ArrayList<SpotifyBrowseEntry> body) {
            this.id = id;
            this.title = title;
            this.body = body;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public ArrayList<SpotifyBrowseEntry> getBody() {
            return body;
        }

        public static SpotifyBrowse fromJSON(JsonObject json) {
            ArrayList<SpotifyBrowseEntry> entries = new ArrayList<>();
            for(JsonElement object : json.getAsJsonArray("body")) {
                entries.add(SpotifyBrowseEntry.fromJSON(object.getAsJsonObject()));
            }
            return new SpotifyBrowse(json.get("id").getAsString(), json.get("title").getAsString(), entries);
        }
    }

    public static class SpotifyBrowseEntry {
        private final String id;
        private final SpotifyBrowseEntryComponent component;
        private final SpotifyBrowseEntryText text;
        private final Optional<SpotifyBrowseEntryCustom> custom;
        private final Optional<SpotifyBrowseEntryMetadata> metadata;
        private final Optional<SpotifyBrowserEntryImages> images;
        private final Optional<ArrayList<SpotifyBrowseEntry>> children;
        private final Optional<SpotifyBrowseEntryEvents> events;

        private SpotifyBrowseEntry(String id, SpotifyBrowseEntryComponent component, SpotifyBrowseEntryText text,
                                   Optional<SpotifyBrowseEntryCustom> custom,
                                   Optional<SpotifyBrowseEntryMetadata> metadata, Optional<SpotifyBrowserEntryImages> images,
                                   Optional<ArrayList<SpotifyBrowseEntry>> children, Optional<SpotifyBrowseEntryEvents> events) {
            this.id = id;
            this.component = component;
            this.text = text;
            this.custom = custom;
            this.metadata = metadata;
            this.images = images;
            this.children = children;
            this.events = events;
        }

        public Optional<SpotifyBrowseEntryEvents> getEvents() {
            return events;
        }

        public String getId() {
            return id;
        }

        public SpotifyBrowseEntryComponent getComponent() {
            return component;
        }

        public SpotifyBrowseEntryText getText() {
            return text;
        }

        public Optional<SpotifyBrowseEntryCustom> getCustom() {
            return custom;
        }

        public Optional<SpotifyBrowseEntryMetadata> getMetadata() {
            return metadata;
        }

        public Optional<SpotifyBrowserEntryImages> getImages() {
            return images;
        }

        public Optional<ArrayList<SpotifyBrowseEntry>> getChildren() {
            return children;
        }

        protected static SpotifyBrowseEntry fromJSON(JsonObject json) {
            Optional<SpotifyBrowserEntryImages> images = Optional.empty();
            if(json.has("images")) {
                images = Optional.of(SpotifyBrowserEntryImages.fromJSON(json.getAsJsonObject("images")));
            }
            Optional<ArrayList<SpotifyBrowseEntry>> children = Optional.empty();
            if(json.has("children")) {
                ArrayList<SpotifyBrowseEntry> childrenList = new ArrayList<>();
                for(JsonElement object : json.getAsJsonArray("children")) {
                    childrenList.add(SpotifyBrowseEntry.fromJSON(object.getAsJsonObject()));
                }
                children = Optional.of(childrenList);
            }
            Optional<SpotifyBrowseEntryCustom> custom = Optional.empty();
            if(json.has("custom")) {
                custom = Optional.of(SpotifyBrowseEntryCustom.fromJSON(json.getAsJsonObject("custom")));
            }
            Optional<SpotifyBrowseEntryMetadata> metadata = Optional.empty();
            if(json.has("metadata")) {
                metadata = Optional.of(SpotifyBrowseEntryMetadata.fromJSON(json.getAsJsonObject("metadata")));
            }
            Optional<SpotifyBrowseEntryEvents> events = Optional.empty();
            if(json.has("events")) {
                events = Optional.of(SpotifyBrowseEntryEvents.fromJSON(json.getAsJsonObject("events")));
            }
            return new SpotifyBrowseEntry(json.get("id").getAsString(),
                    SpotifyBrowseEntryComponent.fromJSON(json.getAsJsonObject("component")),
                    SpotifyBrowseEntryText.fromJSON(json.getAsJsonObject("text")),
                    custom,
                    metadata,
                    images, children, events);
        }
    }

    public static class SpotifyBrowserEntryImages {
        private final ArrayList<SpotifyBrowseEntryImagesImage> images;

        private SpotifyBrowserEntryImages(ArrayList<SpotifyBrowseEntryImagesImage> images) {
            this.images = images;
        }

        public ArrayList<SpotifyBrowseEntryImagesImage> getImages() {
            return images;
        }

        protected static SpotifyBrowserEntryImages fromJSON(JsonObject json) {
            ArrayList<SpotifyBrowseEntryImagesImage> images = new ArrayList<>();
            for(String key : json.keySet()) {
                images.add(new SpotifyBrowseEntryImagesImage(json.getAsJsonObject(key).get("uri").getAsString(),
                        SpotifyBrowseEntryImagesImageTypes.valueOf(key.toUpperCase(Locale.ENGLISH))));
            }
            return new SpotifyBrowserEntryImages(images);
        }
    }

    public static class SpotifyBrowseEntryImagesImage {
        private final String uri;
        private final SpotifyBrowseEntryImagesImageTypes type;

        protected SpotifyBrowseEntryImagesImage(String uri, SpotifyBrowseEntryImagesImageTypes type) {
            this.uri = uri;
            this.type = type;
        }

        public String getUri() {
            return uri;
        }

        public SpotifyBrowseEntryImagesImageTypes getType() {
            return type;
        }
    }

    public enum SpotifyBrowseEntryImagesImageTypes {
        MAIN,
        BACKGROUND
    }

    public static class SpotifyBrowseEntryMetadata {
        private final String sectionId;
        private final Optional<String> uri;
        private final Optional<String> promotion_id;
        private final Optional<String> videoUrl;
        private final Optional<Boolean> isAnimated;
        private final Optional<String> accessibilityText;

        private SpotifyBrowseEntryMetadata(String sectionId, Optional<String> uri, Optional<String> promotion_id,
                                           Optional<String> videoUrl, Optional<Boolean> isAnimated,
                                           Optional<String> accessibilityText) {
            this.sectionId = sectionId;
            this.uri = uri;
            this.promotion_id = promotion_id;
            this.videoUrl = videoUrl;
            this.isAnimated = isAnimated;
            this.accessibilityText = accessibilityText;
        }

        public String getSectionId() {
            return sectionId;
        }

        public Optional<String> getUri() {
            return uri;
        }

        public Optional<String> getPromotion_id() {
            return promotion_id;
        }

        public Optional<String> getVideoUrl() {
            return videoUrl;
        }

        public Optional<Boolean> getIsAnimated() {
            return isAnimated;
        }

        public Optional<String> getAccessibilityText() {
            return accessibilityText;
        }

        protected static SpotifyBrowseEntryMetadata fromJSON(JsonObject jsonObject) {
            Optional<String> uri = Optional.empty();
            if (jsonObject.has("uri")) {
                uri = Optional.of(jsonObject.get("uri").getAsString());
            }
            Optional<String> promotion_id = Optional.empty();
            if (jsonObject.has("promotion_id")) {
                promotion_id = Optional.of(jsonObject.get("promotion_id").getAsString());
            }
            Optional<String> videoUrl = Optional.empty();
            if (jsonObject.has("video_url")) {
                videoUrl = Optional.of(jsonObject.get("video_url").getAsString());
            }
            Optional<Boolean> isAnimated = Optional.empty();
            if (jsonObject.has("is_animated")) {
                isAnimated = Optional.of(jsonObject.get("is_animated").getAsBoolean());
            }
            Optional<String> accessibilityText = Optional.empty();
            if (jsonObject.has("accessibility_text")) {
                accessibilityText = Optional.of(jsonObject.get("accessibility_text").getAsString());
            }
            String sectionId = jsonObject.has("sectionId") ? jsonObject.get("sectionId").getAsString() : "";
            return new SpotifyBrowseEntryMetadata(sectionId, uri, promotion_id,
                    videoUrl, isAnimated, accessibilityText);
        }
    }

    /**
     * The only one implemented is the 'click' event and in it only the 'uri' data type is implemented
     */
    public static class SpotifyBrowseEntryEvents {
        private final ArrayList<SpotifyBrowseEntryEventsEvent> events;

        private SpotifyBrowseEntryEvents(ArrayList<SpotifyBrowseEntryEventsEvent> events) {
            this.events = events;
        }

        public ArrayList<SpotifyBrowseEntryEventsEvent> getEvents() {
            return events;
        }

        protected static SpotifyBrowseEntryEvents fromJSON(JsonObject jsonObject) {
            ArrayList<SpotifyBrowseEntryEventsEvent> events = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                String key = entry.getKey();
                // Only click is implemented right now
                if (key.equals("click")) {
                    JsonObject clickObj = entry.getValue().getAsJsonObject();
                    events.add(SpotifyBrowseEntryEventsEvent.fromJSON(clickObj));
                    break;
                }
            }
            return new SpotifyBrowseEntryEvents(events);
        }
    }

    public static class SpotifyBrowseEntryEventsEvent {
        private final String name;
        private final SpotifyBrowseEntryEventsTypes type;
        private final Optional<SpotifyBrowseEntryEventsEventDataUri> data_uri;

        private SpotifyBrowseEntryEventsEvent(String name, SpotifyBrowseEntryEventsTypes type,
                                              Optional<SpotifyBrowseEntryEventsEventDataUri> data_uri) {
            this.name = name;
            this.type = type;
            this.data_uri = data_uri;
        }

        public String getName() {
            return name;
        }

        public SpotifyBrowseEntryEventsTypes getType() {
            return type;
        }

        public Optional<SpotifyBrowseEntryEventsEventDataUri> getData_uri() {
            return data_uri;
        }

        protected static SpotifyBrowseEntryEventsEvent fromJSON(JsonObject jsonObject) {
            Optional<SpotifyBrowseEntryEventsEventDataUri> data_uri = Optional.empty();
            if (jsonObject.has("data") && jsonObject.getAsJsonObject("data").has("uri") && jsonObject.getAsJsonObject("data").entrySet().size() == 1) {
                data_uri = Optional.of(SpotifyBrowseEntryEventsEventDataUri.fromJSON(jsonObject.getAsJsonObject("data")));
            }
            String name = jsonObject.has("name") ? jsonObject.get("name").getAsString() : "";
            return new SpotifyBrowseEntryEventsEvent(name, SpotifyBrowseEntryEventsTypes.CLICK, data_uri);
        }
    }

    public static class SpotifyBrowseEntryEventsEventDataUri {
        private final String uri;

        private SpotifyBrowseEntryEventsEventDataUri(String uri) {
            this.uri = uri;
        }

        public String getUri() {
            return uri;
        }

        protected static SpotifyBrowseEntryEventsEventDataUri fromJSON(JsonObject jsonObject) {
            return new SpotifyBrowseEntryEventsEventDataUri(jsonObject.has("uri") ? jsonObject.get("uri").getAsString() : "");
        }
    }

    public enum SpotifyBrowseEntryEventsTypes {
        TOGGLEPLAYSTATECLICK,
        CLICK,
        CONTEXTMENUCLICK,
        TOGGLELIKESTATECLICK
    }

    public static class SpotifyBrowseEntryCustom {
        private final Optional<String> style;
        private final Optional<String> backgroundColor;
        private final Optional<String> entityType;

        private SpotifyBrowseEntryCustom(Optional<String> style, Optional<String> backgroundColor,
                                         Optional<String> entityType) {
            this.style = style;
            this.backgroundColor = backgroundColor;
            this.entityType = entityType;
        }

        public Optional<String> getStyle() {
            return style;
        }

        public Optional<String> getBackgroundColor() {
            return backgroundColor;
        }

        public Optional<String> getEntityType() {
            return entityType;
        }

        protected static SpotifyBrowseEntryCustom fromJSON(JsonObject jsonObject) {
            Optional<String> style = Optional.empty();
            if (jsonObject.has("style")) {
                style = Optional.of(jsonObject.get("style").getAsString());
            }
            Optional<String> backgroundColor = Optional.empty();
            if (jsonObject.has("backgroundColor")) {
                backgroundColor = Optional.of(jsonObject.get("backgroundColor").getAsString());
            }
            Optional<String> entityType = Optional.empty();
            if (jsonObject.has("entityType")) {
                entityType = Optional.of(jsonObject.get("entityType").getAsString());
            }
            return new SpotifyBrowseEntryCustom(style, backgroundColor, entityType);
        }
    }

    public static class SpotifyBrowseEntryText {
        private final String title;
        private final Optional<String> accessory;
        private final Optional<String> description;
        private final Optional<String> subtitle;

        private SpotifyBrowseEntryText(String title, Optional<String> accessory, Optional<String> description, Optional<String> subtitle) {
            this.title = title;
            this.accessory = accessory;
            this.description = description;
            this.subtitle = subtitle;
        }

        public String getTitle() {
            return title;
        }


        public Optional<String> getAccessory() {
            return accessory;
        }

        public Optional<String> getDescription() {
            return description;
        }

        public Optional<String> getSubtitle() {
            return subtitle;
        }

        protected static SpotifyBrowseEntryText fromJSON(JsonObject jsonObject) {
            Optional<String> accessory = Optional.empty();
            if (jsonObject.has("accessory")) {
                accessory = Optional.of(jsonObject.get("accessory").getAsString());
            }
            Optional<String> description = Optional.empty();
            if (jsonObject.has("description")) {
                description = Optional.of(jsonObject.get("description").getAsString());
            }
            Optional<String> subtitle = Optional.empty();
            if (jsonObject.has("subtitle")) {
                subtitle = Optional.of(jsonObject.get("subtitle").getAsString());
            }
            String title = jsonObject.has("title") ? jsonObject.get("title").getAsString() : "";
            return new SpotifyBrowseEntryText(title, accessory, description, subtitle);
        }
    }

    public static class SpotifyBrowseEntryComponent {
        private final String id;
        private final String category;

        private SpotifyBrowseEntryComponent(String id, String category) {
            this.id = id;
            this.category = category;
        }

        public String getId() {
            return id;
        }

        public String getCategory() {
            return category;
        }

        protected static SpotifyBrowseEntryComponent fromJSON(JsonObject jsonObject) {
            String id = jsonObject.has("id") ? jsonObject.get("id").getAsString() : "";
            String category = jsonObject.has("category") ? jsonObject.get("category").getAsString() : "";
            return new SpotifyBrowseEntryComponent(id, category);
        }
    }

    public static class SpotifyBrowseSection {
        private final String header;
        private final String id;
        private final ArrayList<SpotifyBrowseEntry> body;

        private SpotifyBrowseSection(String header, String id,
                                     ArrayList<SpotifyBrowseEntry> body) {
            this.header = header;
            this.id = id;
            this.body = body;
        }

        public String getHeader() {
            return header;
        }

        public String getId() {
            return id;
        }

        public ArrayList<SpotifyBrowseEntry> getBody() {
            return body;
        }

        public static SpotifyBrowseSection fromJSON(JsonObject jsonObject) {
            ArrayList<SpotifyBrowseEntry> entries = new ArrayList<>();
            for (JsonElement object : jsonObject.getAsJsonArray("body")) {
                entries.add(SpotifyBrowseEntry.fromJSON(object.getAsJsonObject()));
            }
            String header;
            if (jsonObject.has("header")) {
                header = jsonObject.getAsJsonObject("header").getAsJsonObject("text").get("title").getAsString();
            } else {
                header = jsonObject.has("title") ? jsonObject.get("title").getAsString() : "";
            }
            return new SpotifyBrowseSection(header,
                    jsonObject.has("id") ? jsonObject.get("id").getAsString() : "", entries);
        }
    }

    public static class ArtistUnionHeaderImageDataSource {
        public int maxHeight;
        public int maxWidth;
        public String url;
    }

    public static class ArtistUnionHeaderImageData {
        public List<ArtistUnionHeaderImageDataSource> sources;
    }

    public static class ArtistUnionHeaderImage {
        public ArtistUnionHeaderImageData data;
    }

    public static class ArtistUnionRelatedArtistsArtistProfile {
        public String name;
    }

    public static class ArtistUnionRelatedArtistsArtist {
        public ArtistUnionRelatedArtistsArtistProfile profile;
        public String uri;
    }

    public static class ArtistUnionRelatedArtists {
        public List<ArtistUnionRelatedArtistsArtist> items;
    }

    public static class ArtistUnionDiscoveredOnItemData {
        public String __typename;
        public String description;
        public String name;
        public String uri;
    }

    public static class ArtistUnionDiscoveredOnItem {
        public ArtistUnionDiscoveredOnItemData data;
    }

    public static class ArtistUnionDiscoveredOn {
        public List<ArtistUnionDiscoveredOnItem> items;
    }

    public static class LibraryResponse {
        public Data data;

        public static class Data {
            public Me me;
        }

        public static class Me {
            public LibraryPage libraryV3;
        }
    }

    public static class LibraryFilter {
        public String id;
        public String name;
    }

    public static class LibraryPage {
        @com.google.gson.annotations.SerializedName("__typename")
        public String typename;
        public List<AvailableFilter> availableFilters;
        public List<SortOrder> availableSortOrders;
        public List<LibraryItemEntry> items;
        public PagingInfo pagingInfo;
        public List<LibraryFilter> selectedFilters;
        public SortOrder selectedSortOrder;
        public int totalCount;

        public static LibraryPage fromJsonObject(JsonObject root) {
            // root is expected to be the full response object which contains data.me.libraryV3
            if (root == null) return null;
            JsonObject lib = null;
            if (root.has("data") && root.getAsJsonObject("data").has("me") && root.getAsJsonObject("data").getAsJsonObject("me").has("libraryV3")) {
                lib = root.getAsJsonObject("data").getAsJsonObject("me").getAsJsonObject("libraryV3");
            } else if (root.has("libraryV3")) {
                lib = root.getAsJsonObject("libraryV3");
            }
            if (lib == null) return null;
            return new Gson().fromJson(lib, LibraryPage.class);
        }

        public static LibraryPage fromJsonString(String json) {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return fromJsonObject(root);
        }
    }

    public static class AvailableFilter {
        public String id;
        public String name;
    }

    public static class SortOrder {
        public String id;
        public String name;
    }

    public static class PagingInfo {
        public int limit;
        public int offset;
    }

    public static class LibraryItemEntry {
        public AddedAt addedAt;
        public int depth;
        public ItemWrapper item;
        public boolean pinnable;
        public boolean pinned;
        public PlayedAt playedAt;
    }

    public static class AddedAt {
        public String isoString;
    }

    public static class PlayedAt {
        public String isoString;
    }

    public static class ItemWrapper {
        @com.google.gson.annotations.SerializedName("__typename")
        public String typename;
        @com.google.gson.annotations.SerializedName("_uri")
        public String uri;
        public JsonObject data;
    }

    // Parser for `data.me.library.tracks` responses
    public static class LibraryTracksResponse {
        public Data data;

        public static class Data {
            public Me me;
        }

        public static class Me {
            public Library library;
        }

        public static class Library {
            public UserLibraryTracks tracks;
        }
    }

    public static class UserLibraryTracks {
        @com.google.gson.annotations.SerializedName("__typename")
        public String typename;
        public List<UserLibraryTrackResponse> items;
        public PagingInfo pagingInfo;
        public int totalCount;

        public static UserLibraryTracks fromJsonObject(JsonObject root) {
            if (root == null) return null;
            JsonObject tracksObj = null;
            if (root.has("data") && root.getAsJsonObject("data").has("me")
                    && root.getAsJsonObject("data").getAsJsonObject("me").has("library")
                    && root.getAsJsonObject("data").getAsJsonObject("me").getAsJsonObject("library").has("tracks")) {
                tracksObj = root.getAsJsonObject("data").getAsJsonObject("me").getAsJsonObject("library").getAsJsonObject("tracks");
            } else if (root.has("tracks")) {
                tracksObj = root.getAsJsonObject("tracks");
            }
            if (tracksObj == null) return null;
            return new Gson().fromJson(tracksObj, UserLibraryTracks.class);
        }

        public static UserLibraryTracks fromJsonString(String json) {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            return fromJsonObject(root);
        }
    }

    // Convenience helpers for callers
    public static UserLibraryTracks parseLibraryTracks(JsonObject root) {
        return UserLibraryTracks.fromJsonObject(root);
    }

    public static UserLibraryTracks parseLibraryTracks(String json) {
        return UserLibraryTracks.fromJsonString(json);
    }

    public static class UserLibraryTrackResponse {
        @com.google.gson.annotations.SerializedName("__typename")
        public String typename;
        public AddedAt addedAt;
        public TrackWrapper track;
    }

    public static class TrackWrapper {
        @com.google.gson.annotations.SerializedName("_uri")
        public String uri;
        public TrackData data;
    }

    public static class TrackData {
        @com.google.gson.annotations.SerializedName("__typename")
        public String typename;
        public AlbumOfTrack albumOfTrack;
        public Artists artists;
        public AssociationsV3 associationsV3;
        public ContentRating contentRating;
        public Integer discNumber;
        public Duration duration;
        public String mediaType;
        public String name;
        public Playability playability;
        public Integer trackNumber;
    }

    public static class PlaylistItem {
        @SerializedName("__typename")
        public String typename;
        public List<Attribute> attributes;
        public CurrentUserCapabilities currentUserCapabilities;
        public String description;
        public String format;
        public Images images;
        public String name;
        public OwnerV2 ownerV2;
        public String revisionId;
        public String uri;

        public static PlaylistItem fromJson(String json) {
            return new Gson().fromJson(json, PlaylistItem.class);
        }

        public static class Attribute {
            public String key;
            public String value;
        }

        public static class CurrentUserCapabilities {
            public boolean canEditItems;
            public boolean canView;
        }

        public static class Images {
            public List<ImageItem> items;
        }

        public static class ImageItem {
            public List<CoverSource> sources;
        }

        public static class ColorDetail {
            public String hex;
            public boolean isFallback;
        }

        public static class OwnerV2 {
            public OwnerData data;
        }

        public static class OwnerData {
            @SerializedName("__typename")
            public String typename;
            public Object avatar;
            public String id;
            public String name;
            public String uri;
            public String username;
        }
    }

    public static class ShowItem {
        public CoverArt coverArt;
        public String description;
        public String name;
        public Publisher publisher;
        public String uri;
        public Language language;
        public String mediaType;

        public static class Publisher {
            public String name;
        }

        public static class Language {
            public String code;
        }
    }

    public static class AlbumOfTrack {
        public Artists artists;
        public CoverArt coverArt;
        public String name;
        public String uri;
    }

    public static class Artists {
        public List<ArtistItem> items;
    }

    public static class ArtistItem {
        public ArtistItemData data;
    }

    public static class ArtistItemData {
        public Profile profile;
        public String uri;
    }

    public static class Profile {
        public String name;
    }

    public static class CoverArt {
        public List<CoverSource> sources;
    }

    public static class CoverSource {
        public Integer height;
        public String url;
        public Integer width;
    }

    public static class AssociationsV3 {
        public CountWrapper audioAssociations;
        public CountWrapper videoAssociations;
    }

    public static class CountWrapper {
        public int totalCount;
    }

    public static class ContentRating {
        public String label;
    }

    public static class Duration {
        public long totalMilliseconds;
    }

    public static class Playability {
        public boolean playable;
        public String reason;
    }

    public static class SearchV2Response {
        public Data data;

        public static class Data {
            public SearchV2 searchV2;
        }

        public static class SearchV2 {
            public AlbumsV2 albumsV2;
            public ArtistsWithData artists;
            public Playlists playlists;
            public Podcasts podcasts;
            public TracksV2 tracksV2;
            public Users users;
            public TopResultsV2 topResultsV2;
        }

        public static class AlbumsV2 {
            @SerializedName("__typename")
            public String typename;
            public List<AlbumResponseWrapper> items;
            public int totalCount;
        }

        public static class AlbumResponseWrapper {
            @SerializedName("__typename")
            public String typename;
            public Album data;
        }

        public static class Album {
            @SerializedName("__typename")
            public String typename;
            public Artists artists;
            public CoverArt coverArt;
            public Date date;
            public String name;
            public String uri;
            public String type;
        }

        public static class ArtistsWithData {
            public List<ArtistsItemData> items;
        }

        public static class ArtistsItemData {
            public ArtistItem data;
        }

        public static class Artists {
            public List<ArtistItem> items;
        }

        public static class ArtistItem {
            public ArtistProfile profile;
            public String uri;
        }

        public static class ArtistData {
            @SerializedName("__typename")
            public String typename;
            public ArtistProfile profile;
            public String uri;
        }

        public static class ArtistProfile {
            public String name;
        }

        public static class Playlists {
            public List<PlaylistResponseWrapper> items;
            public int totalCount;

            public static Playlists fromJsonObject(JsonObject root) {
                if (root == null) return null;
                JsonObject playlistsObj = null;
                if (root.has("data") && root.getAsJsonObject("data").has("searchV2")) {
                    playlistsObj = root.getAsJsonObject("data").getAsJsonObject("searchV2").getAsJsonObject("playlists");
                } else if (root.has("playlists")) {
                    playlistsObj = root.getAsJsonObject("playlists");
                }
                return playlistsObj == null ? null : new Gson().fromJson(playlistsObj, Playlists.class);
            }
        }

        public static class PlaylistResponseWrapper {
            @SerializedName("__typename")
            public String typename;
            public PlaylistItem data;
        }

        public static class PlaylistItem {
            @SerializedName("__typename")
            public String typename;
            private String name;
            private String uri;
            private String description;
            private UserResponseWrapper ownerV2;

            public Optional<String> getName() {
                return Optional.ofNullable(name);
            }

            public Optional<String> getUri() {
                return Optional.ofNullable(uri);
            }

            public Optional<String> getDescription() {
                return Optional.ofNullable(description);
            }

            public Optional<UserResponseWrapper> getOwnerV2() {
                return Optional.ofNullable(ownerV2);
            }
        }

        public static class TracksV2 {
            public List<TracksV2Item> items;
            public int totalCount;
        }

        public static class TrackItem {
            public TrackData data;
        }

        public static class TracksV2Item {
            public TrackItem item;
        }

        public static class TrackData {
            @SerializedName("__typename")
            public String typename;
            public String id;
            public String name;
            public String uri;
            public Album albumOfTrack;
            public Artists artists;
            public Duration duration;
        }

        public static class Podcasts {
            public List<PodcastResponseWrapper> items;
            public int totalCount;
        }

        public static class PodcastResponseWrapper {
            @SerializedName("__typename")
            public String typename;
            public Podcast data;
        }

        public static class Podcast {
            public String name;
            public String uri;
            public String mediaType;
            public Publisher publisher;
            public CoverArt coverArt;
        }

        public static class Publisher {
            public String name;
        }

        public static class Users {
            public List<UsersItem> items;
            public int totalCount;
        }

        public static class UsersItem {
            @SerializedName("__typename")
            public String typename;
            public UserData data;
        }

        public static class UserData {
            public String username;
            public String uri;
        }

        public static class UserResponseWrapper {
            @SerializedName("__typename")
            public String typename;
            public UserData data;
        }

        public static class CoverArt {
            public List<CoverSource> sources;
        }

        public static class CoverSource {
            public String url;
            public Integer width;
            public Integer height;
        }

        public static class Avatar {
            public List<CoverSource> sources;
        }

        public static class Duration {
            public Long totalMilliseconds;
        }

        public static class Date {
            public Integer year;
        }

        public static class TopResultsV2 {
            public List<Featured> items;
        }

        public static class Featured {
            @SerializedName("__typename")
            public String typename;
            public JsonObject data;
        }

        public static SearchV2Response fromJsonObject(JsonObject root) {
            try {
                return new Gson().fromJson(root, SearchV2Response.class);
            } catch (Exception ex) {
                return null;
            }
        }

        public static SearchV2Response fromJsonString(String json) {
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                return fromJsonObject(root);
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static LibraryResponse getLibraryPage(String[] filters, String[] features, int limit, int offset) throws IOException, MercuryClient.MercuryException {
        return new Gson().fromJson(
                PublicValues.session.api().user().getLibrary(filters, features, limit, offset).toString(),
                LibraryResponse.class
        );
    }

    /**
     * Gets the complete HomeTab (Used in the tab Home)
     *
     * @return instance of HomeTab
     * @see HomeTab
     */
    public Optional<HomeTab> getHomeTab() throws IOException, MercuryClient.MercuryException {
        JsonObject root = PublicValues.session.api().user().getHome();
        try {
            return Optional.of(HomeTab.fromJSON(root.getAsJsonObject("data").getAsJsonObject("home")));
        }catch (Exception e) {
            ConsoleLogging.error("Error in HomeTab! Dumping JSON: " + root);
            ConsoleLogging.Throwable(e);
        }
        return Optional.empty();
    }

    public static ArtistUnionDiscoveredOn getArtistDiscoveredOn(String uri) throws IOException, MercuryClient.MercuryException {
        return new Gson().fromJson(
                PublicValues.session.api().artist().getArtistDiscoveredOn(uri).toString(),
                ArtistUnionDiscoveredOn.class);
    }

    public static LibraryTracksResponse getLibraryTracks(int limit, int offset) throws IOException, MercuryClient.MercuryException {
        return new Gson().fromJson(
                PublicValues.session.api().user().getLibraryTracks(limit, offset).toString(),
                LibraryTracksResponse.class
        );
    }

    public static ArtistUnionRelatedArtists getArtistRelatedArtists(String uri) throws IOException, MercuryClient.MercuryException {
        return new Gson().fromJson(
                PublicValues.session.api().artist().getArtistRelatedArtists(uri).toString(),
                ArtistUnionRelatedArtists.class);
    }

    public static ArtistUnionHeaderImage getArtistHeaderImage(String uri) throws IOException, MercuryClient.MercuryException {
        return new Gson().fromJson(
                PublicValues.session.api().artist().getArtistHeaderImage(uri).toString(),
                ArtistUnionHeaderImage.class);
    }

    public static SpotifyBrowse getSpotifyBrowse() throws IOException, MercuryClient.MercuryException {
        return SpotifyBrowse.fromJSON(PublicValues.session.api().getSpotifyBrowse());
    }

    public static SpotifyBrowseSection getSpotifyBrowseSection(String sectionUri) throws IOException, MercuryClient.MercuryException {
        return SpotifyBrowseSection.fromJSON(PublicValues.session.api().getSpotifyBrowseSection(sectionUri));
    }

    public static SearchV2Response search(String searchTerm, int offset, int limit, int numberOfTopResults, boolean includeAudiobooks, boolean includeArtistHasConcertsField, boolean includePreRelease, boolean includeAuthors) throws IOException, MercuryClient.MercuryException {
        if (numberOfTopResults < 1) numberOfTopResults = 1;
        return SearchV2Response.fromJsonObject(PublicValues.session.api().search(searchTerm, offset, limit, numberOfTopResults, includeAudiobooks, includeArtistHasConcertsField, includePreRelease, includeAuthors));
    }

    protected static JsonElement optionalJson(JsonObject jsonElement, String key, JsonPrimitive defaultValue) {
        JsonElement element = jsonElement.get(key);
        if (element == null || element.isJsonNull())
            return defaultValue;

        return element;
    }
}

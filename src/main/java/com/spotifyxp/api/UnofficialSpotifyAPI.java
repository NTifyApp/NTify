package com.spotifyxp.api;

import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.com.spotify.canvaz.CanvazOuterClass;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.ConnectionUtils;
import com.spotifyxp.utils.MapUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
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
            JSONObject object = new JSONObject(ConnectionUtils.makeGet("https://spclient.wg.spotify.com/color-lyrics/v2/track/" + uri.split(":")[2] + "?format=json&vocalRemoval=false",
                    MapUtils.of("Authorization", "Bearer " + InstanceManager.getSpotifyApi().getAccessToken(), "App-Platform", "Win32", "User-Agent", ApplicationUtils.getUserAgent())));
            JSONObject lyricsroot = new JSONObject(object.getJSONObject("lyrics").toString());
            Lyrics lyrics = new Lyrics();
            lyrics.language = lyricsroot.getString("language");
            lyrics.providerLyricsId = lyricsroot.getString("providerLyricsId");
            lyrics.providerDisplayName = lyricsroot.getString("providerDisplayName");
            lyrics.syncType = lyricsroot.getString("syncType");
            for (Object line : lyricsroot.getJSONArray("lines")) {
                JSONObject l = new JSONObject(line.toString());
                LyricsLine lyricsLine = new LyricsLine();
                lyricsLine.endTimeMs = Long.parseLong(l.getString("endTimeMs"));
                lyricsLine.startTimeMs = Long.parseLong(l.getString("startTimeMs"));
                lyricsLine.words = l.getString("words");
                lyrics.lines.add(lyricsLine);
            }
            return lyrics;
        } catch (JSONException | IOException e) {
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

        public static HomeTabSectionItem fromJSON(JSONObject json) {
            Optional<HomeTabPlaylist> playlist = Optional.empty();
            Optional<HomeTabArtist> artist = Optional.empty();
            Optional<HomeTabAlbum> album = Optional.empty();
            Optional<HomeTabEpisodeOrChapter> episodeOrChapter = Optional.empty();
            switch (HomeTabSectionItemTypes.valueOf(json.getJSONObject("content").getString("__typename"))) {
                case PlaylistResponseWrapper:
                    if(json.getJSONObject("content").getJSONObject("data").getString("__typename").equals("NotFound")) {
                        break;
                    }
                    if(json.getJSONObject("content").getJSONObject("data").getString("__typename").equals("GenericError")) break;
                    playlist = Optional.of(HomeTabPlaylist.fromJSON(json.getJSONObject("content").getJSONObject("data")));
                    break;
                case ArtistResponseWrapper:
                    if(json.getJSONObject("content").getJSONObject("data").getString("__typename").equals("NotFound")) {
                        break;
                    }
                    artist = Optional.of(HomeTabArtist.fromJSON(json.getJSONObject("content").getJSONObject("data")));
                    break;
                case AlbumResponseWrapper:
                    if(json.getJSONObject("content").getJSONObject("data").getString("__typename").equals("NotFound")) {
                        break;
                    }
                    album = Optional.of(HomeTabAlbum.fromJSON(json.getJSONObject("content").getJSONObject("data")));
                    break;
                case EpisodeOrChapterResponseWrapper:
                    if(json.getJSONObject("content").getJSONObject("data").getString("__typename").equals("NotFound")) {
                        break;
                    }
                    HomeTabEpisodeOrChapter homeTabEpisodeOrChapter = HomeTabEpisodeOrChapter.fromJSON(json.getJSONObject("content").getJSONObject("data"));
                    if(homeTabEpisodeOrChapter != null) episodeOrChapter = Optional.of(homeTabEpisodeOrChapter);
                    break;
                case UnknownType:
                    ConsoleLogging.warning("[HomeTab] Got section item with unknown type");
            }
            return new HomeTabSectionItem(
                    HomeTabSectionItemTypes.valueOf(json.getJSONObject("content").getString("__typename")),
                    json.getString("uri"),
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

        public static HomeTabSection fromJSON(JSONObject json) {
            Optional<String> name = Optional.empty();
            if(json.getJSONObject("data").has("title")) {
                name = Optional.of(json.getJSONObject("data").getJSONObject("title").getString("text"));
            }
            ArrayList<HomeTabSectionItem> items = new ArrayList<>();
            for(Object itemObject : json.getJSONObject("sectionItems").getJSONArray("items")) {
                items.add(HomeTabSectionItem.fromJSON(new JSONObject(itemObject.toString())));
            }
            return new HomeTabSection(
                    HomeTabSectionTypes.valueOf(json.getJSONObject("data").getString("__typename")),
                    name,
                    json.getString("uri"),
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

        public static HomeTab fromJSON(JSONObject json) {
            ArrayList<HomeTabSection> sections = new ArrayList<>();
            for(Object sectionObject : json.getJSONObject("sectionContainer").getJSONObject("sections").getJSONArray("items")) {
                sections.add(HomeTabSection.fromJSON(new JSONObject(sectionObject.toString())));
            }
            return new HomeTab(
                    json.getJSONObject("greeting").getString("text"),
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

        public static HomeTabPlaylist fromJSON(JSONObject object) {
            ArrayList<HomeTabImage> images = new ArrayList<>();
            for(Object image : object.getJSONObject("images").getJSONArray("items")) {
                JSONObject imageObject = new JSONObject(image.toString());
                images.add(HomeTabImage.fromJSON(imageObject.getJSONArray("sources").getJSONObject(0)));
            }
            return new HomeTabPlaylist(
                    object.getString("name"),
                    object.getString("description"),
                    object.getString("uri"),
                    object.getJSONObject("ownerV2").getJSONObject("data").getString("name"),
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

        public static HomeTabImage fromJSON(JSONObject json) {
            return new HomeTabImage(
                    json.optInt("width", -1),
                    json.optInt("height", -1),
                    json.getString("url")
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

        public static HomeTabArtist fromJSON(JSONObject json) {
            ArrayList<HomeTabImage> images = new ArrayList<>();
            if(json.has("visuals")) {
                for (Object imageObject : json.getJSONObject("visuals").getJSONObject("avatarImage").getJSONArray("sources")) {
                    images.add(HomeTabImage.fromJSON(new JSONObject(imageObject.toString())));
                }
            }
            return new HomeTabArtist(
                    json.getJSONObject("profile").getString("name"),
                    json.getString("uri"),
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

        public static HomeTabAlbum fromJSON(JSONObject object) {
            ArrayList<HomeTabArtist> artists = new ArrayList<>();
            for(Object artist : object.getJSONObject("artists").getJSONArray("items")) {
                artists.add(HomeTabArtist.fromJSON(new JSONObject(artist.toString())));
            }
            ArrayList<HomeTabImage> images = new ArrayList<>();
            for(Object image : object.getJSONObject("coverArt").getJSONArray("sources")) {
                images.add(HomeTabImage.fromJSON(new JSONObject(image.toString())));
            }
            return new HomeTabAlbum(
                    object.getString("name"),
                    object.getString("uri"),
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

        public static HomeTabEpisodeOrChapter fromJSON(JSONObject object) {
            ArrayList<HomeTabImage> EpisodeOrChapterImages = new ArrayList<>();
            if(object.getString("__typename").equalsIgnoreCase("restrictedcontent")) return null;
            for(Object image : object.getJSONObject("coverArt").getJSONArray("sources")) {
                EpisodeOrChapterImages.add(HomeTabImage.fromJSON(new JSONObject(image.toString())));
            }
            ArrayList<HomeTabImage> coverImages = new ArrayList<>();
            for(Object image : object.getJSONObject("podcastV2").getJSONObject("data").getJSONObject("coverArt").getJSONArray("sources")) {
                coverImages.add(HomeTabImage.fromJSON(new JSONObject(image.toString())));
            }
            return new HomeTabEpisodeOrChapter(
                    object.getJSONObject("duration").getLong("totalMilliseconds"),
                    object.getJSONObject("releaseDate").getString("isoString"),
                    object.getJSONObject("playedState").getLong("playPositionMilliseconds"),
                    object.getString("name"),
                    object.getString("description"),
                    object.getString("uri"),
                    EpisodeOrChapterImages,
                    object.getJSONObject("podcastV2").getJSONObject("data").getString("name"),
                    object.getJSONObject("podcastV2").getJSONObject("data").getJSONObject("publisher").getString("name"),
                    coverImages
            );
        }
    }

    /**
     * Gets the complete HomeTab (Used in the tab Home)
     *
     * @return instance of HomeTab
     * @see HomeTab
     */
    public Optional<HomeTab> getHomeTab() throws IOException {
        JSONObject root;
        try {
            //https://api-partner.spotify.com/pathfinder/v1/query?operationName=home&variables=%7B%22timeZone%22%3A%22Europe%2FBerlin%22%7D&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%2263c412a34a2071adfd99b804ea2fe1d8e9c5fd7d248e29ca54cc97a7ca06b561%22%7D%7D
            String url = "https://api-partner.spotify.com/pathfinder/v1/query?operationName=home&variables=" + URLEncoder.encode("{\"timeZone\":\"" + ZoneId.systemDefault() + "\"}", Charset.defaultCharset().toString()) + "&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%2263c412a34a2071adfd99b804ea2fe1d8e9c5fd7d248e29ca54cc97a7ca06b561%22%7D%7D";
            root = new JSONObject(new JSONObject(ConnectionUtils.makeGet(url,
                    MapUtils.of("Authorization", "Bearer " + InstanceManager.getSpotifyApi().getAccessToken(), "App-Platform", "Win32", "User-Agent", ApplicationUtils.getUserAgent()))).getJSONObject("data").getJSONObject("home").toString());
        } catch (UnsupportedEncodingException e) {
            String url = "https://api-partner.spotify.com/pathfinder/v1/query?operationName=home&extensions=%7B%22persistedQuery%22%3A%7B%22version%22%3A1%2C%22sha256Hash%22%3A%2263c412a34a2071adfd99b804ea2fe1d8e9c5fd7d248e29ca54cc97a7ca06b561%22%7D%7D";
            root = new JSONObject(new JSONObject(ConnectionUtils.makeGet(url,
                    MapUtils.of("Authorization", "Bearer " + InstanceManager.getSpotifyApi().getAccessToken(), "App-Platform", "Win32", "User-Agent", ApplicationUtils.getUserAgent()))).getJSONObject("data").getJSONObject("home").toString());
        }
        try {
            return Optional.of(HomeTab.fromJSON(root));
        }catch (JSONException e) {
            ConsoleLogging.error("Error in HomeTab! Dumping JSON: " + root);
            ConsoleLogging.Throwable(e);
        }
        return Optional.empty();
    }

    /**
     * Gets an url referring to a canvas mp4 file for a track<br><br>
     * <a style="color:yellow;font:bold">!!Deprecated!! </a> Will be removed in a future version
     *
     * @param uri Track URI (spotify:track:xyz)
     * @return String
     */
    @Deprecated
    public static String getCanvasURLForTrack(String uri) {
        try {
            return PublicValues.session.api().getCanvases(CanvazOuterClass.EntityCanvazRequest.newBuilder().addEntities(CanvazOuterClass.EntityCanvazRequest.Entity.newBuilder().setEntityUri(uri).buildPartial()).build()).getCanvases(0).getUrl();
        } catch (ArrayIndexOutOfBoundsException e) {
            return "";
        } catch (IOException | MercuryClient.MercuryException e) {
            throw new RuntimeException(e);
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

        public static SpotifyBrowse fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            ArrayList<SpotifyBrowseEntry> entries = new ArrayList<>();
            for(Object object : jsonObject.getJSONArray("body")) {
                entries.add(SpotifyBrowseEntry.fromJSON(object.toString()));
            }
            return new SpotifyBrowse(jsonObject.getString("id"), jsonObject.getString("title"), entries);
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

        protected static SpotifyBrowseEntry fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            Optional<SpotifyBrowserEntryImages> images = Optional.empty();
            if(jsonObject.has("images")) {
                images = Optional.of(SpotifyBrowserEntryImages.fromJSON(jsonObject.getJSONObject("images").toString()));
            }
            Optional<ArrayList<SpotifyBrowseEntry>> children = Optional.empty();
            if(jsonObject.has("children")) {
                ArrayList<SpotifyBrowseEntry> childrenList = new ArrayList<>();
                for(Object object : jsonObject.getJSONArray("children")) {
                    childrenList.add(SpotifyBrowseEntry.fromJSON(object.toString()));
                }
                children = Optional.of(childrenList);
            }
            Optional<SpotifyBrowseEntryCustom> custom = Optional.empty();
            if(jsonObject.has("custom")) {
                custom = Optional.of(SpotifyBrowseEntryCustom.fromJSON(jsonObject.getJSONObject("custom").toString()));
            }
            Optional<SpotifyBrowseEntryMetadata> metadata = Optional.empty();
            if(jsonObject.has("metadata")) {
                metadata = Optional.of(SpotifyBrowseEntryMetadata.fromJSON(jsonObject.getJSONObject("metadata").toString()));
            }
            Optional<SpotifyBrowseEntryEvents> events = Optional.empty();
            if(jsonObject.has("events")) {
                events = Optional.of(SpotifyBrowseEntryEvents.fromJSON(jsonObject.getJSONObject("events").toString()));
            }
            return new SpotifyBrowseEntry(jsonObject.getString("id"),
                    SpotifyBrowseEntryComponent.fromJSON(jsonObject.getJSONObject("component").toString()),
                    SpotifyBrowseEntryText.fromJSON(jsonObject.getJSONObject("text").toString()),
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

        protected static SpotifyBrowserEntryImages fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            ArrayList<SpotifyBrowseEntryImagesImage> images = new ArrayList<>();
            for(String key : jsonObject.keySet()) {
                images.add(new SpotifyBrowseEntryImagesImage(jsonObject.getJSONObject(key).getString("uri"),
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

        protected static SpotifyBrowseEntryMetadata fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            Optional<String> uri = Optional.empty();
            if(jsonObject.has("uri")) {
                uri = Optional.of(jsonObject.getString("uri"));
            }
            Optional<String> promotion_id = Optional.empty();
            if(jsonObject.has("promotion_id")) {
                promotion_id = Optional.of(jsonObject.getString("promotion_id"));
            }
            Optional<String> videoUrl = Optional.empty();
            if(jsonObject.has("video_url")) {
                videoUrl = Optional.of(jsonObject.getString("video_url"));
            }
            Optional<Boolean> isAnimated = Optional.empty();
            if(jsonObject.has("is_animated")) {
                isAnimated = Optional.of(jsonObject.getBoolean("is_animated"));
            }
            Optional<String> accessibilityText = Optional.empty();
            if(jsonObject.has("accessibility_text")) {
                accessibilityText = Optional.of(jsonObject.getString("accessibility_text"));
            }
            return new SpotifyBrowseEntryMetadata(jsonObject.getString("sectionId"), uri, promotion_id,
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

        protected static SpotifyBrowseEntryEvents fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            ArrayList<SpotifyBrowseEntryEventsEvent> events = new ArrayList<>();
            for(String key : jsonObject.keySet()) {
                // Only click is implemented right now
                if(key.equals("click")) {
                    events.add(SpotifyBrowseEntryEventsEvent.fromJSON(jsonObject.getJSONObject(key).toString(), SpotifyBrowseEntryEventsTypes.CLICK));
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

        protected static SpotifyBrowseEntryEventsEvent fromJSON(String json, SpotifyBrowseEntryEventsTypes type) {
            JSONObject jsonObject = new JSONObject(json);
            Optional<SpotifyBrowseEntryEventsEventDataUri> data_uri = Optional.empty();
            if(jsonObject.getJSONObject("data").has("uri") && jsonObject.getJSONObject("data").keySet().size() == 1) {
                data_uri = Optional.of(SpotifyBrowseEntryEventsEventDataUri.fromJSON(jsonObject.getJSONObject("data").toString()));
            }
            return new SpotifyBrowseEntryEventsEvent(jsonObject.getString("name"), type, data_uri);
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

        protected static SpotifyBrowseEntryEventsEventDataUri fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            return new SpotifyBrowseEntryEventsEventDataUri(new JSONObject(json).getString("uri"));
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

        protected static SpotifyBrowseEntryCustom fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            Optional<String> style = Optional.empty();
            if(jsonObject.has("style")) {
                style = Optional.of(jsonObject.getString("style"));
            }
            Optional<String> backgroundColor = Optional.empty();
            if(jsonObject.has("backgroundColor")) {
                backgroundColor = Optional.of(jsonObject.getString("backgroundColor"));
            }
            Optional<String> entityType = Optional.empty();
            if(jsonObject.has("entityType")) {
                entityType = Optional.of(jsonObject.getString("entityType"));
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

        protected static SpotifyBrowseEntryText fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            Optional<String> accessory = Optional.empty();
            if(jsonObject.has("accessory")) {
                accessory = Optional.of(jsonObject.getString("accessory"));
            }
            Optional<String> description = Optional.empty();
            if(jsonObject.has("description")) {
                description = Optional.of(jsonObject.getString("description"));
            }
            Optional<String> subtitle = Optional.empty();
            if(jsonObject.has("subtitle")) {
                subtitle = Optional.of(jsonObject.getString("subtitle"));
            }
            return new SpotifyBrowseEntryText(jsonObject.getString("title"), accessory, description, subtitle);
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

        protected static SpotifyBrowseEntryComponent fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            return new SpotifyBrowseEntryComponent(jsonObject.getString("id"), jsonObject.getString("category"));
        }
    }

    public static class SpotifyBrowseSection {
        private final SpotifyBrowseEntry header;
        private final String id;
        private final ArrayList<SpotifyBrowseEntry> body;

        private SpotifyBrowseSection(SpotifyBrowseEntry header, String id,
                                     ArrayList<SpotifyBrowseEntry> body) {
            this.header = header;
            this.id = id;
            this.body = body;
        }

        public SpotifyBrowseEntry getHeader() {
            return header;
        }

        public String getId() {
            return id;
        }

        public ArrayList<SpotifyBrowseEntry> getBody() {
            return body;
        }

        public static SpotifyBrowseSection fromJSON(String json) {
            JSONObject jsonObject = new JSONObject(json);
            ArrayList<SpotifyBrowseEntry> entries = new ArrayList<>();
            for(Object object : jsonObject.getJSONArray("body")) {
                entries.add(SpotifyBrowseEntry.fromJSON(object.toString()));
            }
            return new SpotifyBrowseSection(SpotifyBrowseEntry.fromJSON(jsonObject.getJSONObject("header").toString()),
                    jsonObject.getString("id"), entries);
        }
    }

    public static SpotifyBrowse getSpotifyBrowse() throws IOException {
        String query = "?platform=android&client-timezone=" + URLEncoder.encode("{\"timeZone\":\"" + ZoneId.systemDefault() + "\"}", Charset.defaultCharset().toString()) + "&podcast=true&locale=" + Locale.getDefault();
        return SpotifyBrowse.fromJSON(ConnectionUtils.makeGet("https://spclient.wg.spotify.com/hubview-mobile-v1/browse" + query, new HashMap<String, String>() {{
            //put("client-token", PublicValues.session.api().getClientToken());
            put("authorization", "Bearer " + InstanceManager.getPkce().getToken());
        }}));
    }

    // The endpoint returns an id of 'browse-page-mobile-fallback' when there is something wrong
    public static SpotifyBrowseSection getSpotifyBrowseSection(String sectionUri) throws IOException {
        String query = "?platform=android&client-timezone=" + URLEncoder.encode("{\"timeZone\":\"" + ZoneId.systemDefault() + "\"}", Charset.defaultCharset().toString()) + "&podcast=true&locale=" + Locale.getDefault();
        return SpotifyBrowseSection.fromJSON(ConnectionUtils.makeGet("https://spclient.wg.spotify.com/hubview-mobile-v1/browse/" + sectionUri + query, new HashMap<String, String>() {{
            //put("client-token", PublicValues.session.api().getClientToken());
            put("authorization", "Bearer " + InstanceManager.getPkce().getToken());
        }}));
    }
}

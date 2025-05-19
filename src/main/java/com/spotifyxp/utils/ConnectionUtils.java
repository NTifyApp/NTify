package com.spotifyxp.utils;


import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@SuppressWarnings("unused")
public class ConnectionUtils {

    @Nullable
    public static String makeGet(String url, @Nullable Map<String, String> headers) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", ApplicationUtils.getUserAgent())
                .get();
        if (headers != null) requestBuilder.headers(Headers.of(headers));
        Response response = Objects.requireNonNull(PublicValues.defaultHttpClient.newCall(requestBuilder.build()).execute());
        if(response.code() == 401) {
            if(headers.containsKey("Authorization") && url.contains("spotify.com")) {
                Events.triggerEvent(SpotifyXPEvents.apikeyrefresh.getName());
                Map<String, String> newHeaders = new HashMap<>(headers);
                newHeaders.put("Authorization", "Bearer " + InstanceManager.getSpotifyApi().getAccessToken());
                return makeGet(url, newHeaders);
            }else {
                return null;
            }
        }else {
            return response.body().string();
        }
    }

    public static boolean isWebsiteReachable(String url) {
        try {
            makeGet(url, null);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Nullable
    public static String makePost(String url, NameValuePair[] topost, @Nullable Map<String, String> headers) throws Exception {
        FormBody.Builder formBodyBuilder = new FormBody.Builder();
        for (NameValuePair pair : topost) {
            formBodyBuilder.add(pair.getName(), pair.getValue());
        }
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("User-Agent", ApplicationUtils.getUserAgent())
                .post(formBodyBuilder.build());
        if (headers != null) requestBuilder.headers(Headers.of(headers));
        Response response = Objects.requireNonNull(PublicValues.defaultHttpClient.newCall(requestBuilder.build()).execute());
        if(response.code() == 401) {
            if(headers.containsKey("Authorization") && url.contains("spotify.com")) {
                Events.triggerEvent(SpotifyXPEvents.apikeyrefresh.getName());
                Map<String, String> newHeaders = new HashMap<>(headers);
                newHeaders.put("Authorization", "Bearer " + InstanceManager.getSpotifyApi().getAccessToken());
                return makeGet(url, newHeaders);
            }else {
                return null;
            }
        }else {
            return response.body().string();
        }
    }

    @Nullable
    public static String makeDelete(String url, @Nullable Map<String, String> headers) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .addHeader("User-Agent", ApplicationUtils.getUserAgent());
        if (headers != null) requestBuilder.headers(Headers.of(headers));
        Response response = Objects.requireNonNull(PublicValues.defaultHttpClient.newCall(requestBuilder.build()).execute());
        if(response.code() == 401) {
            if(headers.containsKey("Authorization") && url.contains("spotify.com")) {
                Events.triggerEvent(SpotifyXPEvents.apikeyrefresh.getName());
                Map<String, String> newHeaders = new HashMap<>(headers);
                newHeaders.put("Authorization", "Bearer " + InstanceManager.getSpotifyApi().getAccessToken());
                return makeGet(url, newHeaders);
            }else {
                return null;
            }
        }else {
            return response.body().string();
        }
    }

    public static void openBrowser(String url) throws URISyntaxException, IOException {
        String browserpath = "";
        if(new File(PublicValues.fileslocation, "credentials.json").exists()) {
            browserpath = PublicValues.config.getString(ConfigValues.mypalpath.name);
            if(!browserpath.isEmpty()) {
                ProcessBuilder builder = new ProcessBuilder("\"" + browserpath + "\"", url);
                try {
                    builder.start();
                } catch (IOException e) {
                    ConsoleLogging.Throwable(e);
                }
            }
        }
        if (browserpath.isEmpty()) {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }else{
                JOptionPane.showMessageDialog(ContentPanel.frame, PublicValues.language.translate("utils.browserpath.unabletoopen.message"), PublicValues.language.translate("utils.browserpath.unabletoopen.title"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

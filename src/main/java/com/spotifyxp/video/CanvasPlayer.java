package com.spotifyxp.video;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.deps.com.spotify.canvaz.CanvazOuterClass;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Episode;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Track;
import com.spotifyxp.deps.xyz.gianlu.librespot.mercury.MercuryClient;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.graphics.Graphics;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.PlayerArea;
import com.spotifyxp.swingextension.JFrame;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.util.Objects;

public class CanvasPlayer extends JFrame {
    private File cachePath;
    private boolean videoLoaded = false;

    public CanvasPlayer() {
        setTitle("SpotifyXP - Canvas"); // ToDo: Translate
        setPreferredSize(new Dimension(290, 460));
        if(!PublicValues.config.getBoolean(ConfigValues.cache_disabled.name)) {
            cachePath = new File(PublicValues.appLocation, "cvnscache");
            if(!cachePath.exists()) {
                if(!cachePath.mkdir()) {
                    ConsoleLogging.error("Failed to create cvnscache directory");
                    PublicValues.contentPanel.remove(PlayerArea.canvasPlayerButton.getJComponent());
                }
            }
        }
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
    }

    EventSubscriber onPause = new EventSubscriber() {
        @Override
        public void run(Object... data) {
            if(!videoLoaded) return;
            PublicValues.vlcPlayer.pause();
        }
    };

    EventSubscriber onPlay = new EventSubscriber() {
        @Override
        public void run(Object... data) {
            if(!videoLoaded) return;
            PublicValues.vlcPlayer.resume();
        }
    };

    private String convertUrlToName(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }

    void clearCache() throws NullPointerException{
        for(File file : Objects.requireNonNull(cachePath.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().toLowerCase().endsWith(".mp4");
            }
        }))) {
            if(!file.delete()) {
                ConsoleLogging.warning("Failed to remove file in cvnscache");
            }
        }
    }

    public void loadCanvas(String uri) {
        try {
            if(!PublicValues.config.getBoolean(ConfigValues.cache_disabled.name)) {
                clearCache();
                String cvnsUrl = PublicValues.session.api().getCanvases(CanvazOuterClass.EntityCanvazRequest.newBuilder()
                        .addEntities(CanvazOuterClass.EntityCanvazRequest.Entity.newBuilder()
                                .setEntityUri(uri)
                                .buildPartial())
                        .build()).getCanvases(0).getUrl();
                if(cvnsUrl.isEmpty()) return;
                try (BufferedInputStream in = new BufferedInputStream(new URL(cvnsUrl).openStream());
                     FileOutputStream fileOutputStream = new FileOutputStream(new File(cachePath, convertUrlToName(cvnsUrl)));) {
                    byte[] dataBuffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                        fileOutputStream.write(dataBuffer, 0, bytesRead);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                PublicValues.vlcPlayer.play(new File(cachePath, convertUrlToName(cvnsUrl)).getAbsolutePath());
                videoLoaded = true;
            } else {
                String cvnsUrl = PublicValues.session.api().getCanvases(CanvazOuterClass.EntityCanvazRequest.newBuilder()
                        .addEntities(CanvazOuterClass.EntityCanvazRequest.Entity.newBuilder()
                                .setEntityUri(uri)
                                .buildPartial())
                        .build()).getCanvases(0).getUrl();
                if(cvnsUrl.isEmpty()) return;
                PublicValues.vlcPlayer.play(cvnsUrl);
                videoLoaded = true;
            }
        } catch (IndexOutOfBoundsException ignored) {
            // No canvas for track
            ConsoleLogging.info("No canvas available for track");
        } catch (IOException | MercuryClient.MercuryException e) {
            throw new RuntimeException(e);
        }
    }

    EventSubscriber onNextTrack = new EventSubscriber() {
        @Override
        public void run(Object... data) {
            PublicValues.vlcPlayer.stop();
            String uri = "";
            if (data[0] instanceof Track) {
                uri = ((Track) data[0]).getUri();
            } else if (data[0] instanceof Episode) {
                uri = ((Episode) data[0]).getUri();
            } else {
                ConsoleLogging.error("Invalid object type in next track: " + data[0].getClass().getSimpleName());
            }
            loadCanvas(uri);
        }
    };

    @Override
    public void close() {
        remove(PublicValues.vlcPlayer.getComponent());
        PlayerArea.canvasPlayerButton.isFilled = false;
        PlayerArea.canvasPlayerButton.setImage(Graphics.VIDEO.getPath());
        PublicValues.vlcPlayer.stop();
        PublicValues.vlcPlayer.release();
        Events.unsubscribe(SpotifyXPEvents.trackNext.getName(), onNextTrack);
        Events.unsubscribe(SpotifyXPEvents.playerpause.getName(), onPause);
        Events.unsubscribe(SpotifyXPEvents.playerresume.getName(), onPlay);
        PublicValues.vlcPlayer.removeOnTakeOver();
        dispose();
    }

    @Override
    public void open() throws NullPointerException {
        add(PublicValues.vlcPlayer.getComponent());
        super.open();
        Events.subscribe(SpotifyXPEvents.trackNext.getName(), onNextTrack);
        Events.subscribe(SpotifyXPEvents.playerpause.getName(), onPause);
        Events.subscribe(SpotifyXPEvents.playerresume.getName(), onPlay);
        PublicValues.vlcPlayer.init(this::close);
        PublicValues.vlcPlayer.setLooping(true);
        loadCanvas(Objects.requireNonNull(InstanceManager.getSpotifyPlayer().currentMetadata()).id.toSpotifyUri());
    }
}

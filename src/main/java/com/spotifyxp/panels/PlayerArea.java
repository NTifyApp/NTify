package com.spotifyxp.panels;

import com.spotifyxp.PublicValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.deps.com.spotify.context.ContextTrackOuterClass;
import com.spotifyxp.dialogs.LyricsDialog;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.graphics.Graphics;
import com.spotifyxp.history.PlaybackHistory;
import com.spotifyxp.listeners.PlayerListener;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.pip.PiPPlayer;
import com.spotifyxp.protogens.PlayerState;
import com.spotifyxp.swingextension.*;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.utils.*;
import com.spotifyxp.video.CanvasPlayer;
import org.apache.commons.io.IOUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PlayerArea extends JPanel {
    public static JImagePanel playerImage;
    public static JScrollText playerTitle;
    public static JScrollText playerDescription;
    public static JImageButton playerPlayPreviousButton;
    public static JImageButton playerPlayPauseButton;
    public static JImageButton playerPlayNextButton;
    public static JSlider playerCurrentTime;
    public static JLabel playerPlayTime;
    public static JLabel playerPlayTimeTotal;
    public static JSVGPanel playerAreaShuffleButton;
    public static JSVGPanel playerAreaRepeatingButton;
    public static JSVGPanel playerAreaLyricsButton;
    public static JSVGPanel playerAreaVolumeIcon;
    public static JSlider playerAreaVolumeSlider;
    public static JLabel playerAreaVolumeCurrent;
    public static JSVGPanel heart;
    public static JSVGPanel historyButton;
    private static LastPlayState lastPlayState;
    public static CanvasPlayer canvasPlayer;
    public static JSVGPanel canvasPlayerButton;
    private static boolean doneLastParsing = false;
    public static ContextMenu contextMenu;
    public static PiPPlayer pipPlayer;
    private boolean wasPaused = false;

    public PlayerArea(JFrame frame) {
        setBounds(72, 0, 565, 100);
        setLayout(null);

        playerAreaShuffleButton = new JSVGPanel();
        playerAreaShuffleButton.getJComponent().setBounds(510, 75, 20, 20);
        playerAreaShuffleButton.getJComponent().setBackground(frame.getBackground());
        add(playerAreaShuffleButton.getJComponent());
        playerAreaShuffleButton.setImage(Graphics.SHUFFLE.getPath());
        playerAreaShuffleButton.getJComponent().addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (playerAreaShuffleButton.isFilled) {
                    PublicValues.shuffle = false;
                    InstanceManager.getPlayer().getPlayer().setShuffle(false);
                    try {
                        InstanceManager.getSpotifyPlayer().tracks(true).next.clear();
                        for (String s : Shuffle.before) {
                            InstanceManager.getSpotifyPlayer().addToQueue(s);
                        }
                    } catch (Exception e2) {
                        ConsoleLogging.Throwable(e2);
                        GraphicalMessage.openException(e2);
                    }
                    playerAreaShuffleButton.setImage(Graphics.SHUFFLE.getPath());
                    playerAreaShuffleButton.isFilled = false;
                } else {
                    PublicValues.shuffle = true;
                    InstanceManager.getPlayer().getPlayer().setShuffle(true);
                    Shuffle.makeShuffle();
                    playerAreaShuffleButton.isFilled = true;
                    playerAreaShuffleButton.setImage(Graphics.SHUFFLESELECTED.getPath());
                }
            }
        }));

        playerAreaRepeatingButton = new JSVGPanel();
        playerAreaRepeatingButton.getJComponent().setBounds(540, 75, 20, 20);
        playerAreaRepeatingButton.getJComponent().setBackground(frame.getBackground());
        add(playerAreaRepeatingButton.getJComponent());
        playerAreaRepeatingButton.setImage(Graphics.REPEAT.getPath());
        playerAreaRepeatingButton.getJComponent().addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (playerAreaRepeatingButton.isFilled) {
                    InstanceManager.getPlayer().getPlayer().setRepeat(false, false);
                    playerAreaRepeatingButton.setImage(Graphics.REPEAT.getPath());
                    playerAreaRepeatingButton.isFilled = false;
                } else {
                    InstanceManager.getPlayer().getPlayer().setRepeat(true, false);
                    playerAreaRepeatingButton.isFilled = true;
                    playerAreaRepeatingButton.setImage(Graphics.REPEATSELECTED.getPath());
                }
            }
        }));

        playerImage = new JImagePanel();
        playerImage.setBounds(10, 11, 78, 78);
        add(playerImage);
        playerImage.setImage(Graphics.NOTHINGPLAYING.getPath());
        Events.subscribe(SpotifyXPEvents.onFrameReady.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                playerImage.setImage(SVGUtils.svgToImageInputStreamSameSize(getClass().getResourceAsStream(Graphics.NOTHINGPLAYING.getPath()), new Dimension(78, 78)));
            }
        });

        playerAreaLyricsButton = new JSVGPanel();
        playerAreaLyricsButton.getJComponent().setBounds(280, 75, 14, 14);
        playerAreaLyricsButton.getJComponent().setBackground(frame.getBackground());
        add(playerAreaLyricsButton.getJComponent());
        playerAreaLyricsButton.setImage(Graphics.MICROPHONE.getPath());
        playerAreaLyricsButton.getJComponent().addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                try {
                    if (PublicValues.lyricsDialog == null) {
                        PublicValues.lyricsDialog = new LyricsDialog();
                    }
                    if (playerAreaLyricsButton.isFilled) {
                        PublicValues.lyricsDialog.close();
                        playerAreaLyricsButton.setImage(Graphics.MICROPHONE.getPath());
                        playerAreaLyricsButton.isFilled = false;
                    } else {
                        if (PublicValues.lyricsDialog.open(Objects.requireNonNull(InstanceManager.getSpotifyPlayer().currentPlayable()).toSpotifyUri())) {
                            playerAreaLyricsButton.setImage(Graphics.MICROPHONESELECTED.getPath());
                            playerAreaLyricsButton.isFilled = true;
                        }
                    }
                } catch (NullPointerException e2) {
                    playerAreaLyricsButton.setImage(Graphics.MICROPHONE.getPath());
                    playerAreaLyricsButton.isFilled = false;
                }
            }
        }));

        playerAreaVolumeIcon = new JSVGPanel();
        playerAreaVolumeIcon.getJComponent().setBounds(306, 75, 14, 14);
        playerAreaVolumeIcon.getJComponent().setBackground(frame.getBackground());
        add(playerAreaVolumeIcon.getJComponent());
        playerAreaVolumeIcon.setImage(Graphics.VOLUMEFULL.getPath());

        playerAreaVolumeCurrent = new JLabel();
        playerAreaVolumeCurrent.setBounds(489, 75, 35, 14);
        add(playerAreaVolumeCurrent);

        playerAreaVolumeSlider = new JSlider();
        playerAreaVolumeSlider.setBounds(334, 76, 145, 13);
        add(playerAreaVolumeSlider);
        playerAreaVolumeSlider.setForeground(PublicValues.globalFontColor);
        playerAreaVolumeCurrent.setText("10");
        playerAreaVolumeSlider.setMinimum(0);
        playerAreaVolumeSlider.setMaximum(10);
        playerAreaVolumeSlider.setValue(10);
        InstanceManager.getPlayer().getPlayer().setVolume(65536);
        playerAreaVolumeSlider.addChangeListener(e -> {
            playerAreaVolumeCurrent.setText(String.valueOf(playerAreaVolumeSlider.getValue()));
            switch (playerAreaVolumeSlider.getValue()) {
                case 0:
                    InstanceManager.getPlayer().getPlayer().setVolume(0);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEMUTE.getPath());
                    break;
                case 1:
                    InstanceManager.getPlayer().getPlayer().setVolume(6553);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEHALF.getPath());
                    break;
                case 2:
                    InstanceManager.getPlayer().getPlayer().setVolume(13107);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEHALF.getPath());
                    break;
                case 3:
                    InstanceManager.getPlayer().getPlayer().setVolume(19660);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEHALF.getPath());
                    break;
                case 4:
                    InstanceManager.getPlayer().getPlayer().setVolume(26214);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEHALF.getPath());
                    break;
                case 5:
                    InstanceManager.getPlayer().getPlayer().setVolume(32768);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEHALF.getPath());
                    break;
                case 6:
                    InstanceManager.getPlayer().getPlayer().setVolume(39321);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEHALF.getPath());
                    break;
                case 7:
                    InstanceManager.getPlayer().getPlayer().setVolume(45875);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEHALF.getPath());
                    break;
                case 8:
                    InstanceManager.getPlayer().getPlayer().setVolume(52428);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEHALF.getPath());
                    break;
                case 9:
                    InstanceManager.getPlayer().getPlayer().setVolume(58982);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEHALF.getPath());
                    break;
                case 10:
                    InstanceManager.getPlayer().getPlayer().setVolume(65536);
                    playerAreaVolumeIcon.setImage(Graphics.VOLUMEFULL.getPath());
                    break;
            }
        });

        playerTitle = new JScrollText(PublicValues.language.translate("ui.player.title"));
        playerTitle.setBounds(109, 11, 168, getFontMetrics(getFont()).getHeight());
        add(playerTitle);
        playerTitle.setForeground(PublicValues.globalFontColor);

        playerDescription = new JScrollText(PublicValues.language.translate("ui.player.description"));
        playerDescription.setBounds(109, 40, 138, getFontMetrics(getFont()).getHeight());
        add(playerDescription);
        playerDescription.setForeground(PublicValues.globalFontColor);

        playerPlayPreviousButton = new JImageButton();
        playerPlayPreviousButton.setBounds(287, 11, 70, 36);
        playerPlayPreviousButton.setColor(frame.getBackground());
        add(playerPlayPreviousButton);
        playerPlayPreviousButton.addActionListener(new AsyncActionListener(e -> InstanceManager.getSpotifyPlayer().previous()));
        playerPlayPreviousButton.setImage(Graphics.PLAYERPLAYPREVIOUS.getPath());
        playerPlayPreviousButton.setBorderPainted(false);
        playerPlayPreviousButton.setContentAreaFilled(false);

        playerPlayPauseButton = new JImageButton();
        playerPlayPauseButton.setColor(frame.getBackground());
        playerPlayPauseButton.setBounds(369, 11, 69, 36);
        playerPlayPauseButton.addActionListener(new AsyncActionListener(e -> InstanceManager.getPlayer().getPlayer().playPause()));
        add(playerPlayPauseButton);
        playerPlayPauseButton.setImage(Graphics.PLAYERPlAY.getPath());
        playerPlayPauseButton.setBorderPainted(false);
        playerPlayPauseButton.setContentAreaFilled(false);

        playerPlayNextButton = new JImageButton();
        playerPlayNextButton.setColor(frame.getBackground());
        playerPlayNextButton.setBounds(448, 11, 69, 36);
        add(playerPlayNextButton);
        playerPlayNextButton.setImage(Graphics.PLAYERPLAYNEXT.getPath());
        playerPlayNextButton.setBorderPainted(false);
        playerPlayNextButton.setContentAreaFilled(false);
        playerPlayNextButton.addActionListener(new AsyncActionListener(e -> InstanceManager.getPlayer().getPlayer().next()));

        playerCurrentTime = new JSlider();
        playerCurrentTime.setValue(0);
        playerCurrentTime.setMaximum(381);
        playerCurrentTime.setBounds(306, 54, 200, 13);
        add(playerCurrentTime);
        playerCurrentTime.setForeground(PublicValues.globalFontColor);
        playerCurrentTime.addChangeListener(e -> playerPlayTime.setText(TrackUtils.getHHMMSSOfTrack(playerCurrentTime.getValue() * 1000L)));
        playerCurrentTime.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                wasPaused = InstanceManager.getSpotifyPlayer().isPaused();
                InstanceManager.getPlayer().getPlayer().pause();
                PlayerListener.pauseTimer = true;
            }
        }));
        playerCurrentTime.addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                PlayerListener.pauseTimer = false;
                InstanceManager.getPlayer().getPlayer().seek(playerCurrentTime.getValue() * 1000);
                if (!wasPaused) {
                    InstanceManager.getPlayer().getPlayer().play();
                }
            }
        }));
        playerCurrentTime.addChangeListener(e -> playerPlayTime.setText(TrackUtils.getHHMMSSOfTrack(InstanceManager.getPlayer().getPlayer().time())));

        playerPlayTime = new JLabel("00:00");
        playerPlayTime.setHorizontalAlignment(SwingConstants.RIGHT);
        playerPlayTime.setBounds(244, 54, 57, 14);
        add(playerPlayTime);
        playerPlayTime.setForeground(PublicValues.globalFontColor);

        playerPlayTimeTotal = new JLabel("00:00");
        playerPlayTimeTotal.setBounds(506, 54, 49, 14);
        add(playerPlayTimeTotal);
        playerPlayTimeTotal.setForeground(PublicValues.globalFontColor);

        heart = new JSVGPanel();
        heart.getJComponent().setBackground(frame.getBackground());
        heart.getJComponent().setBounds(525, 20, 24, 24);
        heart.getJComponent().addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (heart.isFilled) {
                    try {
                        InstanceManager.getSpotifyApi().removeUsersSavedTracks(Objects.requireNonNull(InstanceManager.getPlayer().getPlayer().currentPlayable()).toSpotifyUri().split(":")[2]).build().execute();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    heart.setImage(Graphics.HEART.getPath());
                    heart.isFilled = false;
                } else {
                    try {
                        InstanceManager.getSpotifyApi().saveTracksForUser(Objects.requireNonNull(InstanceManager.getPlayer().getPlayer().currentPlayable()).toSpotifyUri().split(":")[2]).build().execute();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    heart.setImage(Graphics.HEARTFILLED.getPath());
                    heart.isFilled = true;
                }
            }
        }));
        heart.setImage(Graphics.HEART.getPath());
        add(heart.getJComponent());

        PublicValues.history = new PlaybackHistory();
        Events.subscribe(SpotifyXPEvents.trackNext.getName(), (Object... data) -> {
            if (InstanceManager.getSpotifyPlayer().currentPlayable() == null) return;
            if (!doneLastParsing) return;
            if (Objects.requireNonNull(InstanceManager.getSpotifyPlayer().currentPlayable()).toSpotifyUri().split(":")[1].equals("track")) {
                try {
                    PublicValues.history.addSong(InstanceManager.getSpotifyApi().getTrack(Objects.requireNonNull(InstanceManager.getSpotifyPlayer().currentPlayable()).toSpotifyUri().split(":")[2]).build().execute());
                } catch (SQLException | IOException e) {
                    ConsoleLogging.Throwable(e);
                }
            }
        });

        if (PublicValues.vlcPlayer.isVideoPlaybackEnabled()) canvasPlayer = new CanvasPlayer();
        canvasPlayerButton = new JSVGPanel();
        canvasPlayerButton.setImage(Graphics.VIDEO.getPath());
        canvasPlayerButton.getJComponent().setBackground(heart.getJComponent().getBackground());
        canvasPlayerButton.getJComponent().setBounds(720, 30, 20, 20);
        canvasPlayerButton.getJComponent().addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (canvasPlayerButton.isFilled) {
                    canvasPlayerButton.isFilled = false;
                    canvasPlayerButton.setImage(Graphics.VIDEO.getPath());
                    canvasPlayer.close();
                } else {
                    canvasPlayerButton.isFilled = true;
                    canvasPlayerButton.setImage(Graphics.VIDEOSELECTED.getPath());
                    canvasPlayer.open();
                }
            }
        }));
        if (PublicValues.vlcPlayer.isVideoPlaybackEnabled())
            PublicValues.contentPanel.add(canvasPlayerButton.getJComponent());


        historyButton = new JSVGPanel();
        historyButton.setImage(Graphics.HISTORY.getPath());
        historyButton.getJComponent().setBackground(heart.getJComponent().getBackground());
        historyButton.getJComponent().setBounds(720, 55, 20, 20);
        historyButton.getJComponent().addMouseListener(new AsyncMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                if (historyButton.isFilled) {
                    historyButton.isFilled = false;
                    historyButton.setImage(Graphics.HISTORY.getPath());
                    PublicValues.history.dispose();
                } else {
                    historyButton.isFilled = true;
                    historyButton.setImage(Graphics.HISTORYSELECTED.getPath());
                    PublicValues.history.open();
                }
            }
        }));
        PublicValues.contentPanel.add(historyButton.getJComponent());

        pipPlayer = new PiPPlayer();

        contextMenu = new ContextMenu();
        contextMenu.addItem(PublicValues.language.translate("ui.playerarea.ctxmenu.item1"), new Runnable() {
            @Override
            public void run() {
                pipPlayer.open();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    contextMenu.showAt(ContentPanel.playerArea, e.getX(), e.getY());
                }
            }
        });

        Events.subscribe(SpotifyXPEvents.onFrameReady.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                if (new File(PublicValues.fileslocation, "play.state").exists()) {
                    parseLastPlayState();
                    try {
                        if (!lastPlayState.uri.isEmpty()) {
                            playerPlayTime.setText(lastPlayState.playtime);
                            playerPlayTimeTotal.setText(lastPlayState.playtimetotal);
                            playerCurrentTime.setMaximum(lastPlayState.playerslidermax);
                            InstanceManager.getSpotifyPlayer().load(lastPlayState.uri, false, PublicValues.shuffle);
                            if (!TrackUtils.isTrackLiked(lastPlayState.uri.split(":")[2])) {
                                heart.isFilled = false;
                                heart.setImage(Graphics.HEART.getPath());
                            } else {
                                heart.isFilled = true;
                                heart.setImage(Graphics.HEARTFILLED.getPath());
                            }
                            InstanceManager.getSpotifyPlayer().seek(lastPlayState.playerslider * 1000);
                            playerAreaVolumeSlider.setValue(Integer.parseInt(lastPlayState.playervolume));
                            doneLastParsing = true;
                        }
                        if (!lastPlayState.history.isEmpty()) {
                            try {
                                InstanceManager.getSpotifyPlayer().tracks(true).previous.clear();
                                for (String s : lastPlayState.history) {
                                    InstanceManager.getSpotifyPlayer().addToQueue(s);
                                }
                            } catch (Exception ignored) {
                                ConsoleLogging.warning("Failed to restore player history");
                            }
                        }
                        if (!lastPlayState.queue.isEmpty()) {
                            try {
                                InstanceManager.getSpotifyPlayer().tracks(true).next.clear();
                                for (String s : lastPlayState.queue) {
                                    InstanceManager.getSpotifyPlayer().addToQueue(s);
                                }
                            } catch (Exception ignored) {
                                ConsoleLogging.warning("Failed to restore player queue");
                            }
                        }
                    } catch (Exception e) {
                        //Failed to load last play state! Don't notify user because it's not that important
                    }
                }
            }
        });

        setBorder(null);
        setBounds(784 / 2 - getWidth() / 2, 8, getWidth(), getHeight() - 3);
    }

    @SuppressWarnings({"ConstantConditions", "Duplicates"})
    public static void saveCurrentState() {
        try {
            if (InstanceManager.getSpotifyPlayer().currentPlayable() == null) return;
            List<PlayerState.PlayableUri> playableQueue = new ArrayList<>();
            List<ContextTrackOuterClass.ContextTrack> playableQueueTracks = InstanceManager.getSpotifyPlayer().tracks(true).next;
            for (int playableQueueTracksIndex = 0; playableQueueTracksIndex < playableQueueTracks.size(); playableQueueTracksIndex++) {
                if (playableQueueTracksIndex >= 200) break; // Because of protobuf this can be higher
                ContextTrackOuterClass.ContextTrack track = playableQueueTracks.get(playableQueueTracksIndex);
                playableQueue.add(PlayerState.PlayableUri.newBuilder()
                        .setId(track.getUri().split(":")[2])
                        .setType(PlayerState.EntityType.valueOf(track.getUri().split(":")[1].toUpperCase()))
                        .build());
            }
            List<PlayerState.PlayableUri> playableHistory = new ArrayList<>();
            List<ContextTrackOuterClass.ContextTrack> playableHistoryTracks = InstanceManager.getSpotifyPlayer().tracks(true).previous;
            for (int playableHistoryTracksIndex = 0; playableHistoryTracksIndex < playableHistoryTracks.size(); playableHistoryTracksIndex++) {
                if (playableHistoryTracksIndex >= 200) break; // Because of protobuf this can be higher
                ContextTrackOuterClass.ContextTrack track = playableHistoryTracks.get(playableHistoryTracksIndex);
                playableHistory.add(PlayerState.PlayableUri.newBuilder()
                        .setId(track.getUri().split(":")[2])
                        .setType(PlayerState.EntityType.valueOf(track.getUri().split(":")[1].toUpperCase()))
                        .build());
            }
            PlayerState.State state = PlayerState.State.newBuilder()
                    .setCurrentTrack(PlayerState.PlayableUri.newBuilder()
                            .setId(InstanceManager.getSpotifyPlayer().currentPlayable().toSpotifyUri().split(":")[2])
                            .setType(PlayerState.EntityType.valueOf(InstanceManager.getSpotifyPlayer().currentPlayable().toSpotifyUri().split(":")[1].toUpperCase()))
                            .build())
                    .setCurrentTimeSlider(PlayerArea.playerCurrentTime.getValue())
                    .setCurrentTimeSliderMax(PlayerArea.playerCurrentTime.getMaximum())
                    .setCurrentTimeString(PlayerArea.playerPlayTime.getText())
                    .setDurationString(PlayerArea.playerPlayTimeTotal.getText())
                    .setCurrentVolumeString(PlayerArea.playerAreaVolumeCurrent.getText())
                    .addAllPlayableHistory(playableHistory)
                    .addAllPlayableQueue(playableQueue)
                    .build();
            try (FileOutputStream outputStream = new FileOutputStream(new File(PublicValues.fileslocation, "play.state"))) {
                outputStream.write(state.toByteArray());
            }
        } catch (NullPointerException e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.openException(e);
            if (new File(PublicValues.fileslocation, "play.state").exists()) {
                if (!new File(PublicValues.fileslocation, "play.state").delete()) {
                    ConsoleLogging.warning("Failed to delete play.state");
                }
            }
        } catch (IOException e) {
            ConsoleLogging.Throwable(e);
            GraphicalMessage.openException(e);
        }
    }

    void parseLastPlayState() {
        try {
            byte[] protoBytes = IOUtils.toByteArray(Files.newInputStream(new File(PublicValues.fileslocation, "play.state").toPath()));
            PlayerState.State parsedState = PlayerState.State.parseFrom(protoBytes);
            LastPlayState state = new LastPlayState();
            state.uri = "spotify" + ":" + parsedState.getCurrentTrack().getType().toString().toLowerCase(Locale.ROOT) + ":" + parsedState.getCurrentTrack().getId();
            state.playerslider = (int) parsedState.getCurrentTimeSlider();
            state.playerslidermax = (int) parsedState.getCurrentTimeSliderMax();
            state.playtime = parsedState.getCurrentTimeString();
            state.playtimetotal = parsedState.getDurationString();
            state.playervolume = parsedState.getCurrentVolumeString();
            for (PlayerState.PlayableUri playableUri : parsedState.getPlayableHistoryList()) {
                state.history.add("spotify" + ":" + playableUri.getType().toString().toLowerCase(Locale.ROOT) + ":" + playableUri.getId());
            }
            for (PlayerState.PlayableUri playableUri : parsedState.getPlayableQueueList()) {
                state.queue.add("spotify" + ":" + playableUri.getType().toString().toLowerCase(Locale.ROOT) + ":" + playableUri.getId());
            }
            PlayerArea.lastPlayState = state;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() {
        if (!ContentPanel.frame.isVisible()) {
            Events.subscribe(SpotifyXPEvents.onFrameVisible.getName(), data -> {
                Thread thread = new Thread(() -> {
                    try {
                        Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    reset();
                });
                thread.start();
            });
        }
        playerPlayTime.setText("00:00");
        playerPlayTimeTotal.setText("00:00");
        playerImage.setImage(Graphics.NOTHINGPLAYING.getInputStream());
        playerCurrentTime.setValue(0);
        playerCurrentTime.setMaximum(381);
        heart.setImage(Graphics.HEART.getPath());
        heart.isFilled = false;
        if (playerAreaLyricsButton.isFilled) {
            PublicValues.lyricsDialog.close();
            playerAreaLyricsButton.setImage(Graphics.MICROPHONE.getPath());
            playerAreaLyricsButton.isFilled = false;
        }
        if (playerAreaRepeatingButton.isFilled) {
            InstanceManager.getPlayer().getPlayer().setRepeat(false, false);
            playerAreaRepeatingButton.setImage(Graphics.REPEAT.getPath());
            playerAreaRepeatingButton.isFilled = false;
        }
    }

    private static class LastPlayState {
        public String uri;
        public String playtimetotal;
        public String playtime;
        public int playerslider;
        public int playerslidermax;
        public String playervolume;
        public final ArrayList<String> history = new ArrayList<>();
        public final ArrayList<String> queue = new ArrayList<>();
    }
}

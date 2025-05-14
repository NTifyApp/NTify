package com.spotifyxp.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.spotifyxp.PublicValues;
import com.spotifyxp.api.UnofficialSpotifyAPI;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.PlayerArea;
import com.spotifyxp.swingextension.PaintPanel;
import com.spotifyxp.swingextension.RAWTextArea;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.ClipboardUtil;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.Resources;
import org.json.JSONException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

public class LyricsDialog extends JDialog {
    public JPanel contentPanel;
    public JScrollPane pane;
    public JPanel paintPanel;
    private final ArrayList<ColoredLyricsLine> displayedLines = new ArrayList<>();

    public LyricsDialog() {
        $$$setupUI$$$();
        setTitle(ApplicationUtils.getName() + " - Song Lyrics"); //ToDo: Translate
        setContentPane(contentPanel);
    }

    UnofficialSpotifyAPI.Lyrics lyrics;
    String uri;

    public boolean open(String uri) {
        try {
            if (isVisible()) {
                lyrics = InstanceManager.getUnofficialSpotifyApi().getLyrics(uri);
                if (lyrics == null) throw new JSONException("");
                //New lyrics
            } else {
                lyrics = InstanceManager.getUnofficialSpotifyApi().getLyrics(uri);
                if (lyrics == null) throw new JSONException("");
                addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        super.windowClosing(e);
                        if (PublicValues.theme.isLight()) {
                            PlayerArea.playerAreaLyricsButton.setImage(new Resources().readToInputStream("icons/microphonedark.svg"));
                        } else {
                            PlayerArea.playerAreaLyricsButton.setImage(new Resources().readToInputStream("icons/microphonewhite.svg"));
                        }
                        PlayerArea.playerAreaLyricsButton.isFilled = false;
                        Events.unsubscribe(SpotifyXPEvents.playerSeekedBackwards.getName(), seekedBackwards());
                        Events.unsubscribe(SpotifyXPEvents.playerSeekedForwards.getName(), seekedForwards());
                    }
                });
                try {
                    setIconImage(ImageIO.read(new Resources().readToInputStream("ntify.png")));
                } catch (Exception e) {
                    ConsoleLogging.Throwable(e);
                    if (PublicValues.config.getString(ConfigValues.hideExceptions.name).equals("false")) {
                        GraphicalMessage.openException(e);
                    }
                }
                ContextMenu menu = new ContextMenu(paintPanel, null, getClass());
                menu.addItem(PublicValues.language.translate("ui.general.copy"), new Runnable() {
                    @Override
                    public void run() {
                        StringBuilder buffer = new StringBuilder();
                        for (ColoredLyricsLine lines : new ArrayList<>(displayedLines)) {
                            buffer.append(lines.getTextContent()).append("\n");
                        }
                        ClipboardUtil.set(buffer.toString());
                    }
                });
                setPreferredSize(new Dimension(ContentPanel.frame.getWidth() / 2, ContentPanel.frame.getHeight() / 2));
                pack();
                setVisible(true);
            }
            displayedLines.clear();
            coloredLines.clear();
            for (UnofficialSpotifyAPI.LyricsLine line : lyrics.lines) {
                append(line.words, PublicValues.globalFontColor, line.startTimeMs);
            }
            triggerRefresh();
            Events.subscribe(SpotifyXPEvents.playerSeekedBackwards.getName(), seekedBackwards());
            Events.subscribe(SpotifyXPEvents.playerSeekedForwards.getName(), seekedForwards());
            this.uri = uri;
            return true;
        } catch (JSONException e) {
            ConsoleLogging.info("No song lyrics available for: " + uri);
            return false;
        }
    }

    EventSubscriber seekedForwards() {
        return new EventSubscriber() {
            @Override
            public void run(Object... data) {
                triggerRefresh();
            }
        };
    }

    EventSubscriber seekedBackwards() {
        return new EventSubscriber() {
            @Override
            public void run(Object... data) {
                lyrics = InstanceManager.getUnofficialSpotifyApi().getLyrics(uri);
                coloredLines.clear();
                for (UnofficialSpotifyAPI.LyricsLine line : lyrics.lines) {
                    coloredLines.add(new LyricsDialog.ColoredLyricsLine(PublicValues.globalFontColor, line.words, line.startTimeMs));
                }
                int activeLine = 0;
                for (LyricsDialog.ColoredLyricsLine line : new ArrayList<>(coloredLines)) {
                    if (line.startTimeMS < InstanceManager.getPlayer().getPlayer().time()) {
                        activeLine++;
                        break;
                    }
                }
                try {
                    coloredLines.set(activeLine, new LyricsDialog.ColoredLyricsLine(
                            PublicValues.globalFontColor.brighter().brighter(),
                            coloredLines.get(activeLine).getTextContent(),
                            coloredLines.get(activeLine).getStartTimeMS()
                    ));
                } catch (IndexOutOfBoundsException ignored) {
                }
                repaint();
            }
        };
    }

    private void createUIComponents() {
        paintPanel = new PaintPanel(this::paintLines);
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        contentPanel = new JPanel();
        contentPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        pane = new JScrollPane();
        contentPanel.add(pane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        pane.setViewportView(paintPanel);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }

    private static class ColoredLyricsLine extends RAWTextArea.ColoredLine {
        private final long startTimeMS;

        public ColoredLyricsLine(Color textColor, String textContent, long startTimeMS) {
            super(textColor, textContent);
            this.startTimeMS = startTimeMS;
        }

        public long getStartTimeMS() {
            return this.startTimeMS;
        }
    }

    public void append(String textContent, Color textColor, long startTimeMS) {
        coloredLines.add(new LyricsDialog.ColoredLyricsLine(textColor, textContent, startTimeMS));
        repaint();
    }

    private final ArrayList<ColoredLyricsLine> coloredLines = new ArrayList<>();

    public void close() {
        Events.unsubscribe(SpotifyXPEvents.playerSeekedBackwards.getName(), seekedBackwards());
        Events.unsubscribe(SpotifyXPEvents.playerSeekedForwards.getName(), seekedForwards());
        setVisible(false);
    }

    public void triggerRefresh() {
        if (lyrics == null) {
            return;
        }
        int counter = 0;
        displayedLines.clear();
        for (LyricsDialog.ColoredLyricsLine line : new ArrayList<>(coloredLines)) {
            try {
                if (line.startTimeMS < InstanceManager.getSpotifyPlayer().time() && coloredLines.get(counter + 1).startTimeMS > InstanceManager.getSpotifyPlayer().time()) {
                    displayedLines.add(line);
                    try {
                        displayedLines.add(coloredLines.get(counter + 1));
                    } catch (IndexOutOfBoundsException ignored) {
                    }
                    break;
                }
            } catch (IndexOutOfBoundsException e) {
                displayedLines.add(line);
                break;
            }
            counter++;
        }
        try {
            displayedLines.set(0, new LyricsDialog.ColoredLyricsLine(
                    PublicValues.globalFontColor.brighter().brighter(),
                    displayedLines.get(0).getTextContent(),
                    displayedLines.get(0).getStartTimeMS()
            ));
        } catch (IndexOutOfBoundsException ignored) {
        }
        repaint();
    }

    public void paintLines(Graphics g) {
        try {
            int ycache = getFontMetrics(getFont()).getHeight() + 5;
            for (RAWTextArea.ColoredLine line : new ArrayList<>(displayedLines)) {
                g.setColor(line.getTextColor());
                g.drawString(line.getTextContent(), 5, ycache);
                ycache += getFontMetrics(getFont()).getHeight() + 5;
            }
            setPreferredSize(new Dimension(getWidth(), ycache + 10));
        } catch (NullPointerException ignored) {
        }
    }
}

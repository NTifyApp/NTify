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

package com.spotifyxp.dialogs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.spotifyxp.PublicValues;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Episode;
import com.spotifyxp.deps.se.michaelthelin.spotify.model_objects.specification.Track;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.PlayerArea;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.swingextension.JImagePanel;
import com.spotifyxp.theming.themes.DarkGreen;
import com.spotifyxp.utils.ApplicationUtils;
import com.spotifyxp.utils.SpotifyUtils;
import com.spotifyxp.utils.TrackUtils;
import com.spotifyxp.utils.Utils;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

public class FullscreenPlayerDialog {
    public JPanel contentPanel;
    public JImagePanel image;
    public JButton closeButton;
    public JPanel controlsPanel;
    public JLabel playerTitleDesc;
    public JFrame frame;

    private int x, y, width, height;

    public FullscreenPlayerDialog() {
        $$$setupUI$$$();

        x = ContentPanel.playerArea.getX();
        y = ContentPanel.playerArea.getY();
        width = ContentPanel.playerArea.getWidth();
        height = ContentPanel.playerArea.getHeight();

        controlsPanel.setLayout(null);

        PublicValues.contentPanel.remove(ContentPanel.playerArea);

        int centerX = (ContentPanel.playerArea.getWidth() - PlayerArea.playerPlayTime.getX()) / 2;
        centerX = centerX * -1;

        ContentPanel.playerArea.setBounds(centerX + 40, 0, width, height);

        controlsPanel.setPreferredSize(new Dimension(width, height));

        controlsPanel.add(ContentPanel.playerArea);
        PublicValues.contentPanel.revalidate();
        PublicValues.contentPanel.repaint();

        PlayerArea.playerImage.setVisible(false);
        PlayerArea.playerTitle.setVisible(false);
        PlayerArea.playerDescription.setVisible(false);

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
                for (WindowListener listener : frame.getWindowListeners()) {
                    listener.windowClosing(null);
                }
            }
        });

        Events.subscribe(SpotifyXPEvents.trackNext.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                if (data[0] instanceof Track) {
                    playerTitleDesc.setText(((Track) data[0]).getName() + " - " + TrackUtils.getArtists(((Track) data[0]).getArtists()));
                    try {
                        image.setImage(new URL(SpotifyUtils.getImageForSystem(((Track) data[0]).getAlbum().getImages()).getUrl()));
                    } catch (MalformedURLException e) {
                        ConsoleLogging.Throwable(e);
                    }
                } else if (data[0] instanceof Episode) {
                    playerTitleDesc.setText(((Episode) data[0]).getShow().getName() + " - " + ((Episode) data[0]).getName());
                    try {
                        image.setImage(new URL(SpotifyUtils.getImageForSystem(((Episode) data[0]).getImages()).getUrl()));
                    } catch (MalformedURLException e) {
                        ConsoleLogging.Throwable(e);
                    }
                }
            }
        });

        if (InstanceManager.getSpotifyPlayer().currentPlayable() != null) {
            if (InstanceManager.getSpotifyPlayer().currentPlayable().toSpotifyUri().split(":")[1].equals("track")) {
                playerTitleDesc.setText(PlayerArea.playerTitle.getText() + " - " + PlayerArea.playerDescription.getText());
            } else {
                playerTitleDesc.setText(PlayerArea.playerDescription + " - " + PlayerArea.playerTitle.getText());
            }

            image.setImage(PlayerArea.playerImage.getImageStream());
        }
    }

    public void open() {
        frame = new JFrame(ApplicationUtils.getName() + " - Fullscreen Player");
        frame.setContentPane(contentPanel);
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                controlsPanel.remove(ContentPanel.playerArea);
                ContentPanel.playerArea.setBounds(x, y, width, height);
                PublicValues.contentPanel.add(ContentPanel.playerArea);
                PublicValues.contentPanel.revalidate();
                PublicValues.contentPanel.repaint();

                PlayerArea.playerImage.setVisible(true);
                PlayerArea.playerTitle.setVisible(true);
                PlayerArea.playerDescription.setVisible(true);
            }
        });
        frame.pack();
        frame.setVisible(true);
        Utils.moveToScreen(frame, Utils.getDisplayNumber(ContentPanel.frame));
        ContentPanel.frame.getGraphicsConfiguration().getDevice().setFullScreenWindow(frame);
    }

    public void close() {
        frame.dispose();
    }

    public static void main(String[] args) {
        PublicValues.theme = new DarkGreen();
        PublicValues.theme.initTheme();
        ContentPanel.frame.setBackground(Color.decode("#3c3f41"));
        FullscreenPlayerDialog dialog = new FullscreenPlayerDialog();
        dialog.open();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout(0, 0));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setMaximumSize(new Dimension(720, 2147483647));
        contentPanel.add(panel1, BorderLayout.SOUTH);
        controlsPanel = new JPanel();
        controlsPanel.setLayout(new BorderLayout(0, 0));
        panel1.add(controlsPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPanel.add(panel2, BorderLayout.NORTH);
        closeButton = new JButton();
        closeButton.setText("X");
        panel2.add(closeButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPanel.add(panel3, BorderLayout.CENTER);
        image = new JImagePanel();
        panel3.add(image, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        playerTitleDesc = new JLabel();
        Font playerTitleDescFont = this.$$$getFont$$$(null, -1, 20, playerTitleDesc.getFont());
        if (playerTitleDescFont != null) playerTitleDesc.setFont(playerTitleDescFont);
        playerTitleDesc.setText("Label");
        panel3.add(playerTitleDesc, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 5, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPanel;
    }

}

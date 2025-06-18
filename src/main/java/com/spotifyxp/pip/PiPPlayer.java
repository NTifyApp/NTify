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
package com.spotifyxp.pip;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.ctxmenu.ContextMenu;
import com.spotifyxp.events.EventSubscriber;
import com.spotifyxp.events.Events;
import com.spotifyxp.events.SpotifyXPEvents;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.manager.InstanceManager;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.panels.PlayerArea;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.swingextension.JImageButton;
import com.spotifyxp.swingextension.JImagePanel;
import com.spotifyxp.utils.AsyncActionListener;
import com.spotifyxp.utils.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;

public class PiPPlayer {
    public static JImagePanel songImage;
    private int pX, pY;
    private boolean isResizing = false, isMoving = false;
    private Rectangle resizingRect, northWestRect, northEastRect, southWestRect, southEastRect;
    private int resizingRectSpacing = 10;
    private int resizeDirection = Cursor.DEFAULT_CURSOR;

    public static JLayeredPane container;
    public static JImageButton playPause;
    public static JImageButton closeButton;
    public static JImageButton nextButton;
    public static JImageButton previousButton;

    public static String pausePath = "/icons/playerpausedark.svg";
    public static String playPath = "/icons/playerplaydark.svg";
    public static String closePath = "/icons/closedark.svg";
    public static String nextPath = "/icons/playerplaynextdark.svg";
    public static String previousPath = "/icons/playerplaypreviousdark.svg";

    public static int initialWindowSize = 280;
    public static int buttonSize = 30;

    public static File cachePath;

    public static ContextMenu ctxMenu;

    public static ArrayList<JImageButton> controlButtons ;

    public static JPanel controlsContainer;

    private JFrame frame;

    void resizeComponents() {
        for(Component component : container.getComponents()) {
            if(component.getName() != null && component.getName().equals("ResizeRect")) {
                component.setBounds(resizingRect);
                continue;
            }
            component.setBounds(0, 0, frame.getWidth(), frame.getHeight());
        }
        closeButton.setBounds(resizingRect.width - buttonSize, 0, buttonSize, buttonSize);
        previousButton.setBounds(0, resizingRect.height - buttonSize, buttonSize, buttonSize);
        playPause.setBounds(resizingRect.width / 2  - buttonSize / 2, resizingRect.height - buttonSize, buttonSize, buttonSize);
        nextButton.setBounds(resizingRect.width - buttonSize, resizingRect.height - buttonSize, buttonSize, buttonSize);
    }

    MouseAdapter mouseAdapter = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if(SwingUtilities.isRightMouseButton(e)) {
                ctxMenu.showAt(container, e.getX(), e.getY());
            }
        }

        @Override
        public void mousePressed(MouseEvent me) {
            if(SwingUtilities.isRightMouseButton(me)) {
                return;
            }

            pX = me.getX();
            pY = me.getY();

            if (northWestRect.contains(me.getPoint())) {
                isResizing = true;
                resizeDirection = Cursor.NW_RESIZE_CURSOR;
            } else if (northEastRect.contains(me.getPoint())) {
                isResizing = true;
                resizeDirection = Cursor.NE_RESIZE_CURSOR;
            } else if (southWestRect.contains(me.getPoint())) {
                isResizing = true;
                resizeDirection = Cursor.SW_RESIZE_CURSOR;
            } else if (southEastRect.contains(me.getPoint())) {
                isResizing = true;
                resizeDirection = Cursor.SE_RESIZE_CURSOR;
            } else if (resizingRect.contains(me.getPoint())) {
                isMoving = true;
                frame.setCursor(Cursor.MOVE_CURSOR);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            isResizing = false;
            isMoving = false;
            resizeDirection = Cursor.DEFAULT_CURSOR;
            frame.setCursor(Cursor.DEFAULT_CURSOR);
            recalculateRects(false);
            resizeComponents();
            frame.revalidate();
            frame.repaint();
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            controlsContainer.setVisible(true);
            frame.revalidate();
            frame.repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if(frame.contains(e.getPoint())) return;
            controlsContainer.setVisible(false);
            frame.revalidate();
            frame.repaint();
        }
    };

    MouseMotionListener mouseMotionListener = new MouseAdapter() {
        @Override
        public void mouseDragged(MouseEvent me) {
            int dx = me.getX() - pX;
            int dy = me.getY() - pY;

            if (isResizing) {
                int newX = frame.getX();
                int newY = frame.getY();
                int newWidth = frame.getWidth();
                int newHeight = frame.getHeight();

                switch (resizeDirection) {
                    case Cursor.NW_RESIZE_CURSOR:
                        newX += dx;
                        newY += dy;
                        newWidth -= dx;
                        newHeight -= dy;
                        break;
                    case Cursor.NE_RESIZE_CURSOR:
                        newY += dy;
                        newWidth += dx;
                        newHeight -= dy;
                        break;
                    case Cursor.SW_RESIZE_CURSOR:
                        newX += dx;
                        newWidth -= dx;
                        newHeight += dy;
                        break;
                    case Cursor.SE_RESIZE_CURSOR:
                        newWidth += dx;
                        newHeight += dy;
                        break;
                }

                if (newWidth > 100 && newHeight > 100) {
                    frame.setBounds(newX, newY, newWidth, newHeight);
                    pX = me.getX();
                    pY = me.getY();
                }
            } else if (isMoving) {
                frame.setLocation(frame.getLocation().x + dx, frame.getLocation().y + dy);
            }
        }

        @Override
        public void mouseMoved(MouseEvent me) {
            for(JImageButton button : controlButtons) {
                if(button.contains(me.getPoint())) {
                    frame.setCursor(Cursor.DEFAULT_CURSOR);
                }
            }
            if (northWestRect.contains(me.getPoint())) {
                frame.setCursor(new Cursor(Cursor.NW_RESIZE_CURSOR));
            } else if (northEastRect.contains(me.getPoint())) {
                frame.setCursor(new Cursor(Cursor.NE_RESIZE_CURSOR));
            } else if (southWestRect.contains(me.getPoint())) {
                frame.setCursor(new Cursor(Cursor.SW_RESIZE_CURSOR));
            } else if (southEastRect.contains(me.getPoint())) {
                frame.setCursor(new Cursor(Cursor.SE_RESIZE_CURSOR));
            } else if (resizingRect.contains(me.getPoint())) {
                frame.setCursor(Cursor.MOVE_CURSOR);
            } else {
                frame.setCursor(Cursor.DEFAULT_CURSOR);
            }
        }
    };

    public PiPPlayer() {
        ctxMenu = new ContextMenu();
        ctxMenu.addItem(PublicValues.language.translate("pip.ctxmenu.item1"), new Runnable() {
            @Override
            public void run() {
                String buttonHeight = JOptionPane.showInputDialog(PublicValues.language.translate("pip.ctxmenu.item1.message"));
                if(buttonHeight.isEmpty()) {
                    return;
                }
                try {
                    buttonSize = Integer.parseInt(buttonHeight);
                    resizeComponents();
                }catch (NumberFormatException e) {
                    ConsoleLogging.Throwable(e);
                }
            }
        });

        Events.subscribe(SpotifyXPEvents.playerresume.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                if(frame != null) {
                    playPause.setImage(new Resources().readToInputStream(pausePath));
                }
            }
        });

        Events.subscribe(SpotifyXPEvents.playerpause.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                if(frame != null) {
                    playPause.setImage(new Resources().readToInputStream(playPath));
                }
            }
        });

        Events.subscribe(SpotifyXPEvents.playerLockRelease.getName(), new EventSubscriber() {
            @Override
            public void run(Object... data) {
                if(frame != null) {
                    songImage.setImage(PlayerArea.playerImage.getImageStream());
                }
            }
        });
    }

    private void recalculateRects(boolean preInit) {
        int windowWidth = frame.getWidth();
        int windowHeight = frame.getHeight();
        if(preInit) {
            windowWidth = initialWindowSize;
            windowHeight = initialWindowSize;
        }
        resizingRect = new Rectangle(resizingRectSpacing, resizingRectSpacing, windowWidth - (resizingRectSpacing * 2), windowHeight - (resizingRectSpacing * 2));
        northWestRect = new Rectangle(0, 0, resizingRectSpacing, resizingRectSpacing);
        northEastRect = new Rectangle(windowWidth - resizingRectSpacing, 0, resizingRectSpacing, resizingRectSpacing);
        southWestRect = new Rectangle(0, windowHeight - resizingRectSpacing, resizingRectSpacing, resizingRectSpacing);
        southEastRect = new Rectangle(windowWidth - resizingRectSpacing, windowHeight - resizingRectSpacing, resizingRectSpacing, resizingRectSpacing);
    }

    public void open() {
        frame = new JFrame();

        recalculateRects(true);

        controlButtons = new ArrayList<>();

        frame.setBackground(Color.BLACK);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setPreferredSize(new Dimension(initialWindowSize, initialWindowSize));
        frame.addMouseListener(mouseAdapter);
        frame.addMouseMotionListener(mouseMotionListener);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        if(!PublicValues.config.getBoolean(ConfigValues.cache_disabled.name)) {
            cachePath = new File(PublicValues.appLocation, "cvnscache");
            if(!cachePath.exists()) {
                if(!cachePath.mkdir()) {
                    ConsoleLogging.error("Failed to create cvnscache directory");
                    PublicValues.contentPanel.remove(PlayerArea.canvasPlayerButton.getJComponent());
                }
            }
        }

        container = new JLayeredPane();
        container.setBackground(Color.BLACK);
        frame.setContentPane(container);

        songImage = new JImagePanel();
        songImage.setBackground(Color.BLACK);
        songImage.setSize(initialWindowSize, initialWindowSize);
        container.add(songImage, JLayeredPane.DEFAULT_LAYER);

        closeButton = new JImageButton();
        closeButton.setBorderPainted(false);
        closeButton.setImage(new Resources().readToInputStream(closePath));
        closeButton.setColor(Color.WHITE);
        closeButton.addActionListener(new AsyncActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        }));
        closeButton.setBounds(resizingRect.width / 2 - buttonSize / 2, 0, buttonSize, buttonSize);
        controlButtons.add(closeButton);


        previousButton = new JImageButton();
        previousButton.setBorderPainted(false);
        previousButton.setImage(new Resources().readToInputStream(previousPath));
        previousButton.setColor(Color.WHITE);
        previousButton.addActionListener(new AsyncActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InstanceManager.getSpotifyPlayer().previous();
            }
        }));
        previousButton.setBounds(0, resizingRect.height - buttonSize, buttonSize, buttonSize);
        controlButtons.add(previousButton);


        playPause = new JImageButton();
        playPause.setImage(new Resources().readToInputStream(playPath));
        playPause.addActionListener(new AsyncActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(InstanceManager.getSpotifyPlayer().isPaused()) {
                    InstanceManager.getSpotifyPlayer().play();
                }else {
                    InstanceManager.getSpotifyPlayer().pause();
                }
            }
        }));
        playPause.setBorderPainted(false);
        playPause.setColor(Color.WHITE);
        playPause.setBounds(resizingRect.width / 2  - buttonSize / 2, resizingRect.height - buttonSize, buttonSize, buttonSize);
        controlButtons.add(playPause);


        nextButton = new JImageButton();
        nextButton.setBorderPainted(false);
        nextButton.setImage(new Resources().readToInputStream(nextPath));
        nextButton.setColor(Color.WHITE);
        nextButton.addActionListener(new AsyncActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                InstanceManager.getSpotifyPlayer().next();
            }
        }));
        nextButton.setBounds(resizingRect.width - buttonSize, resizingRect.height - buttonSize, buttonSize, buttonSize);
        controlButtons.add(nextButton);

        controlsContainer = new JPanel();
        controlsContainer.setName("ResizeRect");
        controlsContainer.setBackground(new Color(0, 0, 0, 0));
        controlsContainer.setOpaque(false);
        controlsContainer.setLayout(null);
        controlsContainer.setVisible(false);
        controlsContainer.add(closeButton);
        controlsContainer.add(previousButton);
        controlsContainer.add(playPause);
        controlsContainer.add(nextButton);
        container.add(controlsContainer, JLayeredPane.PALETTE_LAYER);

        frame.open();

        resizeComponents();
        frame.setLocation(ContentPanel.frame.getLocation());
        if(InstanceManager.getSpotifyPlayer().isPaused()) {
            playPause.setImage(new Resources().readToInputStream(playPath));
        }else{
            playPause.setImage(new Resources().readToInputStream(pausePath));
        }
        songImage.setImage(PlayerArea.playerImage.getImageStream());
    }

    public void close() {
        frame.dispose();
        frame = null;
        container = null;
        playPause = null;
        closeButton = null;
        previousButton = null;
        controlsContainer = null;
        controlButtons = null;
    }
}
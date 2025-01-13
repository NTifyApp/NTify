package com.spotifyxp.panels;

import com.spotifyxp.PublicValues;
import com.spotifyxp.configuration.ConfigValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.swingextension.JFrame;
import com.spotifyxp.swingextension.JImagePanel;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.Resources;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

public class SplashPanel {
    public static JFrame frame;
    public static JLabel linfo = new JLabel();
    boolean selected = false;
    Rectangle clickableRect = new Rectangle(268, 10, 10, 10);

    public void show() {
        frame = new JFrame() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                if(!selected) g.setColor(Color.lightGray);
                if(selected) g.setColor(Color.black);
                g.drawString("X", 270, 20);
            }
        };
        frame.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if(clickableRect.contains(e.getPoint())) {
                    selected = true;
                    frame.repaint();
                }else {
                    selected = false;
                    frame.repaint();
                }
            }
        });
        frame.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(clickableRect.contains(e.getPoint())) {
                    System.exit(0);
                }
            }
        });
        JImagePanel image = new JImagePanel();
        linfo = new JLabel("Please wait...");
        image.setImage(new Resources().readToInputStream("spotifyxp.png"));
        frame.getContentPane().add(image);
        frame.setPreferredSize(new Dimension(290, 300));
        try {
            frame.setIconImage(ImageIO.read(new Resources().readToInputStream("spotifyxp.png")));
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
            if (PublicValues.config.getString(ConfigValues.hideExceptions.name).equals("false")) {
                GraphicalMessage.openException(e);
            }
        }
        frame.add(linfo, BorderLayout.SOUTH);
        frame.setAlwaysOnTop(true);
        frame.setLocationRelativeTo(null);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.pack();
        frame.setLocation(frame.getLocation().x - frame.getWidth() / 2, frame.getLocation().y - frame.getHeight() / 2);
    }

    public static void hide() {
        frame.setVisible(false);
    }
}

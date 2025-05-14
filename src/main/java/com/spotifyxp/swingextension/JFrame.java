package com.spotifyxp.swingextension;

import com.spotifyxp.PublicValues;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.utils.GraphicalMessage;
import com.spotifyxp.utils.Resources;
import com.spotifyxp.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;

public class JFrame extends javax.swing.JFrame {
    public JFrame(String title) {
        super.setTitle(title);
        try {
            setIconImage(ImageIO.read(new Resources().readToInputStream("ntify.png")));
        } catch (IOException e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
        }
    }

    public JFrame() {
        try {
            setIconImage(ImageIO.read(new Resources().readToInputStream("ntify.png")));
        } catch (IOException e) {
            GraphicalMessage.openException(e);
            ConsoleLogging.Throwable(e);
        }
    }

    public Point getCenter() {
        return new Point(getLocation().x / 2, getLocation().y / 2);
    }

    public void close() {
        this.setVisible(false);
    }

    public void open() {
        this.pack();
        this.setVisible(true);
    }

    @Override
    public void pack() {
        if (ContentPanel.frame.isVisible()) {
            Utils.moveToScreen(this, PublicValues.screenNumber);
        }
        super.pack();
    }
}

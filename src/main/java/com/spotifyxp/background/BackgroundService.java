package com.spotifyxp.background;


import com.spotifyxp.PublicValues;
import com.spotifyxp.lib.libDetect;
import com.spotifyxp.logging.ConsoleLogging;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.tray.SystemTrayDialog;
import com.spotifyxp.utils.Resources;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;


public class BackgroundService {
    /**
     * Holds the trayDialog after start() was called
     */
    public static SystemTrayDialog trayDialog;

    /**
     * Creates the tray icon
     */
    public void start() {
        if(PublicValues.osType == libDetect.OSType.Linux) {
            //No support for ICCCM XEmbed protocol on newer Desktop Environments
            return;
        }
        try {
            trayDialog = new SystemTrayDialog();
            trayDialog.add(new ImageIcon(ImageIO.read(new Resources().readToInputStream("ntify.png"))), "SpotifyXP");
            trayDialog.open(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    ContentPanel.frame.setVisible(true);
                    ContentPanel.frame.requestFocus();
                }
            });
        } catch (Exception e) {
            ConsoleLogging.Throwable(e);
        }
    }
}

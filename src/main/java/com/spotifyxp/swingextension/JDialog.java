package com.spotifyxp.swingextension;

import com.spotifyxp.PublicValues;
import com.spotifyxp.panels.ContentPanel;
import com.spotifyxp.utils.Resources;
import com.spotifyxp.utils.Utils;

import javax.imageio.ImageIO;
import java.io.IOException;

public class JDialog extends javax.swing.JDialog {
    public JDialog() throws IOException {
        super(ContentPanel.frame);
        setIconImage(ImageIO.read(new Resources().readToInputStream("ntify.png")));
    }

    public void pack() {
        if (ContentPanel.frame.isVisible()) {
            Utils.moveToScreen(this, PublicValues.screenNumber);
        }
        super.pack();
    }
}
